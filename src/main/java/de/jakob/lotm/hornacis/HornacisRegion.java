package de.jakob.lotm.hornacis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Cordilheira de Hornacis de um mundo: onde fica, a forma, a altura de cada coluna e os lugares das
 * estruturas. Tudo sai da seed do mundo, sem nada do Minecraft, então o mesmo código desenha o terreno no
 * jogo e as prévias fora dele.
 *
 * Na novel: a cordilheira separa Loen de Intis, o pico principal passa de 6000 m e é o mais alto;
 * no cume ficava a Nação da Noite Eterna, com relíquias também nas florestas, vales e encostas suaves em volta.
 *
 * O relevo é montado em etapas: uma forma bruta (envelope da cadeia, cristas, picos e o Pico Principal),
 * uma simulação de erosão pela água numa grade de 24 blocos, que cava os vales que se ramificam como rios,
 * e por fim os poços que sobram são aterrados (a água sempre tem por onde escorrer, nada fica oco).
 * Só StrictMath e raiz quadrada: o resultado é o mesmo em qualquer máquina.
 */
public final class HornacisRegion {
    public static final double LENGTH = 20000;
    public static final double HALF_WIDTH = 2600;
    /** Teto do terreno (o mundo vai até 1727) */
    public static final int MAX_TERRAIN = 1680;
    /** Altura do platô do cume, onde fica o palácio */
    public static final int SUMMIT_Y = 1600;
    public static final double MAIN_RADIUS = 2600;
    private static final double BASE = 70;

    public enum SiteType { PALACE, SUMMIT_RUIN, RUIN, CAMP, MONASTERY }

    public record Site(SiteType type, int index, int x, int z, int y, int radius, int rotation) {}

    /** Diz se um ponto do mundo vanilla é terra firme (para não pôr a cordilheira no meio do mar). */
    public interface LandTest {
        boolean isLand(int x, int z);
    }

    /** Resultado de uma coluna (reaproveitado para não alocar) */
    public static final class Sample {
        public double mask, height, envelope, mainDist, raw, slope;
        double u, v, vv, w, tu, floor;
    }

    public final long seed;
    public final double centerX, centerZ, cos, sin;
    public final double mainX, mainZ, mainAngle;
    public final int minX, minZ, maxX, maxZ;

    private final double spineA1, spineF1, spineP1, spineA2, spineF2, spineP2;
    private final HornacisNoise warpA, warpB, widthNoise, gully, detailA, detailB, ridgeWarpA, ridgeWarpB;
    private final HornacisNoise[] ridge = new HornacisNoise[6];
    private final double[] peakX, peakZ, peakR, peakH;
    private final double[] passU, passDepth;
    private final double[] spireX, spireZ;
    private final double mainU;

    /** Grade já erodida, em coordenadas da cordilheira (u ao longo, v através), uma altura a cada 24 blocos */
    private static final double CELL = 24;
    private float[] grid;
    private int gridU, gridV;
    private double gridU0, gridV0;

    private final List<Site> sites = new ArrayList<>();
    private Site[][] siteGrid;
    private int siteMinX, siteMinZ, siteW, siteH;
    private static final int SITE_CELL = 128;

    /** Altura máxima do terreno em cada célula de 64 blocos (só até ali o ruído é calculado) */
    private short[] top;
    private int tallW, tallH;

