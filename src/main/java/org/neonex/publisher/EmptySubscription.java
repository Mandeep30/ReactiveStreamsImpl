package org.neonex.publisher;

import org.reactivestreams.Subscription;

/**
 * @author Mandeep Rajpal
 */
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
