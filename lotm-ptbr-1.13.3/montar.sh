#!/usr/bin/env bash
# Monta o LOTM PTBR a partir do jar original lotmcraft-1.13.3.jar (uso pessoal).
#   ./montar.sh <lotmcraft-1.13.3.jar> <classpath-neoforge-1.21.1> <classpath-asm>
# O classpath do NeoForge sai de: ./gradlew -I imprimirCp (sourceSets.main.compileClasspath do projeto).
set -euo pipefail
ORIG=$1; CP=$2; ASM=$3; D=$(cd "$(dirname "$0")" && pwd); W=$(mktemp -d)
mkdir -p "$W/orig" "$W/out" "$W/lit" "$W/montar/assets/lotmcraft/lang" "$W/montar/META-INF"
unzip -q "$ORIG" 'de/*' -d "$W/orig"
# 1) nomes de poção/receita/característica como formato traduzível
javac --release 21 -nowarn -proc:none -cp "$ORIG:$CP" -d "$W/out" $(find "$D/classes-corrigidas" -name '*.java')
cp -r "$W/out/de" "$W/orig/"
# 2) textos fixos que vão para a tela, trocados pelo português
javac -nowarn -cp "$ASM" -d "$W/ferr" "$D/ferramenta/Literais.java"
java -cp "$ASM:$W/ferr" Literais aplicar "$W/orig" "$D/textos-fixos.tsv" "$W/lit"
cp -r "$W/out/de" "$W/montar/"; cp -r "$W/lit/de" "$W/montar/"
# 3) idiomas: pt_br completo; os outros ganham os formatos novos
unzip -q "$ORIG" 'assets/lotmcraft/lang/*' -d "$W/montar"
cp "$D/pt_br.json" "$W/montar/assets/lotmcraft/lang/pt_br.json"
python3 - "$W/montar/assets/lotmcraft/lang" <<'PY'
import json,os,sys
d=sys.argv[1]; en=json.load(open(d+'/en_us.json',encoding='utf-8'))
for f in os.listdir(d):
    if f=='pt_br.json': continue
    j=json.load(open(d+'/'+f,encoding='utf-8'))
    for b in ['lotm.potion_recipe','lotm.pathway','lotm.beyonder_characteristic','lotm.potion']: j[b+'.named']='%s '+j.get(b,en[b])
    json.dump(j,open(d+'/'+f,'w',encoding='utf-8'),ensure_ascii=False,indent=2)
PY
# 4) nome do mod
unzip -p "$ORIG" META-INF/neoforge.mods.toml | sed 's/^displayName="LOTMCraft"/displayName="LOTM PTBR"/' > "$W/montar/META-INF/neoforge.mods.toml"
cp "$ORIG" LOTM-PTBR-1.13.3.jar && (cd "$W/montar" && zip -q -r "$OLDPWD/LOTM-PTBR-1.13.3.jar" .)
echo "pronto: LOTM-PTBR-1.13.3.jar"
