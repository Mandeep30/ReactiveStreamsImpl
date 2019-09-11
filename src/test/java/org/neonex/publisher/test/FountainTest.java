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
                    .verify();
    }

    @Test
    public void expectTwoElementsWhenFountainIsCreatedUsingFromIterable() {
        var twoElementsFountain = Fountain.fromIterable(Arrays.asList("Hello", "World"));
        StepVerifier.create(twoElementsFountain)
                    .expectSubscription()
                    .expectNext("Hello", "World")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void expectTwoElementsWhenFountainIsCreatedUsingJust() {
        var twoElementsFountain = Fountain.just("Mandeep", "Singh", "Rajpal");
        StepVerifier.create(twoElementsFountain)
                    .expectSubscription()
                    .expectNext("Mandeep", "Singh", "Rajpal")
                    .expectComplete()
                    .verify();
    }
}
