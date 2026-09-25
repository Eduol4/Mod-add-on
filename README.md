# Dweller Add-on

Mod-companheiro para um Mod (Minecraft 1.20.1 / Forge 47.4.10).
Adiciona personalidade por spawn:

- **80% passiva**: segue o jogador mais proximo mantendo distancia, sem atacar, sem "stare", sem fuga.
- **20% normal**: comportamento padrao do mod, intacto.
- Uma Dweller **passiva** atacada por um jogador **vira hostil** e se defende; volta a ser passiva
  cerca de **25 segundos** apos o ultimo golpe.
- A personalidade e **sorteada uma vez** por criatura e **salva** (sobrevive a recarregar o mundo).

O mod original nao e modificado: este add-on roda ao lado dele.

## Como compilar (na nuvem, sem instalar nada)

1. Suba estes arquivos para o repositorio (mantendo a estrutura de pastas).
2. O GitHub Actions compila sozinho a cada push (aba **Actions**).
3. Ao terminar (verde), abra a execucao e baixe o **Artifact** chamado `jenny-dweller-addon`.
4. Dentro do zip esta o jar. Coloque-o na pasta `mods`, junto do jar da Jenny.

Nao e preciso colocar o jar da Jenny em lugar nenhum: o add-on a reconhece em tempo de execucao.

## Estrutura

- `build.gradle`, `settings.gradle`, `gradle.properties` — configuracao de build.
- `.github/workflows/build.yml` — compila automaticamente no GitHub.
- `src/main/java/com/eduol4/jennyaddon/` — codigo do add-on.
- `src/main/resources/META-INF/` — metadados do mod e access transformer.
