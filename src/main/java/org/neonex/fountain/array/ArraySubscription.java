package org.neonex.fountain.array;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class ArraySubscription<T> implements Subscription {
    private Subscriber<? super T> subscriber;
    private T[] array;
    private int startIndex = 0;
    private boolean isCompleted = false;
    private boolean workInProgress = false;
    private long requestedElements = 0;
    private boolean isCancelled = false;

    ArraySubscription(T[] array, Subscriber<? super T> subscriber) {
        this.array = array;
        this.subscriber = subscriber;
    }

    @Override
    public synchronized void request(long numberOfElements) {
        if (numberOfElements < 1) {
            subscriber.onError(new IllegalArgumentException());
            cancel();
        }
        if (!isCompleted && !isCancelled) {
            requestedElements += numberOfElements;

            //e.g. Array size is 5 and 10 is request but already 2 elements are being served so requested should be 3
            if (requestedElements > array.length) {
                requestedElements = array.length - startIndex;
            }

            //to prevent StackOverFlow
            if (workInProgress) {
                return;
            }

            for (var i = startIndex; i < startIndex + requestedElements; i++) {
                //to handle recursive call of request from onNext()
                workInProgress = true;
                //should not throw NPE instead send it as NPE using onError()
                if (array[i] == null) {
                    subscriber.onError(new NullPointerException());
                    return;
                }
                subscriber.onNext(array[i]);
            }

            startIndex = startIndex + (int) requestedElements;
            requestedElements = 0;
            workInProgress = false;
            if (startIndex == array.length) {
                subscriber.onComplete();
                isCompleted = true;
                return;
            }
        }
    }

    @Override
    public void cancel() {
        isCancelled = true;
    }
}
