package xyz.rimvisuals.auth;

/** Monotonic, short lived lease. A network error never extends it. */
public final class AccessLease {
    private volatile long deadline;
    public boolean active(long now) { return deadline != 0 && now < deadline; }
    public void grant(long now, int seconds) {
        deadline = seconds <= 0 ? 0 : now + Math.min(seconds, 75) * 1_000_000_000L;
    }
    public void clear() { deadline = 0; }
}
