# CLAUDE.md

Lido no início de cada sessão: é como o Claude recupera o contexto do projeto. Detalhes técnicos ficam no [`ARCHITECTURE.md`](ARCHITECTURE.md); o histórico, no [`CHANGELOG.md`](CHANGELOG.md). Em conflito entre uma instrução antiga do chat e este arquivo, **vale este arquivo**. Decisão nova que mude algo aqui entra neste arquivo no mesmo commit. O projeto segue o "Estrutura Universal para Vibe Coding" do mantenedor (versão 20/11/2026) e é irmão do BoomListener (`rilclesaraiva/BoomListener`), de onde vêm o build, o tema e os padrões de tela.

## Projeto
**Helix Gallery** (nome único do app, no ícone e em todo texto; vem da Nebulosa da Hélice, o ícone): galeria de fotos e vídeos **local** para Android, no estilo do Google Fotos, em Material 3 Expressive, com mais recursos. Gratuito, sem anúncios, sem conta. Público: **global**. Repositório público.
- Nome nos arquivos: `helixgallery` (`helixgallery_vX.Y.Z.apk`; título da Release: "Helix Gallery vX.Y.Z").

## Mantenedor
- Nome: **rilclesaraiva** · Contato: saraivarilcle@gmail.com (e as issues do GitHub)
- Apoio: https://buymeacoffee.com/saraivarilcle (só no README, nunca no app) · Licença: GPL-3.0 (cabeçalho de licença no topo de cada arquivo-fonte)
- País: BR (`C=BR`) · Segredos ficam em: Google Drive (pasta privada do mantenedor)

## Stack
- Kotlin 2.4 + Jetpack Compose (Material 3 Expressive) + Media3 + Coil 3. AGP 9.4, Gradle 9.6, `compileSdk 37`, `targetSdk 36`, `minSdk 31` (Android 12), Java 17 (o Gradle roda com JDK 21).
- Build: `./gradlew assembleRelease` · Testes: `./gradlew testDebugUnitTest`. **Nada do Gradle compila nas sessões do Claude** (repositórios Maven fora da rede liberada): a única prova é o CI, e eu digo isso em vez de afirmar que testei.
- Mapa (módulos Gradle): core → `core/*` · data → `data/*` · features → `features/*` (no **plural**, decisão do mantenedor; diferente do BoomListener) · entrada → `app`.
- Versão: `APP_VERSION` em `gradle.properties` (`versionCode = MAJOR*10000 + MINOR*100 + PATCH`; nunca editar `versionCode`/`versionName` direto).
- Versionamento: SemVer, **regra e não escolha**: o número sai do `[Unreleased]`. Só correções → PATCH; algo novo ou alterado compatível → MINOR; quebra de compatibilidade → MAJOR. Trocar o sistema só com pedido explícito.
- Release: commit de release na main (APP_VERSION novo + seção no CHANGELOG, sem tag) → `.github/workflows/ci.yml` cria a tag `vX.Y.Z` e publica o APK. Build para vistoria: artefato de cada push.
- Pacote: `com.helixgallery.app`, fixo para sempre · R8 ativo no release, nunca desligar (com `-dontobfuscate`).
- Ícone: `app/src/main/res/mipmap-anydpi/` e `drawable/ic_launcher_foreground.xml` (a Nebulosa da Hélice); só muda se o mantenedor pedir.

