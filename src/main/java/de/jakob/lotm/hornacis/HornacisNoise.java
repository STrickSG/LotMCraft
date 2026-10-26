package de.jakob.lotm.hornacis;

/**
 * Ruído de gradiente 2D (Perlin melhorado) com permutação própria, determinístico pela seed.
 * Sem dependências do Minecraft: o mesmo código gera o terreno no jogo e as prévias fora dele.
 */
public final class HornacisNoise {
    private static final double[] GX = {1, -1, 1, -1, 1.41421356, -1.41421356, 0, 0};
    private static final double[] GZ = {1, 1, -1, -1, 0, 0, 1.41421356, -1.41421356};

    private final int[] perm = new int[512];
    private final double offX, offZ;

    public HornacisNoise(long seed) {
        long s = seed;
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        for (int i = 255; i > 0; i--) {
            s = mix(s + 0x9E3779B97F4A7C15L);
            int j = (int) Long.remainderUnsigned(s, i + 1);
            int t = p[i]; p[i] = p[j]; p[j] = t;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        s = mix(s + 0x9E3779B97F4A7C15L);
        offX = (s >>> 11) * 0x1.0p-53 * 4096.0;
        s = mix(s + 0x9E3779B97F4A7C15L);
        offZ = (s >>> 11) * 0x1.0p-53 * 4096.0;
    }

    /** Valor em torno de [-1, 1]. */
    public double sample(double x, double z) {
        x += offX;
        z += offZ;
        int xi = floor(x), zi = floor(z);
        double xf = x - xi, zf = z - zi;
        int X = xi & 255, Z = zi & 255;
        double u = fade(xf), v = fade(zf);
        double n00 = grad(perm[perm[X] + Z], xf, zf);
        double n10 = grad(perm[perm[X + 1] + Z], xf - 1, zf);
        double n01 = grad(perm[perm[X] + Z + 1], xf, zf - 1);
        double n11 = grad(perm[perm[X + 1] + Z + 1], xf - 1, zf - 1);
        double nx0 = n00 + u * (n10 - n00);
        double nx1 = n01 + u * (n11 - n01);
        return (nx0 + v * (nx1 - nx0)) * 1.15;
    }

    private static double grad(int h, double x, double z) {
        h &= 7;
        return GX[h] * x + GZ[h] * z;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static int floor(double d) {
        int i = (int) d;
        return d < i ? i - 1 : i;
    }

    /** SplitMix64: espalha os bits de uma seed. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
