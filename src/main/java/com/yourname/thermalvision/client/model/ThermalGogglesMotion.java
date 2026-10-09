package com.yourname.thermalvision.client.model;

/** Allocation-free scalar motion, matching thermal_goggles.motion.json (seconds/radians). */
public final class ThermalGogglesMotion {
    public static final float RAISED = (float) Math.toRadians(-100.0);
    private static final double LOWER_SECONDS = 0.25;
    private static final double RAISE_SECONDS = 0.30;
    private double angle = RAISED;
    private double velocity;
    private double startAngle = RAISED;
    private double startVelocity;
    private double target = RAISED;
    private double pendingTarget = RAISED;
    private double startTime;
    private double duration;
    private double lastTime;
    private boolean braking;
    private boolean active;

    public void snap(boolean nextActive, double now) {
        active = nextActive;
        angle = target = pendingTarget = nextActive ? 0.0 : RAISED;
        velocity = startVelocity = duration = 0.0;
        startAngle = angle;
        startTime = lastTime = now;
        braking = false;
    }

    public void setActive(boolean nextActive, double now) {
        if (active == nextActive) return;
        sample(now);
        active = nextActive;
        pendingTarget = nextActive ? 0.0 : RAISED;
        now = lastTime;
        if (velocity * (pendingTarget - angle) < 0.0) {
            double boundary = velocity > 0.0 ? 0.0 : RAISED;
            double seconds = Math.min(0.025, 2.0 * Math.abs(boundary - angle) / Math.abs(velocity));
            if (seconds > 0.000001) {
                braking = true;
                startAngle = angle;
                startVelocity = velocity;
                startTime = now;
                duration = seconds;
                return;
            }
        }
        beginEase(now);
    }

    private void beginEase(double now) {
        braking = false;
        startAngle = angle;
        startVelocity = velocity;
        target = pendingTarget;
        startTime = now;
        double distance = Math.abs(target - angle);
        duration = (active ? LOWER_SECONDS : RAISE_SECONDS) * Math.max(0.2, distance / Math.abs(RAISED));
        if (velocity * (target - angle) > 0.0) {
            // A normalized Hermite initial slope <=3 is monotone and cannot overshoot.
            duration = Math.min(duration, 3.0 * distance / Math.abs(velocity));
        }
        if (distance < 0.000001) {
            angle = target;
            velocity = duration = 0.0;
        }
    }

    public float sample(double now) {
        now = Math.max(now, lastTime);
        lastTime = now;
        if (duration <= 0.0) return (float) angle;
        double elapsed = now - startTime;
        if (braking) {
            double t = Math.min(elapsed, duration);
            angle = startAngle + startVelocity * (t - t * t / (2.0 * duration));
            velocity = startVelocity * (1.0 - t / duration);
            if (elapsed < duration) return (float) angle;
            double endTime = startTime + duration;
            velocity = 0.0;
            beginEase(endTime);
            if (duration <= 0.0) return (float) angle;
            elapsed = now - startTime;
        }
        if (elapsed >= duration) {
            angle = target;
            velocity = duration = 0.0;
            return (float) angle;
        }
        double t = Math.max(0.0, elapsed / duration);
        double t2 = t * t, t3 = t2 * t;
        angle = (2*t3 - 3*t2 + 1)*startAngle + (t3 - 2*t2 + t)*duration*startVelocity
            + (-2*t3 + 3*t2)*target;
        velocity = ((6*t2 - 6*t)*startAngle + (3*t2 - 4*t + 1)*duration*startVelocity
            + (-6*t2 + 6*t)*target) / duration;
        return (float) angle;
    }
}