## Arquitetura
Módulos com dependências em uma direção só: `app → features → data → core`. Implementações de `data` são `internal` (o resto vê só a interface `…Repository`). Detalhes em [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Regras de estrutura
- Feature não importa outra feature; o compartilhado sobe para core/data. Navegação entre features passa pelo `app` (por isso a lista de um álbum é a `CollectionScreen` de `features/photos`, aberta pelo `app` a pedido de `features/albums`).
- Mídia só via `data/media`. Core sem regra de negócio. Classes de `core/model` são puras (nada de `android.*`).
- Código novo vai para o módulo da responsabilidade dele. Sem módulo adequado, **perguntar antes** de criar um ou de usar um "quase certo".
- Dependência nova só em `gradle/libs.versions.toml`, e só justificada; conferir na documentação oficial que a versão existe. Aprovadas: Coil (imagens), Room (só quando um recurso exigir, previsto na limpeza).
- Configuração repetida vai para os plugins de `build-logic/`. AGP 9 tem Kotlin embutido: nunca aplicar `org.jetbrains.kotlin.android`; sem `kapt` (KSP).
- Dados da pessoa ficam em DataStore/arquivos. Mudança no formato do que já está guardado **sempre** com migração escrita e testada.

## Limites do app (não mudam)
- **Local primeiro:** nenhuma foto sai do aparelho sem a pessoa pedir (compartilhar). Sem IA no aparelho por ora (decisão do mantenedor; pode mudar). Backup em nuvem só depois do local.
- **Permissões:** `READ_MEDIA_IMAGES` e `READ_MEDIA_VIDEO` (`READ_EXTERNAL_STORAGE` até o Android 12L), `MANAGE_MEDIA` opcional (pessoa libera nas configurações do Android; tira a caixa de confirmação ao apagar), `INTERNET` só para o aviso de versão nova. Acesso total às fotos: **sem** o acesso parcial do Android 14. Nada de localização, câmera, microfone ou contatos sem decisão do mantenedor.
- Favoritos e lixeira são os **do sistema** (MediaStore): valem em outros apps. Apagar e lixeira passam pela caixa do sistema (`createTrashRequest`/`createDeleteRequest`/`createFavoriteRequest`).
- Identidade visual própria: inspiração de layout pode, copiar marca ou visual de outro app não.

## Roteiro (aprovado)
1. Base: módulos, CI, tema Helix, ícone, Sobre + aviso de versão, aba Fotos, rolagem rápida por data, barra flutuante, visualizador com zoom e vídeo. **2.** Gerenciar: álbuns, favoritos, lixeira, seleção, compartilhar, detalhes/EXIF. **3.** Visualizar: layouts Regular/Dia/Mês, busca. **4.** Cofre: ZIP AES-256 numa pasta pública (ex.: `Documents/HelixGallery/`), senha criada pela pessoa e mostrada uma vez, desbloqueio pela biometria/PIN do Android; a senha é o único acesso após desinstalar. **5.** Editor básico/intermediário (cortar, girar, ajustar, filtros, data/local). **6.** Limpeza (duplicadas/parecidas, capturas, vídeos grandes, compressão com confirmação). **7.** Lugares e Documentos (mapa; sugerir mover documentos para o cofre; exige biblioteca de reconhecimento no aparelho, justificada antes). **8.** Backup (ZIP via SAF; nuvem depois). Fotos "destacadas" e "agrupar parecidas" entram junto das etapas 3 e 6.

## Design (Material 3 Expressive)
- Nenhuma cor, fonte ou forma "na mão" numa tela: `MaterialTheme.colorScheme`, `.typography`, `.shapes`, `.motionScheme`. Valores próprios só em `core/designsystem`. Cores em par com o "on".
- Componentes do `androidx.compose.material3`; componente próprio só sem equivalente, em `core/designsystem`. APIs experimentais só dentro de `core/designsystem`, embrulhadas.
- Paleta própria da Nebulosa da Hélice (azul-noite, ciano, âmbar, magenta); tema escuro por padrão; claro, escuro e AMOLED; cor dinâmica é opção (Ajustes → Aparência).
- Todo movimento usa `helixSpatialSpec`/`helixFastSpatialSpec`/`helixEffectsSpec`/`helixScreenSpec` (nada de `tween(300)` à mão).
- Painéis de baixo sempre `HelixModalSheet`. Telas de dentro: `HelixSubPage` (título grande + botão de voltar flutuante).
- **Cores dos ícones dos Ajustes:** `HelixAccent` (famílias fixas), mesma família por categoria (Aparência = Purple/Pink, Galeria = Teal, Sistema = Green, Sobre = Blue). Ícone novo em Ajustes sempre com `accent` explícito.
- Toda opção dos Ajustes mora numa categoria. Aviso de atualização é item recolhível em Sistema → Atualizações (modelo BoomListener), sem painel solto.

## Idiomas do app
- Inglês (padrão, `res/values`), português e espanhol: **toda** chave nova nos três (o release falha com `MissingTranslation`). Nenhum texto visível escrito no Kotlin (inclusive `contentDescription`).
- README, CHANGELOG e páginas legais em inglês; CLAUDE.md, ARCHITECTURE.md e mensagens de commit em português.

## Fluxo de trabalho
- **Todo pedido vira item na lista de tarefas da sessão** antes de começar; só marco como feito o que está feito; antes de dizer que terminou, releio a mensagem original e confiro item por item.
- **Bug que não reproduzo aqui: log primeiro, correção depois.** Só chamo de causa o que o log sustentar.
- Uma tarefa por vez, mudanças pequenas. Não refatorar fora do escopo. Na dúvida, perguntar. Nunca inventar API.
- Toda lógica de `data/*` e todo ViewModel novo vem com teste unitário; `core/model` já tem.
- Registrar toda mudança em `[Unreleased]` no CHANGELOG, escrito para quem usa o app. **Sem subir versão** fora do release.
- Só dizer que terminou quando o CI daquele commit passar. CI vermelho é corrigido antes de qualquer tarefa nova (o erro vem como comentário no commit: `gh api repos/rilclesaraiva/Helix-Gallery/commits/<sha>/comments`).
- Tudo direto na `main`: sem branches e sem PR. Cada push gera o build para vistoria.
- **Link do build para vistoria = execução do workflow `CI`**, nunca a do "pages build and deployment". Conferir `.name == "CI"` antes de avisar.
- Bloqueio de segurança do ambiente (proxy, permissão, leitura de credencial) nunca é contornado.
- Preferir remover a acumular. Navegação sempre por `navigateOnce`/`popBackStackOnce`.

## Release (regra absoluta)
- Só com confirmação explícita do mantenedor citando a versão ("confirmo release v1.2.0"). "Ok" ou "pode lançar" sem versão não contam.
- Sem confirmação: nada de tag, bump de versão, mover `[Unreleased]` ou Release. O Claude nunca cria tag nem Release: o commit `chore(release)` na main basta, e o CI faz o resto.
- Com a confirmação: revisão com contexto limpo (subagente lê só este arquivo e o diff), achados ao mantenedor; mover `[Unreleased]` para `## [X.Y.Z] - AAAA-MM-DD`; `APP_VERSION=X.Y.Z`; commit `chore(release): vX.Y.Z`; o CI cria a tag e publica.
- Entre a vistoria e a tag só mudam versão e CHANGELOG; mudou código, nova vistoria.

## Segredos
- Nunca commitar chaves, keystores, senhas ou `.env`; nunca ler nem exibir o conteúdo deles. A keystore de release do Helix **nunca é gerada de novo** (trocar impede atualizar o app). Os 4 Secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) só existem no GitHub, cadastrados pelo mantenedor.

