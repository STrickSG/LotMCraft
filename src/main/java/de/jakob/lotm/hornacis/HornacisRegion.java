package de.jakob.lotm.hornacis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Cordilheira de Hornacis de um mundo: onde fica, a forma, a altura de cada coluna, as zonas (sub-biomas)
 * e os lugares das estruturas. Tudo sai da seed do mundo, sem nada do Minecraft, então o mesmo código
 * desenha o terreno no jogo e as prévias fora dele.
 *
 * Na novel: a cordilheira separa Loen de Intis, o pico principal passa de 6000 m e é o mais alto;
 * no cume ficava a Nação da Noite Eterna, com relíquias também nas florestas, vales e encostas suaves em volta.
 *
 * O relevo é montado em duas etapas: uma forma bruta (envelope da cadeia, cristas, picos e o Pico Principal)
 * e depois uma simulação de erosão pela água numa grade de 8 blocos, que cava os vales que se ramificam
 * como rios e deixa as cristas afiadas. Só StrictMath: o resultado é o mesmo em qualquer máquina.
 */
public final class HornacisRegion {
    public static final double LENGTH = 5000;
    public static final double HALF_WIDTH = 900;
    /** Teto do terreno (o mundo vai até 640) */
    public static final int MAX_TERRAIN = 616;
    /** Altura do platô do cume, onde fica o palácio */
    public static final int SUMMIT_Y = 560;
    public static final double MAIN_RADIUS = 760;
    private static final double BASE = 70;

    public enum Zone { NONE, FOOTHILLS, HIGHLANDS, SLOPES, GLACIER, PEAKS, MAIN_PEAK }

    public enum SiteType { PALACE, SUMMIT_RUIN, RUIN, CAMP, MONASTERY }

    public record Site(SiteType type, int index, int x, int z, int y, int radius, int rotation) {}

    /** Diz se um ponto do mundo vanilla é terra firme (para não pôr a cordilheira no meio do mar). */
    public interface LandTest {
        boolean isLand(int x, int z);
    }

    /** Resultado de uma coluna (reaproveitado para não alocar) */
    public static final class Sample {
        public double mask, height, envelope, valley, mainDist, raw;
        public Zone zone = Zone.NONE;
        double u, v, vv, w, tu;
    }

    public final long seed;
    public final double centerX, centerZ, cos, sin;
    public final double mainX, mainZ, mainAngle;
    public final int minX, minZ, maxX, maxZ;

    private final double spineA1, spineF1, spineP1, spineA2, spineF2, spineP2;
    private final HornacisNoise warpA, warpB, widthNoise, gully, dither, detailA, detailB, ridgeWarpA, ridgeWarpB;
    private final HornacisNoise[] ridge = new HornacisNoise[6];
    private final double[] peakX, peakZ, peakR, peakH;
    private final double[] passU, passDepth;
    private final double[] spireX, spireZ;
    private final double mainU;

    /** Grade já erodida, em coordenadas da cordilheira (u ao longo, v através) */
    private static final double CELL = 8;
    private float[] grid;
    private int gridU, gridV;
    private double gridU0, gridV0;

    private final List<Site> sites = new ArrayList<>();
    private Site[][] siteGrid;
    private int siteMinX, siteMinZ, siteW, siteH;
    private static final int SITE_CELL = 128;

    /** Células de 64 blocos onde o terreno passa de ~300 (só ali o ruído é calculado acima de 320) */
    private boolean[] tall;
    private int tallW, tallH;

