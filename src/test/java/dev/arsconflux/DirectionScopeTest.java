package dev.arsconflux;

import dev.arsconflux.internal.ScopedContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectionScopeTest {
    @Test void nativeImpactOverridesThenRestoresAnOuterProjectileDirection() {
        var scope=new ScopedContext<String>();
        scope.run("projectile",()->{
            scope.run("voxel impact",()->assertEquals("voxel impact",scope.current()));
            assertEquals("projectile",scope.current());
        });
        assertNull(scope.current());
    }
    @Test void explicitStationaryValueMasksOldDirectionAndExceptionDoesNotLeakScope() {
        var scope=new ScopedContext<String>();
        scope.run("old direction",()->{
            assertThrows(IllegalStateException.class,()->scope.run("",()->{
                assertEquals("",scope.current()); throw new IllegalStateException("test");
            }));
            assertEquals("old direction",scope.current());
        });
        assertNull(scope.current());
    }
    @Test void unspecifiedDirectionInheritsTheOuterScope() {
        var scope=new ScopedContext<String>();
        scope.run("beam",()->scope.run(null,()->assertEquals("beam",scope.current())));
        assertNull(scope.current());
    }
}
