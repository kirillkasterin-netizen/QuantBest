package tech.javelin.client.modules.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.javelin.utility.game.player.rotation.Rotation;

public class FuntimeRotationHelper {

    private static class PerlinNoise {
        private final int[] p = new int[512];

        public PerlinNoise(int seed) {
            int[] perm = new int[256];
            for (int i = 0; i < 256; i++) perm[i] = i;

            java.util.Random r = new java.util.Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = r.nextInt(i + 1);
                int t = perm[i];
                perm[i] = perm[j];
                perm[j] = t;
            }

            for (int i = 0; i < 256; i++) {
                int v = perm[i] & 255;
                p[i] = v;
                p[i + 256] = v;
            }
        }

        private static float fade(float t) {
            return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
        }

        private static float lerp(float t, float a, float b) {
            return a + t * (b - a);
        }

        private static float grad(int hash, float x, float y, float z) {
            int h = hash & 15;
            float u = h < 8 ? x : y;
            float v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }

        public float noise(float x, float y) {
            float z = 0.0f;
            int X = ((int) Math.floor(x)) & 255;
            int Y = ((int) Math.floor(y)) & 255;
            int Z = ((int) Math.floor(z)) & 255;

            x = (float) (x - Math.floor(x));
            y = (float) (y - Math.floor(y));
            z = (float) (z - Math.floor(z));

            float u = fade(x);
            float v = fade(y);
            float w = fade(z);

            int A = p[X] + Y;
            int AA = p[A] + Z;
            int AB = p[A + 1] + Z;
            int B = p[X + 1] + Y;
            int BA = p[B] + Z;
            int BB = p[B + 1] + Z;

            return lerp(w,
                lerp(v,
                    lerp(u, grad(p[AA], x, y, z), grad(p[BA], x - 1.0f, y, z)),
                    lerp(u, grad(p[AB], x, y - 1.0f, z), grad(p[BB], x - 1.0f, y - 1.0f, z))),
                lerp(v,
                    lerp(u, grad(p[AA + 1], x, y, z - 1.0f), grad(p[BA + 1], x - 1.0f, y, z - 1.0f)),
                    lerp(u, grad(p[AB + 1], x, y - 1.0f, z - 1.0f), grad(p[BB + 1], x - 1.0f, y - 1.0f, z - 1.0f))));
        }