    private HornacisRegion(long seed, double cx, double cz, double angle, long[] r) {
        this.seed = seed;
        this.centerX = cx;
        this.centerZ = cz;
        this.cos = StrictMath.cos(angle);
        this.sin = StrictMath.sin(angle);
        int k = 0;
        spineA1 = 240 + unit(r[k++]) * 140;
        spineF1 = 0.55 + unit(r[k++]) * 0.35;
        spineP1 = unit(r[k++]) * Math.PI * 2;
        spineA2 = 60 + unit(r[k++]) * 60;
        spineF2 = 1.6 + unit(r[k++]) * 0.8;
        spineP2 = unit(r[k++]) * Math.PI * 2;
        warpA = new HornacisNoise(r[k++]);
        warpB = new HornacisNoise(r[k++]);
        widthNoise = new HornacisNoise(r[k++]);
        gully = new HornacisNoise(r[k++]);
        dither = new HornacisNoise(r[k++]);
        detailA = new HornacisNoise(r[k++]);
        detailB = new HornacisNoise(r[k++]);
        ridgeWarpA = new HornacisNoise(r[k++]);
        ridgeWarpB = new HornacisNoise(r[k++]);
        for (int i = 0; i < ridge.length; i++) ridge[i] = new HornacisNoise(r[k++]);

        // o pico principal fica na espinha, perto do meio
        mainU = (unit(r[k++]) - 0.5) * 0.3 * LENGTH;
        double mv = spine(mainU);
        mainX = toWorldX(mainU, mv);
        mainZ = toWorldZ(mainU, mv);
        mainAngle = unit(r[k++]) * Math.PI * 2;

        // picos secundários ao longo da espinha (longe do principal)
        List<double[]> peaks = new ArrayList<>();
        int count = 17;
        for (int i = 0; i < count; i++) {
            double pu = -LENGTH * 0.44 + i * (LENGTH * 0.88 / (count - 1)) + (unit(r[k++]) - 0.5) * 200;
            double pv = spine(pu) + (unit(r[k++]) - 0.5) * HALF_WIDTH * 0.6;
            double rad = 170 + unit(r[k++]) * 160;
            double h = 60 + unit(r[k++]) * 80;
            if (Math.abs(pu - mainU) < MAIN_RADIUS * 0.8) continue;
            peaks.add(new double[]{toWorldX(pu, pv), toWorldZ(pu, pv), rad, h});
        }
        peakX = new double[peaks.size()];
        peakZ = new double[peaks.size()];
        peakR = new double[peaks.size()];
        peakH = new double[peaks.size()];
        for (int i = 0; i < peaks.size(); i++) {
            peakX[i] = peaks.get(i)[0]; peakZ[i] = peaks.get(i)[1];
            peakR[i] = peaks.get(i)[2]; peakH[i] = peaks.get(i)[3];
        }

        // passagens: onde a crista baixa e dá para atravessar a cordilheira
        passU = new double[3];
        passDepth = new double[3];
        for (int i = 0; i < passU.length; i++) {
            double pu = 0;
            for (int t = 0; t < 20; t++) {
                pu = (unit(r[k++]) - 0.5) * LENGTH * 0.8;
                if (Math.abs(pu - mainU) > MAIN_RADIUS) break;
            }
            passU[i] = pu;
            passDepth[i] = 0.3 + unit(r[k++]) * 0.2;
        }

        // três agulhas de pedra em volta do platô do cume (os “chifres” de Hornacis)
        spireX = new double[3];
        spireZ = new double[3];
        for (int i = 0; i < 3; i++) {
            double a = mainAngle + Math.PI / 3 + i * Math.PI * 2 / 3;
            spireX[i] = mainX + StrictMath.cos(a) * 76;
            spireZ[i] = mainZ + StrictMath.sin(a) * 76;
        }

        // caixa que contém toda a cordilheira (com folga)
        double hl = LENGTH / 2 * 1.12 + 200;
        double hw = HALF_WIDTH * 1.25 * 1.3 + spineA1 + spineA2 + 200;
        double ex = Math.abs(cos) * hl + Math.abs(sin) * hw;
        double ez = Math.abs(sin) * hl + Math.abs(cos) * hw;
        minX = (int) Math.floor(cx - ex);
        maxX = (int) Math.ceil(cx + ex);
        minZ = (int) Math.floor(cz - ez);
        maxZ = (int) Math.ceil(cz + ez);
    }

