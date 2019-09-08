package org.neonex.fountain;

import org.reactivestreams.Subscription;

final class EmptySubscription implements Subscription {
    static final EmptySubscription INSTANCE = new EmptySubscription();

    private EmptySubscription() {
    }

    @Override
    public void request(long l) {
        //No implementation
    }

    @Override
    public void cancel() {
        //No implementation
    }
}