package org.neonex.fountain;

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
            cancel();
        }
        if (!isCompleted && !isCancelled) {
            requestedElements.addAndGet(numberOfElements);

            //e.g. Array size is 5 and 10 elements are requested but already 2 elements are being served so requested should be 3
            //Also, caps the numberOfElements in case numberOfElements > Long.MAX_VALUE
            if (requestedElements.get() > array.length) {
                requestedElements.updateAndGet(n -> array.length - currentElementIndex.get());
            }

            var initialRequestedElements = requestedElements.intValue();
            var index = currentElementIndex.getAndAdd(initialRequestedElements);
            //to prevent StackOverFlow when onNext() calls request() method recursively
            //Also, prevent other threads to return from here one thread is already publishing elements
            if (workInProgress.getAndSet(true)) {
                return;
            }

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
                if (initialRequestedElements == requestedElements.get()) {
                    if (workInProgress.compareAndSet(true, false)) {
                        requestedElements.set(0);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void cancel() {
        isCancelled = true;
    }
}
