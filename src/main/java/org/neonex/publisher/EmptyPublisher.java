package org.neonex.publisher;

import org.reactivestreams.Subscriber;

/**
 * @param <T>
 * @author Mandeep Rajpal
 */
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
