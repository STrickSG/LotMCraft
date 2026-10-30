package de.jakob.lotm.util.playerMap;

import java.util.Map;

import java.util.Locale;

import java.text.Normalizer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedList;
import java.util.List;

public record HonorificName(LinkedList<String> lines) {
    static public final String NBT_LINES = "honorific_name_lines";

    static public final HonorificName EMPTY = new HonorificName(new LinkedList<>());

    static public LinkedList<String> getMustHaveWords(String path){
        return new LinkedList<String>(
                switch (path){
                    case "fool" -> List.of("History", "Mystery", "Bizarreness", "Change", "Wishes", "Miracles", "Drama", "Performances", "Fools");
                    case "error" -> List.of("Time", "Deceit", "Trickery", "Loopholes", "Tampering", "Exploitation", "Bugs", "Theft", "Errors");
                    case "door" -> List.of("Space", "Stars", "Cosmos", "Worlds", "Concealment", "Seals", "Travels", "Chronicles", "Doors");
                    case "visionary" -> List.of("Mind", "Dreams", "Visions", "Fantasies", "Imagination", "Discernment", "Psyche", "Coincidence", "Arrangements");
                    case "sun" -> List.of( "Light", "Order", "Holiness", "Contracts", "Justice", "Energy", "Sun", "Daytime", "Inextinguishable");
                    case "tyrant" -> List.of("Storms", "Calamities", "Seas", "Skies", "Tsunamis", "Undersea", "Creatures", "Tyranny", "Wrath");
                    case "white_tower" -> List.of("Knowledge", "Observation", "Reasoning", "Cognition", "Wisdom", "Reason", "Omniscience", "Learning", "Madness");
                    case "hanged_man" -> List.of("Degeneration", "Corruption", "Mutation", "Shadows", "Darkness", "Whispers", "Sacrifice", "Depravity", "Words");
                    case "darkness" -> List.of("Darkness", "Dreams", "Silence", "Horror", "Repose", "Misfortune", "Concealment", "Stars", "Night");
                    case "death" -> List.of("Death", "Undeath", "Spirits", "Underworld", "Pallor", "Styx", "Dead", "Eternal", "Rest");
                    case "twilight_light" -> List.of("Twilight", "Decay", "Glory", "Passage","Time", "Dawn", "Dusk", "Power", "Combat");
                    case "demoness" -> List.of("Chaos", "Catastrophes","Mirror", "Disasters", "Apocalypse", "Strife", "Plagues", "Primordium", "Femininity");
                    case "red_priest" -> List.of("War", "Calamity", "Iron","Blood", "Battlefield", "Strife", "Chaos", "Destruction", "Masculinity");
                    case "hermit" -> List.of("Knowledge", "Information", "Symbols", "Magic", "Data", "Numbers", "Education", "Stories", "Mysticism");
                    case "paragon" -> List.of("Technology", "Logic", "Machinery", "Physics", "Civilizations", "Structure", "Essence", "Education", "Enlightenment");
                    case "wheel_of_fortune" -> List.of("Fate", "Luck", "Lucky", "Misfortune", "Fortune", "River", "Calamities", "Probability", "Madness", "Chaos");
                    case "mother" -> List.of("Earth", "Fertility", "Reproduction", "Desolation", "Nature", "Origin", "Life", "Harvest", "Bounty");
                    case "moon" -> List.of("Moon", "Fertility", "Reproduction", "Spirituality", "Beauty", "Proliferation", "Moonlight", "Silver", "Crimson");
                    case "abyss" -> List.of("Devils", "Evil", "Desires", "Corruption", "Blood", "Malice", "Blathers", "Corrosion", "Filth");
                    case "chained" -> List.of("Chains", "Temperance", "Deviants", "Restraint", "Indulgence", "Unity", "Binding", "Curses", "Thorns");
                    case "black_emperor" -> List.of("Disorder", "Distortion", "Exploitation", "Entropy", "Abolition", "Frenzy", "Domination", "Black", "Shadow");
                    case "justiciar" -> List.of("Laws", "Rules", "Balance", "Discipline", "Justice", "Judgement", "Order", "White", "Punishment");
                    default -> List.of();
        });
    }

