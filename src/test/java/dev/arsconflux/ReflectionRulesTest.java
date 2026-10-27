package dev.arsconflux;

import dev.arsconflux.internal.projectile.ReflectionRules;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionRulesTest {
    @Test void reflectionPreservesSpeedAndWorksWithAnUnnormalizedSurfaceNormal() {
        var result=ReflectionRules.reflect(3,-4,2,0,5,0);
        assertEquals(3,result.x(),1e-9); assertEquals(4,result.y(),1e-9); assertEquals(2,result.z(),1e-9);
        assertEquals(29,result.x()*result.x()+result.y()*result.y()+result.z()*result.z(),1e-9);
    }
    @Test void tangentAndDoubleReflectionAreStable() {
        var tangent=ReflectionRules.reflect(2,0,0,0,1,0); assertEquals(2,tangent.x(),1e-9); assertEquals(0,tangent.y(),1e-9);
        var first=ReflectionRules.reflect(1,2,3,1,1,1);
        var second=ReflectionRules.reflect(first.x(),first.y(),first.z(),1,1,1);
        assertEquals(1,second.x(),1e-9); assertEquals(2,second.y(),1e-9); assertEquals(3,second.z(),1e-9);
    }
    @Test void invalidAndStationaryFlightsAreRejected() {
        assertNull(ReflectionRules.reflect(0,0,0,1,0,0)); assertNull(ReflectionRules.reflect(1,0,0,0,0,0));
        assertNull(ReflectionRules.reflect(Double.NaN,0,0,1,0,0)); assertNull(ReflectionRules.reflect(1,0,0,Double.POSITIVE_INFINITY,0,0));
    }
    @Test void nativeSalvosShareOneFlightWhileIndependentProjectilesCannotTransferTogether() {
        record Member(Object carrier) {}
        var flight=new Object(); var a=new Member(flight); var b=new Member(flight); var other=new Member(new Object());
        assertTrue(ReflectionRules.sameFlight(List.of(a,b),a,Member::carrier));
        assertFalse(ReflectionRules.sameFlight(List.of(a,other),a,Member::carrier));
        var nativeOnly=new Member(null);
        assertTrue(ReflectionRules.sameFlight(List.of(nativeOnly),nativeOnly,Member::carrier));
        assertFalse(ReflectionRules.sameFlight(List.of(nativeOnly,new Member(null)),nativeOnly,Member::carrier));
    }
    @Test void damageCreditChangesOnlyTheParticipatingCasterOrExecutor() {
        var caster=new Object(); var executor=new Object(); var defender=new Object(); var unrelated=new Object();
        assertSame(defender,ReflectionRules.damageOwner(caster,caster,executor,defender));
        assertSame(defender,ReflectionRules.damageOwner(executor,caster,executor,defender));
        assertSame(unrelated,ReflectionRules.damageOwner(unrelated,caster,executor,defender));
        assertSame(caster,ReflectionRules.damageOwner(caster,caster,executor,null));
        assertNull(ReflectionRules.damageOwner(null,caster,executor,defender));
    }
}