        public float fbm(float x, float y, int octaves, float lacunarity, float gain) {
            float sum = 0.0f;
            float amp = 0.5f;
            float fx = x;
            float fy = y;

            for (int i = 0; i < octaves; i++) {
                sum += noise(fx, fy) * amp;
                fx *= lacunarity;
                fy *= lacunarity;
                amp *= gain;
            }

            return sum;
        }
    }

    private final PerlinNoise perlin = new PerlinNoise(1337);
    private static final float NOISE_YAW_MIN = 0.06f;
    private static final float NOISE_YAW_MAX = 0.38f;
    private static final float NOISE_PITCH_MIN = 0.04f;
    private static final float NOISE_PITCH_MAX = 0.22f;
    private static final float NOISE_FREQ_MIN = 0.55f;
    private static final float NOISE_FREQ_MAX = 1.65f;

    private float noisePhaseYaw = 0.0f;
    private float noisePhasePitch = 0.0f;

    private Vec3d mpCurrent = Vec3d.ZERO;
    private float mpOrbit = 0.0f;
    private long mpLastUpdate = 0L;
    private int mpLastTargetId = Integer.MIN_VALUE;

    private float critHeightOffset = 0.0f;
    private long lastAttackTime = 0L;

    private float overshootYaw = 0.0f;
    private float overshootPitch = 0.0f;
    private long overshootStartTime = 0L;
    private static final float OVERSHOOT_PROB = 0.25f;
    private static final float OVERSHOOT_MIN = 0.8f;
    private static final float OVERSHOOT_MAX = 2.5f;
    private static final long OVERSHOOT_DURATION = 80;

    private float smoothedYaw = 0.0f;
    private float smoothedPitch = 0.0f;
    private long smoothLastTime = 0L;
    private int smoothTargetId = -1;
    private long targetLockTime = 0L;
    private int lastLockedTargetId = -1;
    private float currentSpeedMultiplier = 1.0f;

    public void notifyTargetChanged(LivingEntity newTarget) {
        if (newTarget == null) {
            lastLockedTargetId = -1;
            overshootYaw = 0.0f;
            overshootPitch = 0.0f;
            return;
        }

        int newId = newTarget.getId();

        if (lastLockedTargetId != newId) {
            lastLockedTargetId = newId;
            long now = System.currentTimeMillis();
            targetLockTime = now;

            currentSpeedMultiplier = rnd(0.65f, 0.95f);

            if (java.util.concurrent.ThreadLocalRandom.current().nextFloat() < OVERSHOOT_PROB) {
                overshootYaw = rnd(OVERSHOOT_MIN, OVERSHOOT_MAX) * (java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                overshootPitch = rnd(OVERSHOOT_MIN * 0.6f, OVERSHOOT_MAX * 0.6f) * (java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                overshootStartTime = now;
            } else {
                overshootYaw = 0.0f;
                overshootPitch = 0.0f;
            }
        }
    }

    public Rotation rotateTo(LivingEntity target, boolean attacking, float lastYaw, float lastPitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || target == null) return new Rotation(lastYaw, lastPitch);

        long now = System.currentTimeMillis();
        int tid = target.getId();

        if (smoothTargetId == -1) {
            smoothedYaw = lastYaw;
            smoothedPitch = lastPitch;
        }

        if (attacking && (now - lastAttackTime) > 400) {
            critHeightOffset = rnd(-0.15f, 0.15f);
            lastAttackTime = now;
        }

        float k = 0.035f;
        noisePhaseYaw += k * (0.65f + 0.35f * ((tid * 1103515245) & 255) / 255.0f);
        noisePhasePitch += k * (0.65f + 0.35f * ((tid * 1664525) & 255) / 255.0f);

        Vec3d targetPoint = getMultiPoint(target, attacking, now);

        Vec3d eyes = mc.player.getEyePos();
        Vec3d direction = targetPoint.subtract(eyes);

        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(direction.y, horizontalDistance)));

        targetYaw = MathHelper.wrapDegrees(targetYaw);
        targetPitch = clamp(targetPitch, -90.0f, 90.0f);

        if (smoothTargetId != tid) {
            smoothTargetId = tid;
            smoothLastTime = now;
        }

        long dtMs = now - smoothLastTime;
        if (dtMs < 1) dtMs = 1;
        if (dtMs > 120) dtMs = 120;
        smoothLastTime = now;

        long timeSinceLock = now - targetLockTime;
        float lockProgress = Math.min(1.0f, timeSinceLock / 1000.0f);

        float speedMultiplier = lockProgress * lockProgress * (3.0f - 2.0f * lockProgress);
        speedMultiplier = 0.35f + speedMultiplier * 0.65f;
        speedMultiplier *= currentSpeedMultiplier;

        float baseYawSpeed = attacking ? 55.0f : 60.0f;
        float basePitchSpeed = attacking ? 44.0f : 48.0f;

        float yawSpeed = baseYawSpeed * speedMultiplier;
        float pitchSpeed = basePitchSpeed * speedMultiplier;

        float yawMaxDelta = yawSpeed * ((float) dtMs / 50.0f);
        float pitchMaxDelta = pitchSpeed * ((float) dtMs / 50.0f);

        float yawError = MathHelper.wrapDegrees(targetYaw - smoothedYaw);
        float pitchError = targetPitch - smoothedPitch;

        smoothedYaw += clamp(yawError, -yawMaxDelta, yawMaxDelta);
        smoothedPitch += clamp(pitchError, -pitchMaxDelta, pitchMaxDelta);

        smoothedYaw = MathHelper.wrapDegrees(smoothedYaw);
        smoothedPitch = clamp(smoothedPitch, -90.0f, 90.0f);

        float overshootFactor = 0.0f;
        if (overshootYaw != 0.0f || overshootPitch != 0.0f) {
            long timeSinceOvershoot = now - overshootStartTime;
            if (timeSinceOvershoot < OVERSHOOT_DURATION) {
                overshootFactor = 1.0f - ((float) timeSinceOvershoot / OVERSHOOT_DURATION);
                overshootFactor = overshootFactor * overshootFactor;
            } else {
                overshootYaw = 0.0f;
                overshootPitch = 0.0f;
            }
        }

        float jitterYaw = getJitter(now, tid, true);
        float jitterPitch = getJitter(now, tid, false);

        float finalYaw = smoothedYaw + jitterYaw + (overshootYaw * overshootFactor);
        float finalPitch = smoothedPitch + jitterPitch + (overshootPitch * overshootFactor);

        finalYaw = MathHelper.wrapDegrees(finalYaw);
        finalPitch = clamp(finalPitch, -90.0f, 90.0f);

        return new Rotation(finalYaw, finalPitch);
    }

    public Vec3d getMultiPoint(LivingEntity target, boolean attacking, long now) {
        int tid = target.getId();

        if (tid != mpLastTargetId) {
            mpLastTargetId = tid;
            mpCurrent = Vec3d.ZERO;
            mpOrbit = rnd(0.0f, (float) (Math.PI * 2.0));
            mpLastUpdate = now;
        }

        long dtMs = now - mpLastUpdate;
        if (dtMs < 1) dtMs = 1;
        if (dtMs > 60) dtMs = 60;
        mpLastUpdate = now;

        float orbitSpeed = 0.36f;
        float dtScale = (float) dtMs / 50.0f;
        mpOrbit += orbitSpeed * dtScale + rnd(-0.018f, 0.018f) * dtScale;

        Vec3d desired = calculateOrbitPoint(mpOrbit, attacking);

        float tau = 120.0f;
        float alpha = (float) dtMs / tau;
        alpha = clamp(alpha, 0.0f, 1.0f);
        alpha = alpha * alpha * (3.0f - 2.0f * alpha);

        if (mpCurrent.equals(Vec3d.ZERO)) {
            mpCurrent = desired;
        } else {
            mpCurrent = new Vec3d(
                lerp((float) mpCurrent.x, (float) desired.x, alpha),
                lerp((float) mpCurrent.y, (float) desired.y, alpha),
                lerp((float) mpCurrent.z, (float) desired.z, alpha)
            );
        }

        Box box = target.getBoundingBox();
        double fx = 0.5 + mpCurrent.x * 0.5;
        double fy = mpCurrent.y;
        double fz = 0.5 + mpCurrent.z * 0.5;

        fx = clamp(fx, 0.0, 1.0);
        fy = clamp(fy, 0.36, 0.82);
        fz = clamp(fz, 0.0, 1.0);

        double x = box.minX + (box.maxX - box.minX) * fx;
        double y = box.minY + (box.maxY - box.minY) * fy;
        double z = box.minZ + (box.maxZ - box.minZ) * fz;

        return new Vec3d(
            clamp(x, box.minX + 1.0E-4, box.maxX - 1.0E-4),
            clamp(y, box.minY + 1.0E-4, box.maxY - 1.0E-4),
            clamp(z, box.minZ + 1.0E-4, box.maxZ - 1.0E-4)
        );
    }

    private Vec3d calculateOrbitPoint(float orbit, boolean attacking) {
        float o = orbit;

        float y = 0.56f
            + 0.10f * (float) Math.sin(o * 0.74f + 0.9f)
            + 0.06f * (float) Math.sin(o * 1.28f + 2.2f)
            - 0.05f * (float) (0.5 + 0.5 * Math.sin(o * 0.35f + 1.7f))
            + critHeightOffset;

        float x = (0.22f * (float) Math.sin(o))
            + (0.06f * (float) Math.sin(o * 1.85f + 0.25f));

        float z = (0.08f * (float) Math.cos(o * 0.92f + 0.4f))
            + (0.03f * (float) Math.sin(o * 1.35f + 1.1f));

        float jitter = 0.020f;
        x += rnd(-jitter, jitter);
        z += rnd(-jitter, jitter);
        y += rnd(-jitter * 0.55f, jitter * 0.55f);

        x = clamp(x, -0.46f, 0.46f);
        z = clamp(z, -0.36f, 0.36f);
        y = clamp(y, 0.36f, 0.82f);

        return new Vec3d(x, y, z);
    }

    private float getJitter(long now, int tid, boolean isYaw) {
        float amp = isYaw
            ? lerp(NOISE_YAW_MIN, NOISE_YAW_MAX, 0.5f)
            : lerp(NOISE_PITCH_MIN, NOISE_PITCH_MAX, 0.5f);

        float freq = lerp(NOISE_FREQ_MIN, NOISE_FREQ_MAX, 0.5f);

        float phase = isYaw ? noisePhaseYaw : noisePhasePitch;
        float tt = (float) ((now % 100000L) / 1000.0);

        float noise = perlin.fbm((tt * freq + phase), (float) (tid * 0.013 + 0.11), 4, 2.0f, 0.55f);
        float sway = perlin.noise((tt * 0.22f + phase * 0.07f), (float) (tid * 0.009 + 9.3));

        return (noise * 0.72f + sway * 0.28f) * amp;
    }

    public void reset() {
        noisePhaseYaw = rnd(0.0f, 999.0f);
        noisePhasePitch = rnd(0.0f, 999.0f);
        mpCurrent = Vec3d.ZERO;
        mpOrbit = 0.0f;
        mpLastUpdate = 0L;
        mpLastTargetId = Integer.MIN_VALUE;
        smoothedYaw = 0.0f;
        smoothedPitch = 0.0f;
        smoothLastTime = 0L;
        smoothTargetId = -1;
        targetLockTime = 0L;
        lastLockedTargetId = -1;
        currentSpeedMultiplier = 1.0f;
        overshootYaw = 0.0f;
        overshootPitch = 0.0f;
    }

    private float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float rnd(float a, float b) {
        if (b <= a) return a;
        return a + java.util.concurrent.ThreadLocalRandom.current().nextFloat() * (b - a);
    }
}