    // Traduções em português das palavras obrigatórias. O jogador pode usar a palavra em inglês ou em português.
    private static final Map<String, String> PORTUGUESE_WORDS = Map.ofEntries(
            Map.entry("History", "História"),
            Map.entry("Mystery", "Mistério"),
            Map.entry("Bizarreness", "Bizarrice"),
            Map.entry("Change", "Mudança"),
            Map.entry("Wishes", "Desejos"),
            Map.entry("Miracles", "Milagres"),
            Map.entry("Drama", "Drama"),
            Map.entry("Performances", "Atuações"),
            Map.entry("Fools", "Tolos"),
            Map.entry("Time", "Tempo"),
            Map.entry("Deceit", "Engano"),
            Map.entry("Trickery", "Trapaça"),
            Map.entry("Loopholes", "Brechas"),
            Map.entry("Tampering", "Adulteração"),
            Map.entry("Exploitation", "Exploração"),
            Map.entry("Bugs", "Falhas"),
            Map.entry("Theft", "Roubo"),
            Map.entry("Errors", "Erros"),
            Map.entry("Space", "Espaço"),
            Map.entry("Stars", "Estrelas"),
            Map.entry("Cosmos", "Cosmos"),
            Map.entry("Worlds", "Mundos"),
            Map.entry("Concealment", "Ocultação"),
            Map.entry("Seals", "Selos"),
            Map.entry("Travels", "Viagens"),
            Map.entry("Chronicles", "Crônicas"),
            Map.entry("Doors", "Portas"),
            Map.entry("Mind", "Mente"),
            Map.entry("Dreams", "Sonhos"),
            Map.entry("Visions", "Visões"),
            Map.entry("Fantasies", "Fantasias"),
            Map.entry("Imagination", "Imaginação"),
            Map.entry("Discernment", "Discernimento"),
            Map.entry("Psyche", "Psique"),
            Map.entry("Coincidence", "Coincidência"),
            Map.entry("Arrangements", "Arranjos"),
            Map.entry("Light", "Luz"),
            Map.entry("Order", "Ordem"),
            Map.entry("Holiness", "Santidade"),
            Map.entry("Contracts", "Contratos"),
            Map.entry("Justice", "Justiça"),
            Map.entry("Energy", "Energia"),
            Map.entry("Sun", "Sol"),
            Map.entry("Daytime", "Dia"),
            Map.entry("Inextinguishable", "Inextinguível"),
            Map.entry("Storms", "Tempestades"),
            Map.entry("Calamities", "Calamidades"),
            Map.entry("Seas", "Mares"),
            Map.entry("Skies", "Céus"),
            Map.entry("Tsunamis", "Tsunamis"),
            Map.entry("Undersea", "Profundezas"),
            Map.entry("Creatures", "Criaturas"),
            Map.entry("Tyranny", "Tirania"),
            Map.entry("Wrath", "Ira"),
            Map.entry("Knowledge", "Conhecimento"),
            Map.entry("Observation", "Observação"),
            Map.entry("Reasoning", "Raciocínio"),
            Map.entry("Cognition", "Cognição"),
            Map.entry("Wisdom", "Sabedoria"),
            Map.entry("Reason", "Razão"),
            Map.entry("Omniscience", "Onisciência"),
            Map.entry("Learning", "Aprendizado"),
            Map.entry("Madness", "Loucura"),
            Map.entry("Degeneration", "Degeneração"),
            Map.entry("Corruption", "Corrupção"),
            Map.entry("Mutation", "Mutação"),
            Map.entry("Shadows", "Sombras"),
            Map.entry("Darkness", "Escuridão"),
            Map.entry("Whispers", "Sussurros"),
            Map.entry("Sacrifice", "Sacrifício"),
            Map.entry("Depravity", "Depravação"),
            Map.entry("Words", "Palavras"),
            Map.entry("Silence", "Silêncio"),
            Map.entry("Horror", "Horror"),
            Map.entry("Repose", "Repouso"),
            Map.entry("Misfortune", "Infortúnio"),
            Map.entry("Night", "Noite"),
            Map.entry("Death", "Morte"),
            Map.entry("Undeath", "Não-Morte"),
            Map.entry("Spirits", "Espíritos"),
            Map.entry("Underworld", "Submundo"),
            Map.entry("Pallor", "Palidez"),
            Map.entry("Styx", "Estige"),
            Map.entry("Dead", "Mortos"),
            Map.entry("Eternal", "Eterno"),
            Map.entry("Rest", "Descanso"),
            Map.entry("Twilight", "Crepúsculo"),
            Map.entry("Decay", "Decadência"),
            Map.entry("Glory", "Glória"),
            Map.entry("Passage", "Passagem"),
            Map.entry("Dawn", "Alvorada"),
            Map.entry("Dusk", "Anoitecer"),
            Map.entry("Power", "Poder"),
            Map.entry("Combat", "Combate"),
            Map.entry("Chaos", "Caos"),
            Map.entry("Catastrophes", "Catástrofes"),
            Map.entry("Mirror", "Espelho"),
            Map.entry("Disasters", "Desastres"),
            Map.entry("Apocalypse", "Apocalipse"),
            Map.entry("Strife", "Conflito"),
            Map.entry("Plagues", "Pragas"),
            Map.entry("Primordium", "Primórdio"),
            Map.entry("Femininity", "Feminilidade"),
            Map.entry("War", "Guerra"),
            Map.entry("Calamity", "Calamidade"),
            Map.entry("Iron", "Ferro"),
            Map.entry("Blood", "Sangue"),
            Map.entry("Battlefield", "Batalha"),
            Map.entry("Destruction", "Destruição"),
            Map.entry("Masculinity", "Masculinidade"),
            Map.entry("Information", "Informação"),
            Map.entry("Symbols", "Símbolos"),
            Map.entry("Magic", "Magia"),
            Map.entry("Data", "Dados"),
            Map.entry("Numbers", "Números"),
            Map.entry("Education", "Educação"),
            Map.entry("Stories", "Histórias"),
            Map.entry("Mysticism", "Misticismo"),
            Map.entry("Technology", "Tecnologia"),
            Map.entry("Logic", "Lógica"),
            Map.entry("Machinery", "Maquinário"),
            Map.entry("Physics", "Física"),
            Map.entry("Civilizations", "Civilizações"),
            Map.entry("Structure", "Estrutura"),
            Map.entry("Essence", "Essência"),
            Map.entry("Enlightenment", "Iluminação"),
            Map.entry("Fate", "Destino"),
            Map.entry("Luck", "Sorte"),
            Map.entry("Lucky", "Sortudo"),
            Map.entry("Fortune", "Fortuna"),
            Map.entry("River", "Rio"),
            Map.entry("Probability", "Probabilidade"),
            Map.entry("Earth", "Terra"),
            Map.entry("Fertility", "Fertilidade"),
            Map.entry("Reproduction", "Reprodução"),
            Map.entry("Desolation", "Desolação"),
            Map.entry("Nature", "Natureza"),
            Map.entry("Origin", "Origem"),
            Map.entry("Life", "Vida"),
            Map.entry("Harvest", "Colheita"),
            Map.entry("Bounty", "Fartura"),
            Map.entry("Moon", "Lua"),
            Map.entry("Spirituality", "Espiritualidade"),
            Map.entry("Beauty", "Beleza"),
            Map.entry("Proliferation", "Proliferação"),
            Map.entry("Moonlight", "Luar"),
            Map.entry("Silver", "Prata"),
            Map.entry("Crimson", "Carmesim"),
            Map.entry("Devils", "Diabos"),
            Map.entry("Evil", "Mal"),
            Map.entry("Desires", "Desejos"),
            Map.entry("Malice", "Malícia"),
            Map.entry("Blathers", "Tagarelices"),
            Map.entry("Corrosion", "Corrosão"),
            Map.entry("Filth", "Imundície"),
            Map.entry("Chains", "Correntes"),
            Map.entry("Temperance", "Temperança"),
            Map.entry("Deviants", "Desviantes"),
            Map.entry("Restraint", "Contenção"),
            Map.entry("Indulgence", "Indulgência"),
            Map.entry("Unity", "Unidade"),
            Map.entry("Binding", "Amarras"),
            Map.entry("Curses", "Maldições"),
            Map.entry("Thorns", "Espinhos"),
            Map.entry("Disorder", "Desordem"),
            Map.entry("Distortion", "Distorção"),
            Map.entry("Entropy", "Entropia"),
            Map.entry("Abolition", "Abolição"),
            Map.entry("Frenzy", "Frenesi"),
            Map.entry("Domination", "Dominação"),
            Map.entry("Black", "Negro"),
            Map.entry("Shadow", "Sombra"),
            Map.entry("Laws", "Leis"),
            Map.entry("Rules", "Regras"),
            Map.entry("Balance", "Equilíbrio"),
            Map.entry("Discipline", "Disciplina"),
            Map.entry("Judgement", "Julgamento"),
            Map.entry("White", "Branco"),
            Map.entry("Punishment", "Punição")
    );

