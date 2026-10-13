package dev.arsconflux.internal.presentation;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreviewErrorsTest {
    record Error(String message) {}
    @Test void earlierNewViolationIsDetectedEvenWhenItPointsToTheCarrier() {
        var error = new Error("one payload at position 0");
        assertTrue(PreviewErrors.introduced(List.of(error), List.of(), Error::message).contains(error));
    }
    @Test void preexistingErrorDoesNotInvalidateAnUnrelatedCandidate() {
        var current = new Error("existing invalid earlier phrase");
        assertTrue(PreviewErrors.introduced(List.of(current), List.of(new Error(current.message())), Error::message).isEmpty());
    }
    @Test void identicalErrorsRetainMultiplicityAndIdentity() {
        var first = new Error("same");
        var second = new Error("same");
        var added = PreviewErrors.introduced(List.of(first, second), List.of(new Error("same")), Error::message);
        assertEquals(1, added.size());
        assertFalse(added.contains(first));
        assertTrue(added.contains(second));
    }
    @Test void fixingAnEarlierRuleDoesNotIntroduceErrors() {
        assertTrue(PreviewErrors.introduced(List.<Error>of(), List.of(new Error("requires payload")), Error::message).isEmpty());
    }
}
