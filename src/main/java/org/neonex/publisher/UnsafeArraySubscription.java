package org.neonex.publisher;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@Deprecated(forRemoval = true)
final class UnsafeArraySubscription<T> implements Subscription {
    private Subscriber<? super T> subscriber;
    private T[] array;
    private int startIndex = 0;
    private boolean isCompleted = false;
    private boolean workInProgress = false;
    private int requestedElements = 0;

    public UnsafeArraySubscription(T[] array, Subscriber<? super T> subscriber) {
        this.array = array;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long numberOfElements) {
        if (!isCompleted) {
            requestedElements += (int) numberOfElements;
            if (requestedElements > array.length) {
                requestedElements = array.length - startIndex;
            }
            if (workInProgress) {
                return;
            }
            for (int i = startIndex; i < startIndex + requestedElements; i++) {
                workInProgress = true;
                if (array[i] == null) {
                    subscriber.onError(new NullPointerException());
                    return;
                }
                subscriber.onNext(array[i]);
            }
            startIndex = startIndex + requestedElements;
            requestedElements = 0;
            workInProgress = false;
            if (startIndex == array.length) {
                subscriber.onComplete();
                isCompleted = true;
            }
        }
    }

    @Override
    public void cancel() {

    }
}
