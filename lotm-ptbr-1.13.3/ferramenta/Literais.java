import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Acha textos fixos que vão para a tela e, no modo "aplicar", troca só esses.
 *
 *  scan    <dirClasses> <saida.tsv>
 *  aplicar <dirClasses> <mapa.tsv> <dirSaida>
 *
 * Um texto (LDC String ou concatenação makeConcatWithConstants) é "de tela" quando o valor dele
 * chega a um consumidor de tela: Component.literal, GuiGraphics.drawString etc., ou um método
 * do mod que repassa aquele parâmetro para a tela, ou um campo String que é desenhado.
 * O valor é seguido por variáveis locais, CHECKCAST, campos e métodos que devolvem um parâmetro.
 */
public class Literais {
    static final Set<String> CONSUMIDORES = Set.of(
            "net/minecraft/network/chat/Component.literal",
            "net/minecraft/network/chat/Component.nullToEmpty",
            "net/minecraft/client/gui/GuiGraphics.drawString",
            "net/minecraft/client/gui/GuiGraphics.drawCenteredString",
            "net/minecraft/client/gui/Font.width",
            "net/minecraft/client/gui/Font.plainSubstrByWidth",
            "net/minecraft/client/gui/components/EditBox.setSuggestion",
            "net/minecraft/network/chat/MutableComponent.append"
    );
    static final String STR = "Ljava/lang/String;";

    static final Map<String, Set<Integer>> SINKS = new HashMap<>();     // metodo -> params que vão para a tela
    static final Map<String, Set<Integer>> PASSA = new HashMap<>();     // metodo -> params que podem ser devolvidos
    static final Set<String> CAMPOS = new HashSet<>();                  // campos String desenhados
    static boolean mudou;

