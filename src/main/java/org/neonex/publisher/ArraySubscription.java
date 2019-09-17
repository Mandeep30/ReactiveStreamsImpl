package org.neonex.publisher;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class satisfies the following reactive spec which eventually fulfils the complete spec -
 * <p>
 * 1. Reactive-Streams specification mandates that all method of Subscriber MUST be executed in particular order.
 * 2. Reactive-Streams specification states that Publisher MUST produce less or equal to the specified number of elements in Subscription#request, Back pressure
 * 3. Reactive-Streams specification states that null must be avoided in sending to Subscriber.
 * Thus, in case null element is found inside an array, Publisher MUST interrupt by sending onError signal to its subscriber.
 * 4. It is common that each Subscriber#onNext call can end up with subsequent Subscription#request.
 * Reactive-Streams specification clearly states that Subscriber can synchronously call Subscription and Publisher MUST be protected from recursive calls in the same stack
 * 5. Reactive-Streams specification states that in case of cancellation, Publisher MUST stop sending data eventually.
 * 6. The reactive-streams spec states that execution can occur in a multi-threading environment. Thus Publisher should be prepared to handle concurrency on it.
 * 7. Reactive-Streams clearly states that MUST support demand up to Long.MAX_VALUE - 1 and everything above that line MAST do not fail execution
 *
 * @param <T>
 * @author Mandeep Rajpal
 */
final class ArraySubscription<T> implements Subscription {

    private final Subscriber<? super T> subscriber;
    private final T[] array;

    private volatile boolean isCompleted = false;
    private volatile boolean isCancelled = false;

    private AtomicInteger requestedElements = new AtomicInteger(0);
    private AtomicInteger startIndex = new AtomicInteger(0);

    ArraySubscription(T[] array, Subscriber<? super T> subscriber) {
        this.array = array;
        this.subscriber = subscriber;
    }

    @Override
    public void request(final long numberOfElements) {
        if (numberOfElements < 1) {
            //cancel the subscription for such scenario as per specification
            cancel();
            //send as IllegalArgumentException to onError() channel
            subscriber.onError(new IllegalArgumentException());
            return;
        }
        if (!isCompleted && !isCancelled) {
            //if 'numberOfElements' is greater than Long.Max_Value then cap it to array.length
            //we need to perform this atomically as there can be a case when two threads request numberOfElements > Long.Max_Value
            //and in case it sleeps right after its value has been capped, so, eventually it adds Long.Max_value * 2 which is < 0 to 'requestedElements'
            //getAndUpdate() performs the implementation of apply() method of Functional Interface in an atomic way using CAS
            //here 'requestedElements' act as a flag for work in progress as first time it will return 0
            //and afterwards (when called by other threads of subscriber or from onNext() recursively) it will return value > 0
            var numberOfElementsEmitted = requestedElements.getAndUpdate(n -> {
                n = (int) numberOfElements + n;
                //as Long.Max_Value + some value will result in some negative value
                if (n <= 0) {
                    return array.length;
                }
                return n;
            });

            //'numberOfElements' is primitive local variable which belongs to a thread stack
            //so, for first thread its value will be 0
            if (numberOfElementsEmitted > 0) {
                //work in progress pattern - if one thread is already emitting elements then
                //subsequent threads will return from here after it updates 'requestedElements'
                //also, this prevents stack overflow in case request() is called recursively from onNext()
                //as the first call in the stack will only emit the elements
                return;
            }
            while (true) {
                //either terminate the loop if 'numberOfElementsEmitted >= requestedElements' or if it is greater than array.length
                //startIndex is used to maintain state of index of array
                //till where the elements has been already emitted by publisher
                //this is how back pressure is supported
                for (; numberOfElementsEmitted < requestedElements.get() && startIndex.get() < array.length; startIndex.incrementAndGet()) {
                    //if element is null send it to onError() channel instead of throwing NullPointerException
                    if (array[startIndex.get()] == null) {
                        subscriber.onError(new NullPointerException());
                    }
                    //maintain count of elements which are already sent
                    //this will also be used to perform compensating operation on 'requestedElements'
                    numberOfElementsEmitted++;
                    //send data to onNext() channel
                    //reactive streams are push based unlike Java 8 Streams which are pull based
                    subscriber.onNext(array[startIndex.get()]);
                }
                //this condition needs to be checked prior to 'requestedElements.addAndGet(-numberOfElementsEmitted) == 0'
                //as we have to maintain the order of signals emitted
                if (startIndex.get() == array.length) {
                    //send onComplete() signal
                    subscriber.onComplete();
                    isCompleted = true;
                    return;
                }
                //if first thread sleeps here and other threads add to 'requestedElements'
                //so, we need to reset 'requestedElements=0' after this condition is false
                //now we need a compensating operation,i.e. for 'getAndAdd()' we perform 'addAndGet()'
                //to signal that work in progress is now false
                //this 'requestedElements' should not be dependent on other instance variables so as to avoid race condition
                if (requestedElements.addAndGet(-numberOfElementsEmitted) == 0) {
                    return;
                }
                numberOfElementsEmitted = 0;
                //if above condition fails that means there are more elements requested by other threads
                //so, go back to start of the for loop and serve those elements, Work Stealing Algorithm
            }
        }
    }

    @Override
    public void cancel() {
        isCancelled = true;
    }
}