## Projeto público
- Páginas legais em `docs/privacy.md`, `docs/terms.md` e `docs/licenses.md` (GitHub Pages: `https://rilclesaraiva.github.io/Helix-Gallery/privacy`, `/terms`, `/licenses`), abertas no navegador a partir de Ajustes → Sobre. O app não tem telas com esses textos.
- Coleta nova (SDK, permissão, chamada de rede) → atualizar a política no mesmo commit. Biblioteca nova → `docs/licenses.md`.
- Nenhuma afirmação sem respaldo: o que README, páginas legais e notas dizem precisa ser verdade no código.
- README termina com apoio (Buy Me a Coffee) e contato. No app: nunca citar doação.

## Limitações das sessões do Claude
- Artefatos da Action não baixam (403); logs da Action não são legíveis: o workflow posta o erro no commit. Secrets e tags dão 403 (o mantenedor cadastra; o CI cria a tag).
- Ações do mantenedor pendentes: ativar o GitHub Pages (pasta `docs/` da `main`), manter o repositório público (o aviso de versão depende disso), instalar o app Claude do GitHub se quiser revisão por PR.

## Contexto atual
- Etapas 1 a 3 do roteiro escritas de uma vez (v0.1.0 ainda sem Release). Falta vistoria no aparelho: o primeiro CI decide se compila. Itens conhecidos da primeira entrega: sem "fotos destacadas" nem "agrupar parecidas" ainda; a busca abre o visualizador na lista de todas as fotos; local do EXIF (GPS) não é lido (exigiria `ACCESS_MEDIA_LOCATION`, decisão do mantenedor).