    /** Chave de tradução usada para mostrar a palavra no idioma do jogador. */
    public static String wordTranslationKey(String word) {
        return "lotmcraft.honorific_word." + word.toLowerCase(Locale.ROOT);
    }

    // Compara sem diferenciar maiúsculas nem acentos ("Historia" vale como "História")
    private static String normalize(String word) {
        return Normalizer.normalize(word, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static boolean matchesWord(String input, String target) {
        String normalized = normalize(input);
        if (normalized.equals(normalize(target))) return true;
        String portuguese = PORTUGUESE_WORDS.get(target);
        return portuguese != null && normalized.equals(normalize(portuguese));
    }

    public static int MAX_LENGTH = 200;

    public boolean contains(String str) {return lines.contains(str);}

    public static boolean validate(String path, LinkedList<String> list){
        var mustHave = getMustHaveWords(path);

        for (var str : list){
            var words = List.of(str.split("[ ,]"));
            if(!words.stream().
                    anyMatch(input -> mustHave.stream().
                            anyMatch(target -> matchesWord(input, target))))
                return false;
        }

        return true;
    }

    public boolean isEmpty(){
        return lines.isEmpty();
    }

    public HonorificName addLine(String str){
        LinkedList<String> result = new LinkedList<>(lines);
        result.add(str);

        return new HonorificName(result);
    }

    public String getAllInfo(){
        return !lines.isEmpty()?
                ("\n  Line 1: " + lines.get(0)
                + "\n  Line 2: " + lines.get(1)
                + "\n  Line 3: " + lines.get(2)
                + ((lines.size() >= 4) ? ("\n  Line 4: " + lines.get(3)) : "")
                + ((lines.size() == 5) ? ("\n  Line 5: " + lines.get(4)) : ""))
                : "None";
    }

    public CompoundTag toNBT(){
        CompoundTag tag = new CompoundTag();

        ListTag list = new ListTag();
        for (var value : lines) {
            list.add(StringTag.valueOf(value));
        }

        tag.put(NBT_LINES, list);

        return tag;
    }

    static public HonorificName fromNBT(CompoundTag tag){
        LinkedList<String> list = new LinkedList<>();

        if (tag.contains(NBT_LINES, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(NBT_LINES, Tag.TAG_STRING);

            for (var obj : listTag) {
                list.add(obj.getAsString());
            }
        }

        return new HonorificName(list);
    }

    public static HonorificName fromNetwork(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        LinkedList<String> list = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            list.add(buf.readUtf(MAX_LENGTH));
        }
        return new HonorificName(list);
    }

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(lines.size());
        for (String line : lines) {
            buf.writeUtf(line, MAX_LENGTH);
        }
    }
}
