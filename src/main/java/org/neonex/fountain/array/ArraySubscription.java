package org.neonex.fountain.array;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class ArraySubscription<T> implements Subscription {
    private Subscriber<? super T> subscriber;
    private T[] array;
    private AtomicInteger currentElementIndex = new AtomicInteger(0);
    private volatile boolean isCompleted = false;
    private AtomicBoolean workInProgress = new AtomicBoolean(false);
    private AtomicLong requestedElements = new AtomicLong(0);
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

            //e.g. Array size is 5 and 10 is request but already 2 elements are being served so requested should be 3
            if (requestedElements.get() > array.length) {
                requestedElements.updateAndGet(n -> array.length - currentElementIndex.get());
            }

            var initialRequestedElements = requestedElements.intValue();
            var index = currentElementIndex.getAndAdd(initialRequestedElements);
            //to prevent StackOverFlow
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