    /** Monta a cordilheira de um mundo. A posição é sorteada a até ~6000 blocos do spawn, de preferência em terra. */
    public static HornacisRegion create(long worldSeed, LandTest land) {
        long s = HornacisNoise.mix(worldSeed ^ 0x484F524E41434953L);
        long[] r = new long[200];
        for (int i = 0; i < r.length; i++) {
            s = HornacisNoise.mix(s + 0x9E3779B97F4A7C15L);
            r[i] = s;
        }
        double angle = unit(r[0]) * Math.PI;
        long pick = r[1];
        double bestScore = -1, bx = 0, bz = 0;
        for (int attempt = 0; attempt < 48; attempt++) {
            pick = HornacisNoise.mix(pick + 0x9E3779B97F4A7C15L);
            double dist = 6000 * Math.sqrt(unit(pick));
            pick = HornacisNoise.mix(pick + 0x9E3779B97F4A7C15L);
            double a = unit(pick) * Math.PI * 2;
            double cx = StrictMath.cos(a) * dist, cz = StrictMath.sin(a) * dist;
            int landCount = 0, total = 0;
            for (int i = -5; i <= 5; i++) {
                for (int j = -1; j <= 1; j++) {
                    double u = i * LENGTH * 0.09, v = j * 520;
                    int x = (int) (cx + u * StrictMath.cos(angle) - v * StrictMath.sin(angle));
                    int z = (int) (cz + u * StrictMath.sin(angle) + v * StrictMath.cos(angle));
                    total++;
                    if (land == null || land.isLand(x, z)) landCount++;
                }
            }
            double score = landCount / (double) total;
            if (score > bestScore) { bestScore = score; bx = cx; bz = cz; }
            if (score >= 0.85) break;
        }
        long[] rest = new long[r.length - 2];
        System.arraycopy(r, 2, rest, 0, rest.length);
        HornacisRegion region = new HornacisRegion(worldSeed, bx, bz, angle, rest);
        region.buildGrid(HornacisNoise.mix(worldSeed ^ 0x45524F53L));
        region.placeSites(HornacisNoise.mix(worldSeed ^ 0x5349544553L));
        region.buildTallGrid();
        return region;
    }

    // ---------------------------------------------------------------- geometria

    private double spine(double u) {
        double t = u / LENGTH * Math.PI * 2;
        return spineA1 * StrictMath.sin(t * spineF1 + spineP1) + spineA2 * StrictMath.sin(t * spineF2 + spineP2);
    }

    private double toWorldX(double u, double v) {
        return centerX + u * cos - v * sin;
    }

    private double toWorldZ(double u, double v) {
        return centerZ + u * sin + v * cos;
    }

