package org.neonex.publisher;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <T>
 * @author Mandeep Rajpal
 */
final class ArraySubscription<T> implements Subscription {
    private final Subscriber<? super T> subscriber;
    private final T[] array;
    private volatile boolean isCompleted = false;
    private volatile boolean isCancelled = false;
    private AtomicInteger endIndex = new AtomicInteger(0);
    private AtomicInteger startIndex = new AtomicInteger(0);
    private AtomicBoolean workInProgress = new AtomicBoolean(false);

    ArraySubscription(T[] array, Subscriber<? super T> subscriber) {
        this.array = array;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long numberOfElements) {
        if (numberOfElements < 1) {
            cancel();
            subscriber.onError(new IllegalArgumentException());
            return;
        }
        if (numberOfElements > array.length) {
            numberOfElements = array.length;
        }
        if (!isCompleted && !isCancelled) {
            endIndex.getAndAdd((int) numberOfElements);
            if (workInProgress.getAndSet(true)) {
                return;
            }
            for (; startIndex.get() < endIndex.get() && startIndex.get() < array.length; startIndex.incrementAndGet()) {
                if (array[startIndex.get()] == null) {
                    subscriber.onError(new NullPointerException());
                }
                subscriber.onNext(array[startIndex.get()]);
            }
            workInProgress.compareAndSet(true, false);
            if (startIndex.get() == array.length) {
                subscriber.onComplete();
                isCompleted = true;
            }
        }
    }

    @Override
    public void cancel() {
        isCancelled = true;
    }
}
