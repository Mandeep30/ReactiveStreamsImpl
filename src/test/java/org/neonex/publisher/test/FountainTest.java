package org.neonex.publisher.test;

import org.neonex.publisher.Fountain;
import org.testng.annotations.Test;
import reactor.test.StepVerifier;

import java.util.Arrays;

@SuppressWarnings("ALL")
public class FountainTest {
    @Test
    public void noElementIsEmittedOnEmptyFountain() {
        var emptyFountain = Fountain.empty();
        StepVerifier.create(emptyFountain)
                    .expectSubscription()
                    .expectNext()
                    .expectComplete()
                    .log()
                    .verify();
    }

    @Test
    public void whenFountainIsCreatedUsingFromIterable() {
        var twoElementsFountain = Fountain.fromIterable(Arrays.asList("Hello", "World", "Mandeep"));
        StepVerifier.create(twoElementsFountain)
                    .expectSubscription()
                    .expectNext("Hello", "World", "Mandeep")
                    .expectComplete()
                    .log()
                    .verify();
    }

    @Test
    public void whenFountainIsCreatedUsingJust() {
        var list = Arrays.asList("Mandeep", "Singh", "Rajpal");
        //this works like Reactor's Mono
        var twoElementsFountain = Fountain.just(Arrays.asList("Mandeep", "Singh", "Rajpal"));
        StepVerifier.create(twoElementsFountain)
                    .expectSubscription()
                    .expectNext(list)
                    .expectComplete()
                    .log()
                    .verify();
    }

    @Test
    public void testErrorFountainWhichShouldTerminateImmediatelyAfterSubscribe() {
        var errorFlux = Fountain.error(new RuntimeException());
        StepVerifier.create(errorFlux)
                    .expectSubscription()
                    .expectNext()
                    .expectError(RuntimeException.class)
                    .log()
                    .verify();

    }
}
