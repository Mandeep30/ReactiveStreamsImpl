package org.neonex.publisher.test;

import org.neonex.publisher.Fountain;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


@SuppressWarnings("ALL")
public class ArrayPublisherTest extends PublisherVerification<Long> {

    public ArrayPublisherTest() {
        super(new TestEnvironment());
    }

    @Test
    public void signalsShouldBeEmittedInTheRightOrder() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        ArrayList<Integer> order = new ArrayList<>();
        long toRequest = 5L;
        Long[] array = generate(toRequest);
        Fountain<Long> publisher = Fountain.fromIterable(Arrays.asList(array));

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe");
                order.add(0);
                s.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                System.out.println("onNext " + aLong);
                collected.add(aLong);
                if (!order.contains(1)) {
                    order.add(1);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
                order.add(2);
                latch.countDown();
            }
        });

        latch.await(1, SECONDS);

        assertEquals(order, asList(0, 1, 2));
        assertEquals(collected, asList(array));
    }

    @Test
    public void mustSupportBackPressureControl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 5L;
        Long[] array = generate(toRequest);
        Fountain<Long> publisher = Fountain.fromArray(array);
        Subscription[] subscription = new Subscription[1];

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe()");
                subscription[0] = s;
            }

            @Override
            public void onNext(Long aLong) {
                System.out.printf("onNext(%s)\n", aLong);
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError()");
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete()");
                latch.countDown();
            }
        });

        assertEquals(collected, Collections.emptyList());

        subscription[0].request(1);
        assertEquals(collected, asList(0L));

        subscription[0].request(1);
        assertEquals(collected, asList(0L, 1L));

        subscription[0].request(2);
        assertEquals(collected, asList(0L, 1L, 2L, 3L));

        subscription[0].request(20);
        assertEquals(collected, asList(array));

        subscription[0].request(20);
        assertEquals(collected, asList(array));

        latch.await(1, SECONDS);
    }

    @Test
    public void mustSendNPENormally() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Long[] array = new Long[]{null};
        AtomicReference<Throwable> error = new AtomicReference<>();
        Fountain<Long> publisher = Fountain.fromArray(array);

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(4);
            }

            @Override
            public void onNext(Long aLong) {
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
            }
        });

        latch.await(1, SECONDS);

        assertTrue(error.get() instanceof NullPointerException);
    }

    @Test
    public void shouldNotDieInStackOverflow() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 3L;
        Long[] array = generate(toRequest);
        Fountain<Long> publisher = Fountain.fromArray(array);

        publisher.subscribe(new Subscriber<>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Long aLong) {
                System.out.printf("onNext(%s)\n", aLong);
                collected.add(aLong);
                //recursive call but it should serve all the elements but it should not get stuck in StackOverflow
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                System.out.println("onComplete()");
                latch.countDown();
            }
        });

        latch.await(5, SECONDS);

        assertEquals(collected, asList(array));
    }

    /**
     * one publisher and one subscriber inside that subscriber multiple thread request elements from that publisher
     *
     * @throws InterruptedException
     */
    @Test
    public void multiThreadingTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        final int n = 50000;
        Long[] array = generate(n);
        Fountain<Long> publisher = Fountain.fromArray(array);

        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                for (int i = 0; i < n; i++) {
                    commonPool().execute(() -> s.request(1));
                }
            }

            @Override
            public void onNext(Long aLong) {
                System.out.printf("onNext(%s)\n", aLong);
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                System.out.println("onComplete()");
                latch.countDown();
            }
        });

        latch.await(2, MINUTES);

        assertEquals(collected, asList(array));
    }

    @Test
    public void shouldBePossibleToCancelSubscription() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArrayList<Long> collected = new ArrayList<>();
        long toRequest = 1000L;
        Long[] array = generate(toRequest);
        Fountain<Long> publisher = Fountain.fromArray(array);

        publisher.subscribe(new Subscriber<>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
                s.request(toRequest);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        latch.await(1, SECONDS);

        assertEquals(collected, Collections.emptyList());
    }

    private static Long[] generate(long num) {
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 1000000 : num)
                .boxed()
                .toArray(Long[]::new);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Fountain.fromArray(generate(elements));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Fountain.error(new RuntimeException());
    }
}
