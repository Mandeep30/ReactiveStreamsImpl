package org.neonex.publisher;

import org.reactivestreams.Subscriber;

final class ErrorPublisher<T> extends Fountain<T> {
    private Throwable error;

    ErrorPublisher(Throwable error) {
        this.error = error;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(EmptySubscription.INSTANCE);
        subscriber.onError(error);
    }
}
