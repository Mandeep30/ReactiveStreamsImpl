package org.neonex.publisher;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @param <T>
 * @author Mandeep Rajpal
 */
final class ArraySubscription<T> implements Subscription {
    private final Subscriber<? super T> subscriber;
    private final T[] array;
    private final AtomicInteger currentElementIndex = new AtomicInteger(0);
    private volatile boolean isCompleted = false;
    private final AtomicBoolean workInProgress = new AtomicBoolean(false);
    private final AtomicLong requestedElements = new AtomicLong(0);
    private volatile boolean isCancelled = false;

    ArraySubscription(T[] array, Subscriber<? super T> subscriber) {
        this.array = array;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long numberOfElements) {
        if (numberOfElements < 1) {
            subscriber.onError(new IllegalArgumentException());
        }
        if (!isCompleted && !isCancelled) {
            requestedElements.addAndGet(numberOfElements);

            //e.g. Array size is 5 and 10 elements are requested but already 2 elements are being served so requested should be 3
            //Also, caps the numberOfElements in case numberOfElements > Long.MAX_VALUE
            if (requestedElements.get() > array.length) {
                requestedElements.updateAndGet(n -> array.length - currentElementIndex.get());
            }
            //Primitive local variables are thread safe
            var initialRequestedElements = requestedElements.intValue();
            //currentElementIndex maintains index of Array till where the data has been published
            // index = currentElementIndex;
            // currentElementIndex = currentElementIndex + initialRequestedElements;
            var index = currentElementIndex.getAndAdd(initialRequestedElements);

            //'Work in Progress' pattern
            //To prevent StackOverFlow when onNext() calls request() method recursively
            //Also, prevent other threads to return from here when one thread is already publishing elements - Work Stealing
            if (workInProgress.getAndSet(true)) {
                return;
            }
            //From this point of code it is guaranteed that only one thread will be present here. So, no CAS required
            //Will break infinite loop when WIP is false or all the elements of array are published
            while (true) {
                for (; index < currentElementIndex.get() && index < array.length; index++) {
                    //should not throw NPE instead send it as NPE using onError()
                    if (array[index] == null) {
                        subscriber.onError(new NullPointerException());
                        return;
                    }
                    subscriber.onNext(array[index]);
                }
                if (currentElementIndex.get() >= array.length) {
                    subscriber.onComplete();
                    isCompleted = true;
                    return;
                }
                //No more elements to process so, this thread may return now
                if (initialRequestedElements == requestedElements.get()) {
                    workInProgress.set(false);
                    requestedElements.set(0);
                    return;

                }
            }
        }
    }

    @Override
    public void cancel() {
        isCancelled = true;
    }
}
