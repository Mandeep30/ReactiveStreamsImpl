package org.neonex.fountain;

import org.reactivestreams.Subscriber;

final class EmptyPublisher<T> extends Fountain<T> {
    private final static Fountain INSTANCE = new EmptyPublisher();

    private EmptyPublisher() {

    }

    static Fountain getInstance() {
        return INSTANCE;
    }

    @Override
    public void subscribe(Subscriber subscriber) {
        subscriber.onSubscribe(EmptySubscription.INSTANCE);
        subscriber.onComplete();
    }
}
