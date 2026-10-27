package dev.arsconflux;

import dev.arsconflux.internal.billing.ForwardedGroups;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForwardedGroupsTest {
    private List<String> unpaid(String... parts) {
        return ForwardedGroups.unmanaged(List.of(parts),p->p.startsWith("+"),p->p.startsWith("paid:"),p->!p.equals("form"));
    }
    @Test void dispatchOwnedEffectsAndTheirWrittenAugmentsAreNotQuotedAgain() {
        assertEquals(List.of("native", "+native"),unpaid("paid:ars_damage","+amplify","paid:iron","+split","native","+native"));
    }
    @Test void leadingGeometryAugmentsAndEmptyRemaindersHaveNoRepeatedPrice() {
        assertEquals(List.of(),unpaid("+sphere","+hollow","paid:damage","+amplify"));
        assertEquals(List.of(),unpaid("form","+accelerate"));
        assertEquals(List.of(),unpaid());
    }
    @Test void unmanagedGroupsRetainTheirOwnAugmentsAndWrittenDuplicates() {
        assertEquals(List.of("native","+one","native","+two"),unpaid("+shape","native","+one","paid:damage","+three","native","+two"));
    }
}
