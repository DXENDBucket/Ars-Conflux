package dev.arsconflux.api.execution;

import dev.arsconflux.api.context.TriggerGeometry;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.resource.ResourceAccount;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import java.util.Objects;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import dev.arsconflux.internal.EntityTransfers;
import java.util.function.BooleanSupplier;

/** A transient server execution. Permission, parameter semantics and payment are provider supplied.
 * This is not a persistence format for summons or long-lived areas. */
public final class ExecutionSession<P> {
    public enum State { PREPARED, ACTIVE, COMPLETED, CANCELLED }
    public enum EndReason { COMPLETED, EXPIRED, OWNER_GONE, DENIED, WORLD_UNLOADED, SPAWN_FAILED, SERVER_STOPPED, ERROR }
    private final UUID id = UUID.randomUUID();
    private ServerLevel world;
    private final ServerLevel originWorld;
    private final Set<Entity> projectiles = new LinkedHashSet<>();
    private boolean capturingProjectiles;
    private final LivingEntity caster;
    private CastContext context;
    private final P parameters;
    private final int maxTicks;
    private final long started;
    private final ExecutionDriver<P> driver;
    private final ImpactContinuation continuation;
    private final BooleanSupplier permission;
    private final ExecutionScope scope;
    private TriggerPayment payment = TriggerPayment.FREE;
    private boolean repeatPayments;
    private java.util.List<ExecutionClaim> claims;
    private State state = State.PREPARED;

    public ExecutionSession(ServerLevel world, LivingEntity caster, P parameters, int maxTicks,
                            ExecutionDriver<P> driver, ImpactContinuation continuation,
                            BooleanSupplier permission, ExecutionScope scope) {
        this(world, CastContext.detached(caster, null, null), parameters, maxTicks, driver, continuation, permission, scope);
    }
    public ExecutionSession(ServerLevel world, CastContext context, P parameters, int maxTicks,
                            ExecutionDriver<P> driver, ImpactContinuation continuation,
                            BooleanSupplier permission, ExecutionScope scope) {
        this.world = Objects.requireNonNull(world); this.originWorld = world; this.context = Objects.requireNonNull(context);
        this.caster = Objects.requireNonNull(context.caster());
        this.parameters = parameters; this.maxTicks = Math.max(1, maxTicks);
        this.driver = Objects.requireNonNull(driver); this.continuation = Objects.requireNonNull(continuation);
        this.permission = Objects.requireNonNull(permission); Objects.requireNonNull(scope);
        this.scope = action -> ExecutionSessions.scoped(this, () -> CastContexts.run(this.context, () -> scope.run(action)));
        started = world.getGameTime();
    }
    public UUID id() { return id; }
    public ServerLevel world() { return world; }
    public ServerLevel originWorld() { return originWorld; }
    public boolean usesWorld(ServerLevel level) { return world == level || originWorld == level; }
    /** Explicitly register a provider-owned visual, including visuals not derived from Projectile. */
    public void trackProjectile(Entity entity) {
        if (active() && entity.level() == world && !entity.isRemoved()) projectiles.add(entity);
    }
    /** Internal spawn hook: only capture projectiles produced by driver.start, not impact-created children. */
    public void captureProjectile(Entity entity) {
        if (capturingProjectiles && entity instanceof Projectile) trackProjectile(entity);
    }
    /** Explicit remote trajectory migration; the caster stays in its origin world. */
    public boolean relocateProjectiles(ServerLevel destination, Vec3 displacement) {
        if (!active()) return false;
        world = destination;
        if (context.ars() != null) ((dev.arsconflux.mixin.SpellContextAccess)context.ars()).arsConflux$level(destination);
        for (var entity : java.util.List.copyOf(projectiles)) {
            if (entity.isRemoved()) { projectiles.remove(entity); continue; }
            if (!EntityTransfers.projectile(entity, destination, entity.position().add(displacement))) {
                projectiles.forEach(Entity::discard);
                finish(EndReason.SPAWN_FAILED); return false;
            }
        }
        return true;
    }
    public LivingEntity caster() { return caster; }
    public CastContext context() { return context; }
    public ExecutionSession<P> account(ResourceAccount account) {
        if (state != State.PREPARED) throw new IllegalStateException("Account must be set before start");
        context = context.withAccount(account); return this;
    }
    public P parameters() { return parameters; }
    public State state() { return state; }
    public boolean active() { return state == State.ACTIVE; }
    public boolean occupiesCaster() { return driver.occupiesCaster(); }
    public java.util.List<ExecutionClaim> claims() {
        if(claims==null)claims=java.util.List.copyOf(driver.claims(this));
        return claims;
    }
    public int remainingTicks() { return (int)Math.max(0, Math.min(Integer.MAX_VALUE, maxTicks - (originWorld.getGameTime() - started))); }
    public ExecutionSession<P> billing(TriggerPayment payment, boolean repeat) {
        if (state != State.PREPARED) throw new IllegalStateException("Billing must be set before start");
        this.payment = Objects.requireNonNull(payment); repeatPayments = repeat; return this;
    }
    private boolean ownerPresent() {
        var executor = context.executor();
        return caster.isAlive() && !caster.isRemoved() && caster.level() == originWorld
                && (executor == null || executor.isAlive() && !executor.isRemoved() && executor.level() == originWorld);
    }
    public boolean permitted() { return scope.call(() -> ownerPresent() && permission.getAsBoolean()); }
    public boolean activate(BooleanSupplier action) {
        return scope.call(() -> active() && permitted() && (repeatPayments ? payment.trigger(action) : action.getAsBoolean()));
    }
    public boolean start() {
        return scope.call(() -> {
            if (state != State.PREPARED) return false;
            if (!permitted()) { finish(EndReason.DENIED); return false; }
            state = State.ACTIVE;
            try {
                boolean success = repeatPayments ? startDriver() : payment.trigger(this::startDriver);
                if (!success) finish(EndReason.SPAWN_FAILED);
                return success && active();
            } catch (RuntimeException | Error error) { fail(error); throw error; }
        });
    }
    private boolean startDriver() {
        capturingProjectiles = true;
        try { return driver.start(this); } finally { capturingProjectiles = false; }
    }
    public void tick() {
        scope.run(() -> {
            if (!active()) return;
            if (!ownerPresent()) { finish(EndReason.OWNER_GONE); return; }
            long age = originWorld.getGameTime() - started;
            if (age >= maxTicks) { finish(EndReason.EXPIRED); return; }
            if (age % 20 == 0 && !permitted()) { finish(EndReason.DENIED); return; }
            try { if (driver.tick(this)) finish(EndReason.COMPLETED); }
            catch (RuntimeException | Error error) { fail(error); throw error; }
        });
    }
    /** Report successful impacts only; billing belongs to the execution, not continuation dispatch. */
    public void impact(HitResult hit) {
        if (!active() || !permitted()) return;
        var next = context.withTarget(hit).withExecutor(caster).withIncomingDirection(driver.incomingDirection());
        scope.run(() -> CastContexts.run(next, () -> TriggerGeometry.scoped(next.incomingDirection(), () -> continuation.resolve(hit))));
    }
    public void finish(EndReason reason) {
        if (state == State.COMPLETED || state == State.CANCELLED) return;
        state = reason == EndReason.COMPLETED ? State.COMPLETED : State.CANCELLED;
        scope.run(() -> {
            try { driver.close(this, reason); } finally {
                try { continuation.close(); } finally { projectiles.clear(); }
            }
        });
    }
    private void fail(Throwable error) {
        try { finish(EndReason.ERROR); } catch (RuntimeException | Error cleanup) { error.addSuppressed(cleanup); }
    }
}
