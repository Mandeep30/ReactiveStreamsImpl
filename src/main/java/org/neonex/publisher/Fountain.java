package org.neonex.publisher;

import com.google.common.collect.Iterators;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Class containing factory methods to instantiate a {@link Publisher}
 *
 * @param <T> type of Publisher
 * @author Mandeep Rajpal
 */
public abstract class Fountain<T> implements Publisher<T> {

    /**
     * Creates a new {@link Publisher}  the emits the items contained in the provided array.
     *
     * @param array input array which will be used to create a publisher
     * @param <T>   data type of input array
     * @return a new Publisher<T>
     */
    public static <T> Fountain<T> fromArray(T[] array) {
        if (array == null || array.length == 0) {
            return empty();
        }
        return new ArrayPublisher<>(array);
    }

    /**
     * Creates a new {@link Publisher} the emits the items contained in varargs
     *
     * @param elements varargs input elements which will be used to create a publisher
     * @param <T>      data  type of input varargs elements
     * @return a new Publisher<T>
     */
    @SafeVarargs
    public static <T> Fountain<T> just(T... elements) {
        if (elements == null || elements.length == 0) {
            return empty();
        }
        return fromArray(elements);
    }

    /**
     * Creates a new {@link Publisher} the emits the items contained in the {@link Iterable} collection
     *
     * @param <T>      data  type of elements in a iterable collection
     * @param elements of a collection which implements {@link Iterable}
     * @return a new Publisher<T>
     */
    public static <T> Fountain fromIterable(Iterable<T> elements) {
        if (elements == null) {
            return empty();
        }
        var iterator = elements.iterator();
        int size = Iterators.size(iterator);
        if (size == 0) {
            return empty();
        }
        @SuppressWarnings("unchecked")
        var array = (T[]) new Object[size];
        var index = 0;
        iterator = elements.iterator();
        while (iterator.hasNext()) {
            array[index] = iterator.next();
            index++;
        }
        return fromArray(array);
    }

    /**
     * Returns a singleton instance of an empty {@link Publisher}
     * this will send a {@link Subscription}, using onSubscribe(), which has no impl of request() and cancel()
     * and will subsequently call onComplete()
     *
     * @return a new Publisher
     */
    @SuppressWarnings("unchecked")
    public static <T> Fountain<T> empty() {
        return (Fountain<T>) EmptyPublisher.getInstance();
    }

    /**
     * Returns a {@link Publisher} with EmptySubscription and immediately signals onError after being subscribed
     *
     * @param error used while calling onError
     * @param <T>   unused in this case
     * @return a new Publisher
     */
    public static <T> Fountain<T> error(Throwable error) {
        return new ErrorPublisher<>(error);
    }

}
