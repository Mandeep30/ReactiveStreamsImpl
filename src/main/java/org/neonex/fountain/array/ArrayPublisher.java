package org.neonex.fountain.array;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Arrays;

public class ArrayPublisher<T> implements Publisher<T> {
    private T[] array;

    public ArrayPublisher(T[] array) {
        this.array = Arrays.copyOf(array, array.length);
    }

    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new ArraySubscription<>(array, subscriber));
    }
}