    public boolean inBox(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /** Forma da cadeia num ponto: máscara (onde o terreno vanilla dá lugar à cordilheira) e envelope (0 nas bordas, 1 na espinha). */
    private void shape(double x, double z, Sample out) {
        out.mask = 0;
        out.envelope = 0;
        double dx = x - centerX, dz = z - centerZ;
        double u = dx * cos + dz * sin;
        double v = -dx * sin + dz * cos;
        double wu = u + 150 * warpA.sample(x / 900, z / 900);
        double wv = v + 150 * warpB.sample(x / 900, z / 900);
        double vv = wv - spine(wu);
        double w = HALF_WIDTH * clamp(0.92 + 0.3 * widthNoise.sample(wu / 1600, 3.7), 0.65, 1.25);
        double tu = Math.abs(wu) / (LENGTH / 2);
        out.u = u;
        out.v = v;
        out.vv = vv;
        out.w = w;
        out.tu = tu;
        double a2 = 1 - smooth(0.78, 1.1, tu);
        double q2 = Math.abs(vv) / (w * 1.3);
        out.mask = a2 * (1 - smooth(0.7, 1.0, q2));
        if (out.mask <= 0) return;
        double a = 1 - smooth(0.62, 1.0, tu);
        double q = Math.abs(vv) / w;
        double b = 1 - smooth(0.25, 1.0, q);
        out.envelope = a * b;
    }

    /** Relevo bruto, antes da erosão. */
    private double roughHeight(double x, double z, Sample s) {
        double env = s.envelope;
        if (env <= 0) return BASE;
        // cristas: ruído “ridged” esticado ao longo da cadeia e torcido por um segundo ruído
        double rx = s.u / 1.7 + 260 * ridgeWarpA.sample(x / 1300, z / 1300);
        double rz = s.vv + 260 * ridgeWarpB.sample(x / 1300, z / 1300);
        double r = clamp((ridged(rx, rz) - 0.12) / 0.6, 0, 1);
        double h = BASE + 110 * env + 360 * StrictMath.pow(env, 0.7) * r;

        for (int i = 0; i < peakX.length; i++) {
            double d = Math.hypot(x - peakX[i], z - peakZ[i]);
            if (d < peakR[i]) h += peakH[i] * StrictMath.pow(1 - d / peakR[i], 1.6) * env * (0.55 + 0.45 * r);
        }
        for (int i = 0; i < passU.length; i++) {
            double g = StrictMath.exp(-sq((s.u - passU[i]) / 240));
            h -= (h - 160) * passDepth[i] * g * Math.max(0, 1 - Math.abs(s.vv) / s.w);
        }

        // nenhum outro pico chega perto do principal
        h = softCap(h, 470, 45);

        // o Pico Principal: um chifre de pedra com três arestas, bem acima de tudo
        double mdx = x - mainX, mdz = z - mainZ;
        double dm = Math.sqrt(mdx * mdx + mdz * mdz);
        if (dm < MAIN_RADIUS * 1.3) {
            double phi = StrictMath.atan2(mdz, mdx) - mainAngle;
            double reff = dm * (1 + 0.2 * StrictMath.cos(3 * phi) + 0.05 * StrictMath.cos(7 * phi + 1.3) + 0.06 * gully.sample(x / 80, z / 80));
            double t = reff / MAIN_RADIUS;
            if (t < 1) {
                double hm = 170 + (652 - 170) * StrictMath.pow(1 - t, 1.35);
                double wm = 1 - smooth(0.65, 1.0, t);
                h = smoothMax(h, hm * wm + h * (1 - wm), 30);
            }
        }
        return h;
    }

    private double ridged(double x, double z) {
        double freq = 1.0 / 640, amp = 1, sum = 0, norm = 0, weight = 1;
        for (HornacisNoise n : ridge) {
            double s = 1 - Math.abs(n.sample(x * freq, z * freq));
            s = s * s * weight;
            weight = clamp(0.45 + s * 1.2, 0, 1);
            sum += s * amp;
            norm += amp;
            freq *= 2.05;
            amp *= 0.5;
        }
        return sum / norm;
    }

    // ---------------------------------------------------------------- erosão

    private void buildGrid(long seed) {
        double halfU = LENGTH / 2 * 1.12 + 260;
        double halfV = HALF_WIDTH * 1.25 * 1.3 + spineA1 + spineA2 + 260;
        gridU0 = -halfU;
        gridV0 = -halfV;
        gridU = (int) Math.ceil(2 * halfU / CELL) + 1;
        gridV = (int) Math.ceil(2 * halfV / CELL) + 1;
        float[] h = new float[gridU * gridV];
        Sample s = new Sample();
        for (int j = 0; j < gridV; j++) {
            double v = gridV0 + j * CELL;
            for (int i = 0; i < gridU; i++) {
                double u = gridU0 + i * CELL;
                double x = toWorldX(u, v), z = toWorldZ(u, v);
                shape(x, z, s);
                h[i + j * gridU] = (float) (s.envelope > 0 ? roughHeight(x, z, s) : BASE);
            }
        }
        erode(h, seed);
        grid = h;
    }

    /**
     * Erosão por gotas: cada gota desce pela encosta, arranca material onde corre rápido e deposita onde
     * desacelera. Alturas em unidades de célula (8 blocos) para os parâmetros ficarem na escala certa.
     */
    private void erode(float[] map, long seed) {
        int w = gridU, hgt = gridV;
        for (int i = 0; i < map.length; i++) map[i] /= (float) CELL;
        final int radius = 3;
        // pesos do pincel de erosão
        List<int[]> offsets = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        float sumW = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d < radius) {
                    float wgt = (float) (1 - d / radius);
                    offsets.add(new int[]{dx, dy});
                    weights.add(wgt);
                    sumW += wgt;
                }
            }
        }
        int[] bx = new int[offsets.size()], by = new int[offsets.size()];
        float[] bw = new float[offsets.size()];
        for (int i = 0; i < bw.length; i++) { bx[i] = offsets.get(i)[0]; by[i] = offsets.get(i)[1]; bw[i] = weights.get(i) / sumW; }

        final float inertia = 0.06f, capacityFactor = 6f, minCapacity = 0.01f, erodeSpeed = 0.4f, depositSpeed = 0.12f;
        final float evaporate = 0.015f, gravity = 4f;
        final int maxLife = 70;
        float baseLevel = (float) (BASE / CELL) + 0.5f;
        long rng = seed;
        int droplets = (int) (w * hgt * 2.2);
        float[] grad = new float[3];
        for (int n = 0; n < droplets; n++) {
            rng = HornacisNoise.mix(rng + 0x9E3779B97F4A7C15L);
            float px = (float) (unit(rng) * (w - 3) + 1);
            rng = HornacisNoise.mix(rng + 0x9E3779B97F4A7C15L);
            float py = (float) (unit(rng) * (hgt - 3) + 1);
            if (map[(int) px + (int) py * w] < baseLevel) continue;
            float dirX = 0, dirY = 0, speed = 1, water = 1, sediment = 0;
            for (int life = 0; life < maxLife; life++) {
                int nodeX = (int) px, nodeY = (int) py;
                int idx = nodeX + nodeY * w;
                float ox = px - nodeX, oy = py - nodeY;
                gradient(map, w, px, py, grad);
                dirX = dirX * inertia - grad[0] * (1 - inertia);
                dirY = dirY * inertia - grad[1] * (1 - inertia);
                float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
                if (len < 1e-6f) break;
                dirX /= len;
                dirY /= len;
                px += dirX;
                py += dirY;
                if (px < 1 || py < 1 || px >= w - 2 || py >= hgt - 2) break;
                float oldH = grad[2];
                gradient(map, w, px, py, grad);
                float dh = grad[2] - oldH;
                float capacity = Math.max(-dh * speed * water * capacityFactor, minCapacity);
                if (sediment > capacity || dh > 0) {
                    float dep = dh > 0 ? Math.min(dh, sediment) : (sediment - capacity) * depositSpeed;
                    sediment -= dep;
                    map[idx] += dep * (1 - ox) * (1 - oy);
                    map[idx + 1] += dep * ox * (1 - oy);
                    map[idx + w] += dep * (1 - ox) * oy;
                    map[idx + w + 1] += dep * ox * oy;
                } else {
                    float amount = Math.min((capacity - sediment) * erodeSpeed, -dh);
                    for (int b = 0; b < bw.length; b++) {
                        int ex = nodeX + bx[b], ey = nodeY + by[b];
                        if (ex < 0 || ey < 0 || ex >= w || ey >= hgt) continue;
                        int ei = ex + ey * w;
                        float take = Math.min(amount * bw[b], map[ei] - baseLevel + 0.5f);
                        if (take <= 0) continue;
                        map[ei] -= take;
                        sediment += take;
                    }
                }
                speed = (float) Math.sqrt(Math.max(0, speed * speed - dh * gravity));
                water *= (1 - evaporate);
            }
        }
        for (int i = 0; i < map.length; i++) map[i] *= (float) CELL;
    }

    private static void gradient(float[] map, int w, float px, float py, float[] out) {
        int x = (int) px, y = (int) py;
        float ox = px - x, oy = py - y;
        int i = x + y * w;
        float nw = map[i], ne = map[i + 1], sw = map[i + w], se = map[i + w + 1];
        out[0] = (ne - nw) * (1 - oy) + (se - sw) * oy;
        out[1] = (sw - nw) * (1 - ox) + (se - ne) * ox;
        out[2] = nw * (1 - ox) * (1 - oy) + ne * ox * (1 - oy) + sw * (1 - ox) * oy + se * ox * oy;
    }

    /** Altura da grade erodida (interpolação bicúbica, sem degraus de 8 em 8 blocos). */
    private double gridHeight(double u, double v) {
        double gx = (u - gridU0) / CELL, gy = (v - gridV0) / CELL;
        int ix = (int) Math.floor(gx), iy = (int) Math.floor(gy);
        if (ix < 1 || iy < 1 || ix >= gridU - 2 || iy >= gridV - 2) return BASE;
        double fx = gx - ix, fy = gy - iy;
        double[] col = new double[4];
        for (int j = -1; j <= 2; j++) {
            int row = (iy + j) * gridU + ix;
            col[j + 1] = cubic(grid[row - 1], grid[row], grid[row + 1], grid[row + 2], fx);
        }
        return cubic(col[0], col[1], col[2], col[3], fy);
    }

    private static double cubic(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)));
    }

    /** Fundo de vale? (mais baixo que a vizinhança na grade) */
    private double valley(double u, double v, double h) {
        double avg = 0;
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            avg += gridHeight(u + StrictMath.cos(a) * 40, v + StrictMath.sin(a) * 40);
        }
        return avg / 8 - h;
    }

    // ---------------------------------------------------------------- altura final

    /** Altura, máscara e zona de uma coluna. */
    public void sample(double x, double z, Sample out) {
        sampleRaw(x, z, out);
        if (out.mask <= 0) return;
        double h = out.raw;
        // platôs e terraços onde ficam as estruturas
        if (siteGrid != null) {
            int gx = ((int) Math.floor(x) - siteMinX) / SITE_CELL, gz = ((int) Math.floor(z) - siteMinZ) / SITE_CELL;
            if (x >= siteMinX && z >= siteMinZ && gx < siteW && gz < siteH) {
                Site[] cell = siteGrid[gx + gz * siteW];
                if (cell != null) {
                    for (Site site : cell) {
                        double d = Math.hypot(x - site.x(), z - site.z());
                        if (d >= site.radius()) continue;
                        double f = 1 - smooth(site.radius() * 0.6, site.radius(), d);
                        h += (site.y() - h) * f;
                    }
                }
            }
        }
        out.height = Math.min(h, MAX_TERRAIN);
        out.zone = zone(x, z, out);
    }

    /** Altura sem os platôs das estruturas. */
    public void sampleRaw(double x, double z, Sample out) {
        out.zone = Zone.NONE;
        out.mask = 0;
        if (x < minX || x > maxX || z < minZ || z > maxZ) return;
        shape(x, z, out);
        if (out.mask <= 0) return;
        double h = gridHeight(out.u, out.v);
        // detalhe fino por cima da grade de 8 blocos
        double env = out.envelope;
        h += env * (1.6 * detailA.sample(x / 28, z / 28) + 0.6 * detailB.sample(x / 11, z / 11));

        double mdx = x - mainX, mdz = z - mainZ;
        double dm = Math.sqrt(mdx * mdx + mdz * mdz);
        if (dm < 140) {
            // platô do cume (o palácio) e as agulhas de pedra em volta
            double f = 1 - smooth(54, 92, dm);
            h += (SUMMIT_Y - h) * f;
            for (int i = 0; i < spireX.length; i++) {
                double ds = Math.hypot(x - spireX[i], z - spireZ[i]);
                if (ds < 16) h = Math.max(h, 606 - 46 * StrictMath.pow(ds / 16, 1.25));
            }
        }
        out.mainDist = dm;
        out.raw = h;
        out.height = Math.min(h, MAX_TERRAIN);
        out.valley = 0;
    }

    private Zone zone(double x, double z, Sample s) {
        if (s.mask < 0.45) return Zone.NONE;
        double h = s.height + 18 * dither.sample(x / 90, z / 90);
        if (s.mainDist < 340 || (s.mainDist < 600 && h > 470)) return Zone.MAIN_PEAK;
        if (h > 440) return Zone.PEAKS;
        if (h > 335) {
            if (h < 420 && valley(s.u, s.v, s.height) > 9) return Zone.GLACIER;
            return Zone.SLOPES;
        }
        if (h > 210) return Zone.HIGHLANDS;
        return Zone.FOOTHILLS;
    }

    // ---------------------------------------------------------------- terreno alto (desempenho)

    /** Este pedaço do mundo pode ter terreno acima de 320? (só ali vale calcular o ruído até 640) */
    public boolean isTall(int x0, int z0, int x1, int z1) {
        if (tall == null || x1 < minX || z1 < minZ || x0 > minX + tallW * 64 || z0 > minZ + tallH * 64) return false;
        int cx0 = Math.max(0, (x0 - minX) >> 6), cz0 = Math.max(0, (z0 - minZ) >> 6);
        int cx1 = Math.min(tallW - 1, (x1 - minX) >> 6), cz1 = Math.min(tallH - 1, (z1 - minZ) >> 6);
        for (int i = cx0; i <= cx1; i++)
            for (int j = cz0; j <= cz1; j++)
                if (tall[i + j * tallW]) return true;
        return false;
    }

    private void buildTallGrid() {
        tallW = (maxX - minX) / 64 + 1;
        tallH = (maxZ - minZ) / 64 + 1;
        boolean[] t = new boolean[tallW * tallH];
        Sample s = new Sample();
        for (int i = 0; i < tallW; i++) {
            for (int j = 0; j < tallH; j++) {
                double max = 0;
                for (int a = 0; a <= 4 && max <= 280; a++) {
                    for (int b = 0; b <= 4 && max <= 280; b++) {
                        sample(minX + i * 64 + a * 16, minZ + j * 64 + b * 16, s);
                        if (s.mask > 0) max = Math.max(max, s.height);
                    }
                }
                // folga para o detalhe 3D e para o que fica entre as amostras
                t[i + j * tallW] = max > 280;
            }
        }
        tall = t;
    }

    // ---------------------------------------------------------------- estruturas

    public List<Site> sites() {
        return Collections.unmodifiableList(sites);
    }

    private void placeSites(long seed) {
        long s = seed;
        Sample tmp = new Sample();
        // palácio no platô do cume
        sites.add(new Site(SiteType.PALACE, 0, (int) Math.round(mainX), (int) Math.round(mainZ), SUMMIT_Y, 50, (int) Math.round(Math.toDegrees(mainAngle) / 90) & 3));
        // ruínas no cume: terraços nos ombros do chifre, entre as arestas
        for (int i = 0; i < 3; i++) {
            double ang = mainAngle + i * Math.PI * 2 / 3;
            for (double rr = 150; rr < 300; rr += 10) {
                int x = (int) Math.round(mainX + StrictMath.cos(ang) * rr), z = (int) Math.round(mainZ + StrictMath.sin(ang) * rr);
                sampleRaw(x, z, tmp);
                if (tmp.raw < 520) {
                    sites.add(new Site(SiteType.SUMMIT_RUIN, i, x, z, (int) Math.round(tmp.raw), 30, i & 3));
                    break;
                }
            }
        }
        // ruínas nas florestas, vales e encostas suaves em volta do pico principal
        s = pickSites(s, SiteType.RUIN, 8, 420, 1400, 140, 440, 0.6, 240, 36, tmp);
        // acampamentos de aventureiros espalhados pela cordilheira
        s = pickSitesAnywhere(s, SiteType.CAMP, 22, 100, 470, 0.8, 260, 14, tmp);
        // o mosteiro da Igreja da Noite Eterna, vigiando o caminho para o pico
        pickSites(s, SiteType.MONASTERY, 1, 900, 1800, 170, 340, 0.4, 0, 44, tmp);
        indexSites();
    }

    private long pickSites(long s, SiteType type, int want, double rMin, double rMax, double hMin, double hMax,
                           double maxSlope, double spacing, int radius, Sample tmp) {
        int found = 0;
        for (int attempt = 0; attempt < 5000 && found < want; attempt++) {
            s = HornacisNoise.mix(s + 0x9E3779B97F4A7C15L);
            double ang = unit(s) * Math.PI * 2;
            s = HornacisNoise.mix(s + 0x9E3779B97F4A7C15L);
            double rr = rMin + unit(s) * (rMax - rMin);
            int x = (int) Math.round(mainX + StrictMath.cos(ang) * rr), z = (int) Math.round(mainZ + StrictMath.sin(ang) * rr);
            if (tryAdd(type, found, x, z, hMin, hMax, maxSlope, spacing, radius, tmp, s)) found++;
        }
        return s;
    }

    private long pickSitesAnywhere(long s, SiteType type, int want, double hMin, double hMax, double maxSlope,
                                   double spacing, int radius, Sample tmp) {
        int found = 0;
        for (int attempt = 0; attempt < 8000 && found < want; attempt++) {
            s = HornacisNoise.mix(s + 0x9E3779B97F4A7C15L);
            double u = (unit(s) - 0.5) * LENGTH * 0.9;
            s = HornacisNoise.mix(s + 0x9E3779B97F4A7C15L);
            double v = spine(u) + (unit(s) - 0.5) * HALF_WIDTH * 1.6;
            int x = (int) Math.round(toWorldX(u, v)), z = (int) Math.round(toWorldZ(u, v));
            if (tryAdd(type, found, x, z, hMin, hMax, maxSlope, spacing, radius, tmp, s)) found++;
        }
        return s;
    }

    private boolean tryAdd(SiteType type, int index, int x, int z, double hMin, double hMax, double maxSlope,
                           double spacing, int radius, Sample tmp, long s) {
        sampleRaw(x, z, tmp);
        if (tmp.mask < 0.85 || tmp.raw < hMin || tmp.raw > hMax) return false;
        if (Math.hypot(x - mainX, z - mainZ) < 300) return false;
        double h = tmp.raw;
        double slope = 0;
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            sampleRaw(x + StrictMath.cos(a) * radius * 0.8, z + StrictMath.sin(a) * radius * 0.8, tmp);
            slope = Math.max(slope, Math.abs(tmp.raw - h) / (radius * 0.8));
        }
        if (slope > maxSlope) return false;
        for (Site o : sites) {
            double min = Math.max(spacing, o.radius() + radius + 40);
            if (Math.hypot(o.x() - x, o.z() - z) < min) return false;
        }
        sites.add(new Site(type, index, x, z, (int) Math.round(h), radius, (int) ((s >>> 40) & 3)));
        return true;
    }

    private void indexSites() {
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (Site s : sites) {
            x0 = Math.min(x0, s.x() - s.radius()); z0 = Math.min(z0, s.z() - s.radius());
            x1 = Math.max(x1, s.x() + s.radius()); z1 = Math.max(z1, s.z() + s.radius());
        }
        siteMinX = x0;
        siteMinZ = z0;
        siteW = (x1 - x0) / SITE_CELL + 1;
        siteH = (z1 - z0) / SITE_CELL + 1;
        List<List<Site>> cells = new ArrayList<>();
        for (int i = 0; i < siteW * siteH; i++) cells.add(null);
        for (Site s : sites) {
            for (int gx = (s.x() - s.radius() - x0) / SITE_CELL; gx <= (s.x() + s.radius() - x0) / SITE_CELL; gx++) {
                for (int gz = (s.z() - s.radius() - z0) / SITE_CELL; gz <= (s.z() + s.radius() - z0) / SITE_CELL; gz++) {
                    int i = gx + gz * siteW;
                    if (cells.get(i) == null) cells.set(i, new ArrayList<>());
                    cells.get(i).add(s);
                }
            }
        }
        Site[][] g = new Site[siteW * siteH][];
        for (int i = 0; i < g.length; i++) if (cells.get(i) != null) g[i] = cells.get(i).toArray(new Site[0]);
        siteGrid = g;
    }

    // ---------------------------------------------------------------- utilidades

    private static double unit(long v) {
        return (v >>> 11) * 0x1.0p-53;
    }

    static double smooth(double e0, double e1, double x) {
        double t = clamp((x - e0) / (e1 - e0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Achata suavemente o que passa de um teto (os picos comuns não competem com o principal) */
    private static double softCap(double h, double cap, double k) {
        if (h <= cap - k) return h;
        double over = h - (cap - k);
        return cap - k + k * (1 - StrictMath.exp(-over / k));
    }

    private static double smoothMax(double a, double b, double k) {
        double h = clamp(0.5 + 0.5 * (b - a) / k, 0, 1);
        return a + (b - a) * h + k * h * (1 - h);
    }
}
