package dev.arsconflux.internal.projectile;

import java.util.*;
import java.util.function.Function;

/** Pure reflection math and group eligibility, independent of any projectile provider. */
public final class ReflectionRules {
    private ReflectionRules() {}
    public record Velocity(double x,double y,double z) {}
    public static Velocity reflect(double x,double y,double z,double nx,double ny,double nz) {
        double speed=x*x+y*y+z*z, length=nx*nx+ny*ny+nz*nz;
        if(!Double.isFinite(speed) || speed<=1.0e-7 || !Double.isFinite(length) || length<=1.0e-12)return null;
        double dot=(x*nx+y*ny+z*nz)/length;
        return new Velocity(x-2*dot*nx,y-2*dot*ny,z-2*dot*nz);
    }
    /** A shared activation may transfer its damage owner only if it owns this single flight. */
    public static <T,K> boolean sameFlight(List<T> members,T hit,Function<T,K> carrier) {
        K key=carrier.apply(hit);
        return members.stream().allMatch(member->member==hit || key!=null && carrier.apply(member)==key);
    }
    public static <T> T damageOwner(T declared,T caster,T executor,T reflectedOwner) {
        return declared!=null && reflectedOwner!=null && (declared==caster || declared==executor)?reflectedOwner:declared;
    }
}