    private HornacisRegion(long seed, double cx, double cz, double angle, long[] r) {
        this.seed = seed;
        this.centerX = cx;
        this.centerZ = cz;
        this.cos = StrictMath.cos(angle);
        this.sin = StrictMath.sin(angle);
        int k = 0;
        spineA1 = 960 + unit(r[k++]) * 560;
        spineF1 = 0.55 + unit(r[k++]) * 0.35;
        spineP1 = unit(r[k++]) * Math.PI * 2;
        spineA2 = 240 + unit(r[k++]) * 240;
        spineF2 = 1.6 + unit(r[k++]) * 0.8;
        spineP2 = unit(r[k++]) * Math.PI * 2;
        warpA = new HornacisNoise(r[k++]);
        warpB = new HornacisNoise(r[k++]);
        widthNoise = new HornacisNoise(r[k++]);
        gully = new HornacisNoise(r[k++]);
        k++; // (era o ruído das zonas)
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
        int count = 40;
        for (int i = 0; i < count; i++) {
            double pu = -LENGTH * 0.44 + i * (LENGTH * 0.88 / (count - 1)) + (unit(r[k++]) - 0.5) * 200;
            double pv = spine(pu) + (unit(r[k++]) - 0.5) * HALF_WIDTH * 0.6;
            double rad = 600 + unit(r[k++]) * 500;
            double h = 150 + unit(r[k++]) * 200;
            if (Math.abs(pu - mainU) < MAIN_RADIUS * 0.7) continue;
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
        double hl = LENGTH / 2 * 1.12 + 700;
        double hw = HALF_WIDTH * 1.25 * 1.3 + spineA1 + spineA2 + 700;
        double ex = Math.abs(cos) * hl + Math.abs(sin) * hw;
        double ez = Math.abs(sin) * hl + Math.abs(cos) * hw;
        minX = (int) Math.floor(cx - ex);
        maxX = (int) Math.ceil(cx + ex);
        minZ = (int) Math.floor(cz - ez);
        maxZ = (int) Math.ceil(cz + ez);
    }

    /**
     * Guarda, na pasta do mundo, o lugar escolhido para a cordilheira (para sempre) e a grade erodida
     * (calcular leva alguns segundos).
     */
    public interface GridStore {
        /** Centro (x, z) escolhido na primeira vez que este mundo abriu com a cordilheira, ou null. */
        double[] loadCenter(long seed);

        void saveCenter(long seed, double x, double z);

        float[] load(long key, int size);

        void save(long key, float[] grid);
    }

    /** Muda quando o relevo muda: a grade guardada de antes deixa de valer. */
    public static final int VERSION = 2;

    public long gridKey() {
        long k = HornacisNoise.mix(seed ^ VERSION * 0x9E3779B97F4A7C15L);
        k = HornacisNoise.mix(k ^ Double.doubleToLongBits(centerX));
        return HornacisNoise.mix(k ^ Double.doubleToLongBits(centerZ));
    }

    public static HornacisRegion create(long worldSeed, LandTest land) {
        return create(worldSeed, land, null, List.of());
    }

    /**
     * Monta a cordilheira de um mundo. Na primeira vez a posição é sorteada a até ~8000 blocos do spawn, de
     * preferência em terra firme e sempre longe do que o mundo já tinha gerado antes ({@code explored}: os
     * arquivos de região, de 512 x 512 blocos, que já existem), para não cortar o que já foi construído ou
     * explorado. Depois a posição fica guardada e nunca muda.
     */
    public static HornacisRegion create(long worldSeed, LandTest land, GridStore store, List<int[]> explored) {
        long s = HornacisNoise.mix(worldSeed ^ 0x484F524E41434953L);
        long[] r = new long[200];
        for (int i = 0; i < r.length; i++) {
            s = HornacisNoise.mix(s + 0x9E3779B97F4A7C15L);
            r[i] = s;
        }
        double angle = unit(r[0]) * Math.PI;
        double[] center = store == null ? null : store.loadCenter(worldSeed);
        if (center == null) {
            center = pickCenter(r[1], angle, land, explored);
            if (store != null) store.saveCenter(worldSeed, center[0], center[1]);
        }
        double bx = center[0], bz = center[1];
        long[] rest = new long[r.length - 2];
        System.arraycopy(r, 2, rest, 0, rest.length);
        HornacisRegion region = new HornacisRegion(worldSeed, bx, bz, angle, rest);
        region.buildGrid(HornacisNoise.mix(worldSeed ^ 0x45524F53L), store);
        region.placeSites(HornacisNoise.mix(worldSeed ^ 0x5349544553L));
        region.buildTopGrid();
        return region;
    }

    private static double[] pickCenter(long pick, double angle, LandTest land, List<int[]> explored) {
        double ca = StrictMath.cos(angle), sa = StrictMath.sin(angle);
        double bestScore = -1, bx = 0, bz = 0;
        // anéis cada vez mais longe: o primeiro (até 8000 blocos) quase sempre serve num mundo novo
        for (int ring = 0; ring < 40 && bestScore < 0; ring++) {
            double maxDist = 8000 + ring * 6000.0;
            for (int attempt = 0; attempt < 48; attempt++) {
                pick = HornacisNoise.mix(pick + 0x9E3779B97F4A7C15L);
                double dist = maxDist * Math.sqrt(unit(pick));
                pick = HornacisNoise.mix(pick + 0x9E3779B97F4A7C15L);
                double a = unit(pick) * Math.PI * 2;
                double cx = StrictMath.cos(a) * dist, cz = StrictMath.sin(a) * dist;
                if (overlaps(cx, cz, ca, sa, explored)) continue;
                int landCount = 0, total = 0;
                for (int i = -5; i <= 5; i++) {
                    for (int j = -1; j <= 1; j++) {
                        double u = i * LENGTH * 0.09, v = j * 1500;
                        int x = (int) (cx + u * ca - v * sa);
                        int z = (int) (cz + u * sa + v * ca);
                        total++;
                        if (land == null || land.isLand(x, z)) landCount++;
                    }
                }
                double score = landCount / (double) total;
                if (score > bestScore) { bestScore = score; bx = cx; bz = cz; }
                if (score >= 0.85) break;
            }
        }
        return new double[]{bx, bz};
    }

    /** A cordilheira com centro em (cx, cz) passaria por algum pedaço do mundo que já foi gerado? */
    private static boolean overlaps(double cx, double cz, double ca, double sa, List<int[]> explored) {
        // retângulo (girado) que contém a cadeia inteira: comprimento, curvas da espinha, largura, torção e folga
        double hu = LENGTH / 2 * 1.12 + 500 + 1200 + 362;
        double hv = 2000 + HALF_WIDTH * 1.25 * 1.3 + 500 + 1200 + 362;
        for (int[] t : explored) {
            double dx = t[0] * 512.0 + 256 - cx, dz = t[1] * 512.0 + 256 - cz;
            double u = dx * ca + dz * sa, v = -dx * sa + dz * ca;
            if (Math.abs(u) < hu && Math.abs(v) < hv) return true;
        }
        return false;
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
        double wu = u + 500 * warpA.sample(x / 3000, z / 3000);
        double wv = v + 500 * warpB.sample(x / 3000, z / 3000);
        double vv = wv - spine(wu);
        double w = HALF_WIDTH * clamp(0.92 + 0.3 * widthNoise.sample(wu / 6000, 3.7), 0.65, 1.25);
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
        double rx = s.u / 1.7 + 900 * ridgeWarpA.sample(x / 4500, z / 4500);
        double rz = s.vv + 900 * ridgeWarpB.sample(x / 4500, z / 4500);
        double r = clamp((ridged(rx, rz) - 0.12) / 0.6, 0, 1);
        double h = BASE + 280 * env + 880 * StrictMath.pow(env, 0.7) * r;

        for (int i = 0; i < peakX.length; i++) {
            double d = dist(x - peakX[i], z - peakZ[i]);
            if (d < peakR[i]) h += peakH[i] * StrictMath.pow(1 - d / peakR[i], 1.6) * env * (0.55 + 0.45 * r);
        }
        // as passagens só baixam as cristas (nunca sobem um vale)
        for (int i = 0; i < passU.length; i++) {
            double g = StrictMath.exp(-sq((s.u - passU[i]) / 800));
            h -= Math.max(0, h - 600) * passDepth[i] * g * Math.max(0, 1 - Math.abs(s.vv) / s.w);
        }

        // nenhum outro pico chega perto do principal
        h = softCap(h, 1260, 130);

        // o Pico Principal: um chifre de pedra com três arestas, bem acima de tudo
        double mdx = x - mainX, mdz = z - mainZ;
        double dm = Math.sqrt(mdx * mdx + mdz * mdz);
        if (dm < MAIN_RADIUS * 1.3) {
            double phi = StrictMath.atan2(mdz, mdx) - mainAngle;
            double reff = dm * (1 + 0.2 * StrictMath.cos(3 * phi) + 0.05 * StrictMath.cos(7 * phi + 1.3) + 0.06 * gully.sample(x / 250, z / 250));
            double t = reff / MAIN_RADIUS;
            // a base do chifre se apaga junto com a cadeia (senão ela sairia pela borda estreita da
            // cordilheira e seria cortada num paredão); a suavização também some na borda do chifre
            double wm = (1 - smooth(0.65, 1.0, t)) * smooth(0.0, 0.3, env);
            if (wm > 0) h = smoothMax(h, horn(reff) * wm + h * (1 - wm), 80 * wm);
        }
        return h;
    }

    /**
     * Perfil do Pico Principal (distância do centro → altura): no alto as faces são íngremes como as de um
     * chifre (~45°), embaixo o maciço se abre devagar. O platô do palácio fica bem no topo.
     */
    private static final double[] HORN_D = {0, 92, 150, 250, 400, 600, 900, 1300, 2000, 2600};
    private static final double[] HORN_H = {1606, 1600, 1545, 1455, 1330, 1190, 1030, 860, 610, 450};
    private static final double[] HORN_M = monotoneSlopes(HORN_D, HORN_H);

    private static double horn(double d) {
        int n = HORN_D.length;
        if (d >= HORN_D[n - 1]) return HORN_H[n - 1];
        int k = 0;
        while (d > HORN_D[k + 1]) k++;
        double len = HORN_D[k + 1] - HORN_D[k], t = (d - HORN_D[k]) / len;
        double t2 = t * t, t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * HORN_H[k] + (t3 - 2 * t2 + t) * len * HORN_M[k]
                + (-2 * t3 + 3 * t2) * HORN_H[k + 1] + (t3 - t2) * len * HORN_M[k + 1];
    }

    /** Inclinações (Fritsch–Carlson) para a curva passar pelos pontos sem ondular entre eles. */
    private static double[] monotoneSlopes(double[] x, double[] y) {
        int n = x.length;
        double[] d = new double[n - 1], m = new double[n];
        for (int i = 0; i < n - 1; i++) d[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
        m[0] = d[0];
        m[n - 1] = d[n - 2];
        for (int i = 1; i < n - 1; i++) {
            if (d[i - 1] * d[i] <= 0) continue;
            double a = x[i] - x[i - 1], b = x[i + 1] - x[i];
            double w1 = 2 * b + a, w2 = b + 2 * a;
            m[i] = (w1 + w2) / (w1 / d[i - 1] + w2 / d[i]);
        }
        return m;
    }

    private double ridged(double x, double z) {
        double freq = 1.0 / 2200, amp = 1, sum = 0, norm = 0, weight = 1;
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

    private void buildGrid(long seed, GridStore store) {
        double halfU = LENGTH / 2 * 1.12 + 800;
        double halfV = HALF_WIDTH * 1.25 * 1.3 + spineA1 + spineA2 + 800;
        gridU0 = -halfU;
        gridV0 = -halfV;
        gridU = (int) Math.ceil(2 * halfU / CELL) + 1;
        gridV = (int) Math.ceil(2 * halfV / CELL) + 1;
        if (store != null) {
            float[] saved = store.load(gridKey(), gridU * gridV);
            if (saved != null && saved.length == gridU * gridV) {
                grid = saved;
                return;
            }
        }
        float[] h = new float[gridU * gridV];
        // o relevo bruto de cada célula não depende das outras: pode ser feito em paralelo
        java.util.stream.IntStream.range(0, gridV).parallel().forEach(j -> {
            Sample s = new Sample();
            double v = gridV0 + j * CELL;
            for (int i = 0; i < gridU; i++) {
                double u = gridU0 + i * CELL;
                double x = toWorldX(u, v), z = toWorldZ(u, v);
                shape(x, z, s);
                h[i + j * gridU] = (float) (s.envelope > 0 ? roughHeight(x, z, s) : BASE);
            }
        });
        // a erosão é em sequência (cada gota muda o terreno da próxima): sempre o mesmo resultado
        float[] rough = h.clone();
        erode(h, seed);
        // a erosão só abre os vales: fica metade do que ela cavou, e quase nada no Pico Principal
        // (senão ela derrete as montanhas e o cume vira uma torre fina)
        for (int j = 0; j < gridV; j++) {
            double v = gridV0 + j * CELL;
            for (int i = 0; i < gridU; i++) {
                double u = gridU0 + i * CELL;
                double d = dist(toWorldX(u, v) - mainX, toWorldZ(u, v) - mainZ);
                double keep = 0.08 + 0.42 * smooth(250, 1400, d);
                int k = i + j * gridU;
                h[k] = (float) (rough[k] - (rough[k] - h[k]) * keep);
            }
        }
        fillPits(h);
        grid = h;
        if (store != null) store.save(gridKey(), h);
    }

    /**
     * Aterra os poços (depressões fechadas) que a erosão deixa: a "água" entra pela borda da grade e sobe
     * sempre pelo ponto mais baixo que ainda falta (priority-flood). Onde ela teria de empoçar, o terreno sobe
     * até a borda do poço, com um declive mínimo para tudo escorrer. Ordem total (altura, índice): o mesmo
     * resultado sempre.
     */
    private void fillPits(float[] h) {
        final float slope = 0.05f;
        int w = gridU, n = gridU * gridV;
        boolean[] done = new boolean[n];
        LongHeap heap = new LongHeap(4 * (gridU + gridV));
        for (int i = 0; i < w; i++) {
            seed(h, done, heap, i);
            seed(h, done, heap, i + (gridV - 1) * w);
        }
        for (int j = 1; j < gridV - 1; j++) {
            seed(h, done, heap, j * w);
            seed(h, done, heap, w - 1 + j * w);
        }
        // fora das montanhas (na altura da base) também é saída: a planície em volta fica como está
        for (int k = 0; k < n; k++) if (h[k] <= BASE + 0.5f) seed(h, done, heap, k);
        while (!heap.isEmpty()) {
            int c = (int) heap.pop();
            float hc = h[c];
            int cx = c % w, cy = c / w;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = cy + dy;
                if (ny < 0 || ny >= gridV) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = cx + dx;
                    if ((dx == 0 && dy == 0) || nx < 0 || nx >= w) continue;
                    int k = nx + ny * w;
                    if (done[k]) continue;
                    done[k] = true;
                    if (h[k] <= hc) h[k] = hc + slope;
                    heap.push(key(h[k], k));
                }
            }
        }
    }

    private static void seed(float[] h, boolean[] done, LongHeap heap, int k) {
        if (done[k]) return;
        done[k] = true;
        heap.push(key(h[k], k));
    }

    /** Altura (positiva) nos 32 bits de cima, índice nos de baixo: ordena por altura e desempata pelo índice. */
    private static long key(float height, int index) {
        return ((long) Float.floatToIntBits(Math.max(0, height)) << 32) | index;
    }

    /** Fila de prioridade mínima de longs (sem objetos: a grade tem centenas de milhares de pontos). */
    private static final class LongHeap {
        private long[] a;
        private int size;

        LongHeap(int capacity) {
            a = new long[Math.max(16, capacity)];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void push(long v) {
            if (size == a.length) a = java.util.Arrays.copyOf(a, size * 2);
            int i = size++;
            while (i > 0) {
                int p = (i - 1) >> 1;
                if (a[p] <= v) break;
                a[i] = a[p];
                i = p;
            }
            a[i] = v;
        }

        long pop() {
            long top = a[0], last = a[--size];
            int i = 0;
            while (true) {
                int l = 2 * i + 1;
                if (l >= size) break;
                int m = l + 1 < size && a[l + 1] < a[l] ? l + 1 : l;
                if (a[m] >= last) break;
                a[i] = a[m];
                i = m;
            }
            a[i] = last;
            return top;
        }
    }

    /**
     * Erosão por gotas: cada gota desce pela encosta, arranca material onde corre rápido e deposita onde
     * desacelera. Alturas em unidades de célula (24 blocos) para os parâmetros ficarem na escala certa.
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

    /**
     * Altura da grade erodida (interpolação bicúbica, sem degraus de 24 em 24 blocos). Também guarda em
     * {@code out} a inclinação ali (blocos de altura por bloco andado) e o "chão": a interpolação linear dos
     * 4 nós em volta, que nunca forma poços porque a grade não tem.
     */
    private double gridSample(double u, double v, Sample out) {
        double gx = (u - gridU0) / CELL, gy = (v - gridV0) / CELL;
        int ix = (int) Math.floor(gx), iy = (int) Math.floor(gy);
        if (ix < 1 || iy < 1 || ix >= gridU - 2 || iy >= gridV - 2) {
            out.slope = 0;
            out.floor = BASE;
            return BASE;
        }
        double fx = gx - ix, fy = gy - iy;
        int r0 = (iy - 1) * gridU + ix, r1 = r0 + gridU, r2 = r1 + gridU, r3 = r2 + gridU;
        double a0 = cubic(grid, r0, fx), a1 = cubic(grid, r1, fx), a2 = cubic(grid, r2, fx), a3 = cubic(grid, r3, fx);
        double s0 = cubicSlope(grid, r0, fx), s1 = cubicSlope(grid, r1, fx), s2 = cubicSlope(grid, r2, fx), s3 = cubicSlope(grid, r3, fx);
        double du = cubic(s0, s1, s2, s3, fy) / CELL, dv = cubicSlope(a0, a1, a2, a3, fy) / CELL;
        out.slope = Math.sqrt(du * du + dv * dv);
        float c00 = grid[r1], c10 = grid[r1 + 1], c01 = grid[r2], c11 = grid[r2 + 1];
        out.floor = (c00 * (1 - fx) + c10 * fx) * (1 - fy) + (c01 * (1 - fx) + c11 * fx) * fy;
        return cubic(a0, a1, a2, a3, fy);
    }

    private static double cubic(float[] g, int i, double t) {
        return cubic(g[i - 1], g[i], g[i + 1], g[i + 2], t);
    }

    private static double cubicSlope(float[] g, int i, double t) {
        return cubicSlope(g[i - 1], g[i], g[i + 1], g[i + 2], t);
    }

    /** Derivada de {@link #cubic} em t. */
    private static double cubicSlope(double p0, double p1, double p2, double p3, double t) {
        return 0.5 * (p2 - p0 + t * (2 * (2 * p0 - 5 * p1 + 4 * p2 - p3) + 3 * t * (3 * (p1 - p2) + p3 - p0)));
    }

    private static double cubic(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)));
    }

    // ---------------------------------------------------------------- altura final

    /** Altura e máscara de uma coluna. */
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
                        double d = dist(x - site.x(), z - site.z());
                        if (d >= site.radius()) continue;
                        double f = 1 - smooth(site.radius() * 0.6, site.radius(), d);
                        h += (site.y() - h) * f;
                    }
                }
            }
        }
        out.height = Math.min(h, MAX_TERRAIN);
    }

    /** Máscara da cordilheira num ponto (0 fora, 1 no meio): só a forma, sem calcular a altura. */
    public double mask(double x, double z, Sample tmp) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) return 0;
        shape(x, z, tmp);
        return tmp.mask;
    }

    /** Quanto o terreno vanilla é substituído (1 no miolo da cadeia); na borda as montanhas só somam ao vanilla. */
    public static double core(Sample s) {
        return smooth(0.06, 0.35, s.envelope);
    }

    /** Altura sem os platôs das estruturas. */
    public void sampleRaw(double x, double z, Sample out) {
        out.mask = 0;
        if (x < minX || x > maxX || z < minZ || z > maxZ) return;
        shape(x, z, out);
        if (out.mask <= 0) return;
        double h = gridSample(out.u, out.v, out);
        // detalhe fino por cima da grade de 24 blocos: pedras e saliências nas encostas, quase liso no plano
        // (no plano o detalhe faria pocinhos)
        double amp = out.envelope * (0.15 + 0.85 * smooth(0.15, 0.7, out.slope));
        h += amp * (6 * detailA.sample(x / 60, z / 60) + 2.5 * detailB.sample(x / 22, z / 22) + 0.8 * detailA.sample(x / 9, z / 9));
        // nunca mais de 1,5 bloco abaixo do "chão" da grade: a superfície não fica com buracos
        h = Math.max(h, out.floor - 1.5);

        double mdx = x - mainX, mdz = z - mainZ;
        double dm = Math.sqrt(mdx * mdx + mdz * mdz);
        if (dm < 140) {
            // platô do cume (o palácio) e as agulhas de pedra em volta
            double f = 1 - smooth(54, 92, dm);
            h += (SUMMIT_Y - h) * f;
            // (na borda de cada agulha o perfil já está abaixo do platô: sem degrau em volta)
            for (int i = 0; i < spireX.length; i++) {
                double ds = dist(x - spireX[i], z - spireZ[i]);
                if (ds < 16) h = Math.max(h, 1650 - 70 * StrictMath.pow(ds / 16, 1.25));
            }
        }
        out.mainDist = dm;
        out.raw = h;
        out.height = Math.min(h, MAX_TERRAIN);
    }

    // ---------------------------------------------------------------- terreno alto (desempenho)

    /**
     * Altura máxima do terreno da cordilheira num pedaço do mundo (0 se ela não passa ali).
     * O ruído do terreno só precisa ser calculado até esta altura: o resto é ar.
     */
    public int topIn(int x0, int z0, int x1, int z1) {
        if (top == null || x1 < minX || z1 < minZ || x0 > minX + tallW * 64 || z0 > minZ + tallH * 64) return 0;
        int cx0 = Math.max(0, (x0 - minX) >> 6), cz0 = Math.max(0, (z0 - minZ) >> 6);
        int cx1 = Math.min(tallW - 1, (x1 - minX) >> 6), cz1 = Math.min(tallH - 1, (z1 - minZ) >> 6);
        int max = 0;
        for (int i = cx0; i <= cx1; i++)
            for (int j = cz0; j <= cz1; j++)
                max = Math.max(max, top[i + j * tallW]);
        return max;
    }

    /**
     * Teto de cada célula de 64 blocos, tirado direto dos nós da grade: cada nó vale para a própria célula e
     * as vizinhas (a interpolação usa nós a até 48 blocos). A folga cobre o detalhe fino (até ~10 blocos) e
     * o quanto a bicúbica passa dos nós nas encostas íngremes; o platô do cume, as agulhas e os terraços das
     * estruturas entram à parte.
     */
    private void buildTopGrid() {
        tallW = (maxX - minX) / 64 + 1;
        tallH = (maxZ - minZ) / 64 + 1;
        float[] t = new float[tallW * tallH];
        for (int j = 0; j < gridV; j++) {
            double v = gridV0 + j * CELL;
            for (int i = 0; i < gridU; i++) {
                float hv = grid[i + j * gridU];
                if (hv <= BASE + 1) continue;
                double u = gridU0 + i * CELL;
                raise(t, toWorldX(u, v), toWorldZ(u, v), 1, hv);
            }
        }
        raise(t, mainX, mainZ, 3, 1660);
        for (Site site : sites) raise(t, site.x(), site.z(), 1, site.y());
        short[] out = new short[t.length];
        for (int k = 0; k < t.length; k++) out[k] = (short) (t[k] > 0 ? Math.min(MAX_TERRAIN, Math.ceil(t[k]) + 40) : 0);
        top = out;
    }

    private void raise(float[] t, double x, double z, int spread, float height) {
        int cx = ((int) Math.floor(x) - minX) >> 6, cz = ((int) Math.floor(z) - minZ) >> 6;
        for (int a = -spread; a <= spread; a++) {
            for (int b = -spread; b <= spread; b++) {
                int i = cx + a, j = cz + b;
                if (i < 0 || j < 0 || i >= tallW || j >= tallH) continue;
                t[i + j * tallW] = Math.max(t[i + j * tallW], height);
            }
        }
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
        // ruínas no cume: nos ombros do chifre, entre as arestas, no trecho mais plano de cada lado (numa encosta
        // íngreme o terraço viraria um buraco cavado na montanha); se nenhum trecho for plano o bastante, não há ruína
        for (int i = 0; i < 3; i++) {
            double ang = mainAngle + i * Math.PI * 2 / 3;
            double best = Double.MAX_VALUE;
            int bx = 0, bz = 0, by = 0;
            for (double rr = 150; rr <= 900; rr += 10) {
                for (int k = -4; k <= 4; k++) {
                    double a = ang + k * 0.12;
                    int x = (int) Math.round(mainX + StrictMath.cos(a) * rr), z = (int) Math.round(mainZ + StrictMath.sin(a) * rr);
                    sampleRaw(x, z, tmp);
                    if (tmp.mask < 0.85 || tmp.raw > 1500 || tmp.raw < 1150) continue;
                    double h = tmp.raw, slope = 0;
                    for (int q = 0; q < 8 && slope < best; q++) {
                        double qa = q * Math.PI / 4;
                        sampleRaw(x + StrictMath.cos(qa) * 26, z + StrictMath.sin(qa) * 26, tmp);
                        slope = Math.max(slope, Math.abs(tmp.raw - h) / 26);
                    }
                    if (slope < best) {
                        best = slope;
                        bx = x;
                        bz = z;
                        by = (int) Math.round(h);
                    }
                }
            }
            if (best < 0.5) sites.add(new Site(SiteType.SUMMIT_RUIN, i, bx, bz, by, 30, i & 3));
        }
        // ruínas nas florestas, vales e encostas suaves em volta do pico principal
        s = pickSites(s, SiteType.RUIN, 10, 1500, 5000, 300, 1150, 0.42, 700, 36, tmp);
        // acampamentos de aventureiros espalhados pela cordilheira
        s = pickSitesAnywhere(s, SiteType.CAMP, 35, 250, 1200, 0.8, 700, 14, tmp);
        // o mosteiro da Igreja da Noite Eterna, vigiando o caminho para o pico
        pickSites(s, SiteType.MONASTERY, 1, 3000, 6000, 500, 900, 0.4, 0, 44, tmp);
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
        if (dist(x - mainX, z - mainZ) < 900) return false;
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
            if (dist(o.x() - x, o.z() - z) < min) return false;
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

    /** Distância (raiz quadrada é exata em qualquer máquina, Math.hypot não garante) */
    private static double dist(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
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
