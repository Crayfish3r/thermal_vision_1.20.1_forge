import com.yourname.thermalvision.client.model.ThermalGogglesMotion;

public final class MotionChecks {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ThermalGogglesMotion motion = new ThermalGogglesMotion();
        motion.snap(false, 0);
        motion.setActive(true, 0);
        require(Math.abs(motion.sample(.125) - ThermalGogglesMotion.RAISED/2) < 1e-6, "source smoothstep midpoint");
        require(motion.sample(.25) == 0, "lowering duration");
        motion.setActive(false, .25);
        require(Math.abs(motion.sample(.40) - ThermalGogglesMotion.RAISED/2) < 1e-6, "raising midpoint");
        require(motion.sample(.55) == ThermalGogglesMotion.RAISED, "raising duration");

        motion.snap(false, 0);
        motion.setActive(true, 0);
        float current = motion.sample(.12);
        double dt = .00001;
        float before = motion.sample(.12 + dt);
        double velocityBefore = (before - current) / dt;
        motion.setActive(false, .12 + dt);
        require(Math.abs(motion.sample(.12+dt)-before) < 1e-7, "reversal starts at current position");
        double velocityAfter = (motion.sample(.12+2*dt)-before) / dt;
        require(Math.abs(velocityAfter-velocityBefore) < .025, "continuous angular velocity on reversal");

        ThermalGogglesMotion low = new ThermalGogglesMotion();
        ThermalGogglesMotion high = new ThermalGogglesMotion();
        low.snap(false,0); high.snap(false,0); low.setActive(true,0); high.setActive(true,0);
        for (int i=1;i<=240;i++) {
            double t=i/960.0;
            high.sample(t);
            if (i%16==0) require(Math.abs(high.sample(t)-low.sample(t))<1e-7,"FPS independent trajectory");
        }
        java.util.Random random = new java.util.Random(7);
        double time=.12+2*dt;
        boolean active=false;
        for (int i=0;i<4000;i++) {
            time += random.nextDouble()*.012;
            if (i%3==0) {
                float a=motion.sample(time);
                active=!active;
                motion.setActive(active,time);
                require(Math.abs(a-motion.sample(time))<1e-6,"rapid reversal position continuity");
            }
            float a=motion.sample(time);
            require(a >= ThermalGogglesMotion.RAISED-1e-6 && a<=1e-6,"no overshoot under rapid toggles");
        }
        motion.setActive(false,time);
        require(motion.sample(time+1)==ThermalGogglesMotion.RAISED,"settles after rapid toggles");
        float paused=motion.sample(time+1);
        require(motion.sample(time+1)==paused,"paused clock does not move pose");
        System.out.println("MOTION CHECKS PASS: timing, smoothstep, C1 reversal, 60/960 FPS, 4000 rapid samples, pause.");
    }
}
