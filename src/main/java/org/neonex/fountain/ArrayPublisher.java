package org.neonex.fountain;

import org.reactivestreams.Subscriber;

import java.util.Arrays;

/**
 * @param <T>
 * @author Mandeep Rajpal
 */
final class ArrayPublisher<T> extends Fountain<T> {
    private final T[] array;

    ArrayPublisher(T[] array) {
        this.array = Arrays.copyOf(array, array.length);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new ArraySubscription<>(array, subscriber));
    }
}