    static String esc(String s) { return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n"); }
    static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) { char n = s.charAt(++i); b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n); }
            else b.append(c);
        }
        return b.toString();
    }
    static boolean ehConcat(AbstractInsnNode in) { return in instanceof InvokeDynamicInsnNode d && d.name.equals("makeConcatWithConstants"); }
    static boolean add(Map<String, Set<Integer>> m, String k, int v) {
        boolean r = m.computeIfAbsent(k, x -> new HashSet<>()).add(v);
        if (r) mudou = true;
        return r;
    }

    static final class Ctx {
        final ClassNode cn; final MethodNode mn; final Frame<SourceValue>[] f; final InsnList il;
        final Map<Integer, Integer> slotParam = new HashMap<>();
        Ctx(ClassNode cn, MethodNode mn, Frame<SourceValue>[] f) {
            this.cn = cn; this.mn = mn; this.f = f; this.il = mn.instructions;
            Type[] ps = Type.getArgumentTypes(mn.desc);
            int slot = (mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
            for (int i = 0; i < ps.length; i++) { slotParam.put(slot, i); slot += ps[i].getSize(); }
        }
        String chave() { return cn.name + "." + mn.name + mn.desc; }
        Frame<SourceValue> at(AbstractInsnNode in) { return f[il.indexOf(in)]; }
    }

    /** Segue a origem de um valor até textos fixos (coletados em 'textos'), parâmetros ou campos. */
    static void resolver(Ctx c, AbstractInsnNode src, Set<AbstractInsnNode> textos, Set<AbstractInsnNode> vistos) {
        if (!vistos.add(src)) return;
        if ((src instanceof LdcInsnNode l && l.cst instanceof String) || ehConcat(src)) { textos.add(src); return; }
        if (src instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD) {
            Frame<SourceValue> fr = c.at(src); if (fr == null) return;
            SourceValue local = fr.getLocal(v.var);
            if (local.insns.isEmpty()) {           // valor inicial = parâmetro
                Integer p = c.slotParam.get(v.var);
                if (p != null) add(SINKS, c.chave(), p);
                return;
            }
            for (AbstractInsnNode st : local.insns) {
                if (st instanceof VarInsnNode s && s.getOpcode() == Opcodes.ASTORE) {
                    Frame<SourceValue> fs = c.at(st); if (fs == null) continue;
                    for (AbstractInsnNode o : fs.getStack(fs.getStackSize() - 1).insns) resolver(c, o, textos, vistos);
                }
            }
            return;
        }
        if (src.getOpcode() == Opcodes.CHECKCAST || src.getOpcode() == Opcodes.DUP) {
            Frame<SourceValue> fr = c.at(src); if (fr == null) return;
            for (AbstractInsnNode o : fr.getStack(fr.getStackSize() - 1).insns) resolver(c, o, textos, vistos);
            return;
        }
        if (src instanceof FieldInsnNode fi && (fi.getOpcode() == Opcodes.GETFIELD || fi.getOpcode() == Opcodes.GETSTATIC) && fi.desc.equals(STR)) {
            if (CAMPOS.add(fi.owner + "." + fi.name)) mudou = true;
            return;
        }
        if (src instanceof MethodInsnNode m) {
            Set<Integer> ps = PASSA.get(m.owner + "." + m.name + m.desc);
            if (ps == null) return;
            Frame<SourceValue> fr = c.at(src); if (fr == null) return;
            Type[] args = Type.getArgumentTypes(m.desc);
            int base = fr.getStackSize() - args.length;
            for (int p : ps) for (AbstractInsnNode o : fr.getStack(base + p).insns) resolver(c, o, textos, vistos);
        }
    }

    /** Analisa um método: atualiza SINKS/PASSA/CAMPOS e devolve os textos de tela achados nele. */
    static Set<AbstractInsnNode> analisar(ClassNode cn, MethodNode mn) {
        Set<AbstractInsnNode> textos = new LinkedHashSet<>();
        if (mn.instructions.size() == 0) return textos;
        Frame<SourceValue>[] frames;
        try { frames = new Analyzer<>(new SourceInterpreter()).analyze(cn.name, mn); } catch (AnalyzerException e) { return textos; }
        Ctx c = new Ctx(cn, mn, frames);
        boolean devolveString = Type.getReturnType(mn.desc).getDescriptor().equals(STR);
        for (AbstractInsnNode in : mn.instructions) {
            Frame<SourceValue> fr = c.at(in);
            if (fr == null) continue;
            if (in instanceof MethodInsnNode m) {
                boolean base0 = CONSUMIDORES.contains(m.owner + "." + m.name);
                Set<Integer> extra = SINKS.get(m.owner + "." + m.name + m.desc);
                if (!base0 && extra == null) continue;
                Type[] args = Type.getArgumentTypes(m.desc);
                int base = fr.getStackSize() - args.length;
                for (int a = 0; a < args.length; a++) {
                    if (!args[a].getDescriptor().equals(STR)) continue;
                    if (!base0 && !extra.contains(a)) continue;
                    for (AbstractInsnNode o : fr.getStack(base + a).insns) resolver(c, o, textos, new HashSet<>());
                }
            } else if (in instanceof FieldInsnNode fi && (fi.getOpcode() == Opcodes.PUTFIELD || fi.getOpcode() == Opcodes.PUTSTATIC)
                    && CAMPOS.contains(fi.owner + "." + fi.name)) {
                for (AbstractInsnNode o : fr.getStack(fr.getStackSize() - 1).insns) resolver(c, o, textos, new HashSet<>());
            } else if (devolveString && in.getOpcode() == Opcodes.ARETURN) {
                // parâmetros que chegam ao return fazem deste método um "repassador"
                Set<AbstractInsnNode> lixo = new HashSet<>();
                Map<String, Set<Integer>> antes = new HashMap<>();
                SINKS.forEach((k, v) -> antes.put(k, new HashSet<>(v)));
                boolean m0 = mudou;
                for (AbstractInsnNode o : fr.getStack(fr.getStackSize() - 1).insns) resolverRetorno(c, o, new HashSet<>());
            }
        }
        return textos;
    }

    static void resolverRetorno(Ctx c, AbstractInsnNode src, Set<AbstractInsnNode> vistos) {
        if (!vistos.add(src)) return;
        if (src instanceof VarInsnNode v && v.getOpcode() == Opcodes.ALOAD) {
            Frame<SourceValue> fr = c.at(src); if (fr == null) return;
            SourceValue local = fr.getLocal(v.var);
            if (local.insns.isEmpty()) { Integer p = c.slotParam.get(v.var); if (p != null) add(PASSA, c.chave(), p); return; }
            for (AbstractInsnNode st : local.insns) if (st instanceof VarInsnNode s && s.getOpcode() == Opcodes.ASTORE) {
                Frame<SourceValue> fs = c.at(st); if (fs == null) continue;
                for (AbstractInsnNode o : fs.getStack(fs.getStackSize() - 1).insns) resolverRetorno(c, o, vistos);
            }
        } else if (src.getOpcode() == Opcodes.CHECKCAST) {
            Frame<SourceValue> fr = c.at(src); if (fr == null) return;
            for (AbstractInsnNode o : fr.getStack(fr.getStackSize() - 1).insns) resolverRetorno(c, o, vistos);
        }
    }

    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[1]);
        List<Path> arquivos;
        try (Stream<Path> s = Files.walk(dir)) { arquivos = s.filter(p -> p.toString().endsWith(".class")).sorted().toList(); }
        Map<Path, ClassNode> nodes = new LinkedHashMap<>();
        for (Path p : arquivos) { ClassNode cn = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(cn, 0); nodes.put(p, cn); }
        int rodadas = 0;
        do {
            mudou = false; rodadas++;
            for (ClassNode cn : nodes.values()) for (MethodNode mn : cn.methods) analisar(cn, mn);
        } while (mudou && rodadas < 30);
        System.err.println("rodadas=" + rodadas + " sinks=" + SINKS.size() + " passa=" + PASSA.size() + " campos=" + CAMPOS.size());

        if (a[0].equals("scan")) {
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get(a[2]), StandardCharsets.UTF_8))) {
                for (ClassNode cn : nodes.values()) for (MethodNode mn : cn.methods) for (AbstractInsnNode in : analisar(cn, mn)) {
                    if (in instanceof LdcInsnNode l) out.println(cn.name + "\tldc\t" + mn.name + "\t" + esc((String) l.cst));
                    else out.println(cn.name + "\tconcat\t" + mn.name + "\t" + esc((String) ((InvokeDynamicInsnNode) in).bsmArgs[0]));
                }
            }
            return;
        }
        Map<String, String> mapa = new HashMap<>();
        for (String linha : Files.readAllLines(Paths.get(a[2]), StandardCharsets.UTF_8)) {
            if (linha.isBlank()) continue;
            String[] t = linha.split("\t", -1);
            mapa.put(unesc(t[0]), unesc(t[1]));
        }
        Path saida = Paths.get(a[3]);
        int trocas = 0, mudadas = 0;
        for (Map.Entry<Path, ClassNode> e : nodes.entrySet()) {
            ClassNode cn = e.getValue();
            boolean alterou = false;
            for (MethodNode mn : cn.methods) for (AbstractInsnNode in : analisar(cn, mn)) {
                if (in instanceof LdcInsnNode l) {
                    String t = mapa.get((String) l.cst);
                    if (t != null) { l.cst = t; trocas++; alterou = true; }
                } else {
                    InvokeDynamicInsnNode d = (InvokeDynamicInsnNode) in;
                    String t = mapa.get((String) d.bsmArgs[0]);
                    if (t != null) {
                        long x = ((String) d.bsmArgs[0]).chars().filter(ch -> ch == 1 || ch == 2).count();
                        long y = t.chars().filter(ch -> ch == 1 || ch == 2).count();
                        if (x != y) throw new IllegalStateException("placeholders diferentes: " + t);
                        d.bsmArgs[0] = t; trocas++; alterou = true;
                    }
                }
            }
            if (!alterou) continue;
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            Path destino = saida.resolve(dir.relativize(e.getKey()));
            Files.createDirectories(destino.getParent());
            Files.write(destino, cw.toByteArray());
            mudadas++;
        }
        System.out.println("trocas=" + trocas + " classes=" + mudadas);
    }
}
