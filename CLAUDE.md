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
- Ícone: a Nebulosa da Hélice do SVG do mantenedor (`docs/brand/helix-nebula-icon.svg`). O launcher usa a imagem renderizada em `app/src/main/res/drawable-nodpi/ic_launcher_foreground.webp` (o Android não aceita blur/ruído em vetor) e o `HelixLogo` usa `core/designsystem/.../drawable-nodpi/helix_logo.webp`; só muda se o mantenedor pedir.

## Arquitetura
Módulos com dependências em uma direção só: `app → features → data → core`. Implementações de `data` são `internal` (o resto vê só a interface `…Repository`). Detalhes em [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Regras de estrutura
- Feature não importa outra feature; o compartilhado sobe para core/data. Navegação entre features passa pelo `app` (por isso a lista de um álbum é a `CollectionScreen` de `features/photos`, aberta pelo `app` a pedido de `features/albums`).
- Mídia só via `data/media`. Core sem regra de negócio. Classes de `core/model` são puras (nada de `android.*`).
- Código novo vai para o módulo da responsabilidade dele. Sem módulo adequado, **perguntar antes** de criar um ou de usar um "quase certo".
- Dependência nova só em `gradle/libs.versions.toml`, e só justificada; conferir na documentação oficial que a versão existe. Aprovadas: Coil (imagens), Zip4j (ZIP AES-256 do cofre), Reorderable (arrastar para reordenar, mesma do BoomListener), ML Kit Text Recognition (OCR do Copiar texto), Room (só quando um recurso exigir, previsto na limpeza).
- Configuração repetida vai para os plugins de `build-logic/`. AGP 9 tem Kotlin embutido: nunca aplicar `org.jetbrains.kotlin.android`; sem `kapt` (KSP).
- Dados da pessoa ficam em DataStore/arquivos. Mudança no formato do que já está guardado **sempre** com migração escrita e testada.

## Limites do app (não mudam)
- **Local primeiro:** nenhuma foto sai do aparelho sem a pessoa pedir (compartilhar). Sem IA no aparelho por ora (decisão do mantenedor; pode mudar). **Exceção aprovada (27/11/2026): reconhecimento de texto (OCR) no aparelho** com ML Kit, modelo latino embutido (`data/media/TextReader`), só sob demanda, para "Copiar texto" no visualizador (inteira ou só de uma área escolhida num retângulo, `TextAreaPicker`; o texto lido é editável antes de copiar); **sem** índice nem busca por texto (decisão do mantenedor), nada é salvo. O APK leva só `arm64-v8a` (`abiFilters`, ML Kit traz ~4 MB nativos por arquitetura). Backup em nuvem só depois do local.
- **Permissões:** `READ_MEDIA_IMAGES` e `READ_MEDIA_VIDEO` (`READ_EXTERNAL_STORAGE` até o Android 12L), `MANAGE_MEDIA` pedido uma vez na primeira abertura, logo após o acesso às fotos (a pessoa pode negar; Ajustes → Galeria → Apagar muda depois; tira a caixa de confirmação ao apagar), `INTERNET` só para o aviso de versão nova, `USE_BIOMETRIC` só para o desbloqueio do cofre. Acesso total às fotos: **sem** o acesso parcial do Android 14. Nada de localização, câmera, microfone ou contatos sem decisão do mantenedor.
- Favoritos e lixeira são os **do sistema** (MediaStore): valem em outros apps. Apagar e lixeira passam pela caixa do sistema (`createTrashRequest`/`createDeleteRequest`/`createFavoriteRequest`).
- Identidade visual própria: inspiração de layout pode, copiar marca ou visual de outro app não.

## Roteiro (aprovado)
1. Base: módulos, CI, tema Helix, ícone, Sobre + aviso de versão, aba Fotos, rolagem rápida por data, barra flutuante, visualizador com zoom e vídeo. **2.** Gerenciar: álbuns, favoritos, lixeira, seleção, compartilhar, detalhes/EXIF. **3.** Visualizar: layouts Regular/Dia/Mês (a aba de busca foi removida a pedido do mantenedor, 27/11/2026). **4.** Cofre: ZIP AES-256 numa pasta pública (ex.: `Documents/HelixGallery/`), senha criada pela pessoa e mostrada uma vez, desbloqueio pela biometria/PIN do Android; a senha é o único acesso após desinstalar. **5.** Editor básico/intermediário (cortar, girar, ajustar, filtros, data/local). **6.** Limpeza (duplicadas/parecidas, capturas, vídeos grandes, compressão com confirmação). **7.** Lugares e Documentos (mapa; sugerir mover documentos para o cofre; exige biblioteca de reconhecimento no aparelho, justificada antes). **8.** Backup (ZIP via SAF; nuvem depois). Fotos "destacadas" e "agrupar parecidas" entram junto das etapas 3 e 6.

## Design (Material 3 Expressive)
- Nenhuma cor, fonte ou forma "na mão" numa tela: `MaterialTheme.colorScheme`, `.typography`, `.shapes`, `.motionScheme`. Valores próprios só em `core/designsystem`. Cores em par com o "on".
- Componentes do `androidx.compose.material3`; componente próprio só sem equivalente, em `core/designsystem`. APIs experimentais só dentro de `core/designsystem`, embrulhadas.
- Paleta própria da Nebulosa da Hélice (azul-noite, ciano, âmbar, magenta); tema escuro por padrão; claro, escuro e AMOLED; cor dinâmica é opção (Ajustes → Aparência).
- **Navegação (modelo do Boom):** as abas (`tab/photos|albums`) são destinos do NavHost com `saveState`/`restoreState` (por isso lembram a posição); direção das animações decidida por `isBackNavigation` na pilha; voltar fica bloqueado enquanto a tela anima; tocar na aba aberta sobe ao topo (`LocalTabReselect`/`ScrollToTopOnReselect`). Ajustes: home por categorias → `settings/appearance|gallery|system|about`, escolhas como `RadioButton` (nunca `DropdownMenu`); ícones novos vêm dos traçados do Boom em `HelixIcons`.
- **Trancar sem senha:** ao criar/abrir o cofre o app guarda a senha cifrada com chave do Keystore **sem** biometria (`AutoAddStore`), só para `addQuietly` (Adicionar a → Cofre, seleção); ver o cofre continua exigindo a senha/biometria. Sem senha guardada, cai no fluxo normal.
- **Cofre (etapa 4):** `data/vault` (ZIP AES-256 com zip4j, arquivo `Documents/HelixGallery/HelixVault.zip` via MediaStore; cópia de trabalho só no cache enquanto aberto, apagada ao trancar) e `features/vault`. Originais só saem do aparelho (`deleteRequest`) depois de gravados no cofre. Desbloqueio rápido: senha cifrada com chave do Keystore que exige biometria/PIN (30 s); sem isso vale a senha digitada. O cofre tranca ao sair da tela e ao ir para segundo plano, e a janela fica com `FLAG_SECURE` enquanto a tela do cofre existe (recentes em branco, sem captura).
- **Abrir com:** `ExternalViewerActivity` (VIEW em `image/*` e `video/*`, content/file) mostra o arquivo em tela própria, fora da navegação; recusa `file://` da pasta de dados do app.
- **Nada de mudança seca (regra):** toda mudança repentina na tela (zoom por toque duplo, reordenar, aparecer/sumir, troca de estado visual) é animada com as specs do tema; transição instantânea é bug.
- **Seleção (modelo do Boom):** `HelixSelectionBar` flutuante (fechar, contagem, selecionar tudo, ⋮ com ações) toma o lugar da barra de baixo (`LocalSelectionBarActive`); o título recua (`recedeWhileSelecting`). Segurar e deslizar na grade seleciona em sequência. Ações da seleção ficam embaixo (`HelixSelectionActionsBar`, lista horizontal; o ⋮ do topo não existe); na lixeira, `HelixBottomActionBar` (Restaurar/Excluir). Cada título de data tem a bolinha de marcar o período (`MediaGrid`); títulos: hoje/ontem, dia da semana até 6 dias, "Dom., 15 de nov.", com ano se for de outro ano. Apagar/lixeira confirmam no `HelixConfirmSheet` avulso (**sem** botão de voltar), nunca na caixa do sistema isolada. **Painel inteligente:** `HelixSmartSheet` (altura anima por fora, conteúdo sai à direita e entra em mola; `HelixMenuAction.page`, `HelixConfirmPage`, voltar redondo nas páginas). Regra: **painel inteligente só onde há para onde voltar** (menu → página, escolha → lista); sem passo anterior é painel independente (`HelixModalSheet`/`HelixConfirmSheet`). Painel que nasce de menu/escolha é página dele, nunca outro painel por cima. **Exceção:** os detalhes do visualizador não são `ModalBottomSheet`: sobem como painel da própria tela empurrando a foto (progresso 0–1 que acompanha o dedo); o gesto vertical (`detailsDrag`) decide o eixo no começo e o carrossel só recebe o horizontal.
- **Abas sem título (Fotos e Álbuns):** nada de `HelixPageTitle` nessas duas abas; o topo é uma faixa de 64dp sob a barra de status, onde flutuam os Ajustes. A aba Fotos abre com a fileira de lembranças (`StoriesRow`/`HelixStoryCard`, dados de `buildStories` em `core/model`: neste dia, álbuns rápidos, favoritas), só com datas e pastas locais; cartão abre uma `MediaSource` (`Day` ou álbum). Telas de dentro continuam com título. A grade da aba Fotos é corrida (`GridLayout.Regular` forçado, sem títulos de data nem bolinhas; a data fica na pílula do topo, `GridDatePill`, que aparece ao rolar; sem o balão do mês ao lado da bolinha) e arredonda só os cantos de fora (`roundedEnds`, forma do tema); álbuns e coleções seguem Regular/Dia/Mês.
- **Abas por gesto:** as abas são um `HorizontalPager` num único destino (`tab/main`), que segue o dedo como os carrosséis do Boom (qualquer aba nova entra no `Tab`). Seleção por arrasto marca a faixa inteira (âncora → dedo) em toda grade (`MediaGrid`; cofre usa `Modifier.dragSelect`). A bolinha da rolagem rápida liga `LocalTabSwipeLock` enquanto o dedo está nela e o `app` trava o pager. Ferramentas do topo de Álbuns são editáveis (`AlbumTool`, guardadas em DataStore): painel com uma lista só (fixadas, divisor, de fora) em que segurar e arrastar reordena, e cruzar o divisor fixa ou tira; sem botões. Tela do cofre: modelo do Babel-Manager com campo redondo e botão de olho. Barra do visualizador sem fundo (gradiente leve); vídeo declara `AudioAttributes` (pausa o resto do sistema).
- Todo movimento usa `helixSpatialSpec`/`helixFastSpatialSpec`/`helixEffectsSpec`/`helixScreenSpec` (nada de `tween(300)` à mão).
- Painéis de baixo sempre `HelixModalSheet` (voltar do app, nunca o do M3; exceção: detalhes do visualizador, acima). Menu dos 3 pontinhos = painel emergente (`HelixMoreMenu`/`HelixMenuSheet`), nunca `DropdownMenu`. Telas de dentro: `HelixSubPage` (título grande + botão de voltar flutuante).
- **Proibida barra superior presa no topo** (barra de pesquisa não entra na regra): o título é a primeira linha da lista (`HelixPageTitle`, rola junto) e o voltar flutua (`HelixFloatingBackButton`, círculo com opacidade 0,9; o título deixa `HelixBackButtonInset` livre para o botão nunca cobri-lo). Botões que flutuam sobre a página usam `HelixFloatingCircle`. Barra da seleção flutua e toma o lugar do voltar.
- Controles de vídeo e barra de progresso = `HelixSeekBar` (porte do `BoomSeekBar`); não copiar o visual de outro app, tirar do código do Boom. Rolagem rápida = só a alça recebe o dedo. Pinça na grade troca layout/colunas (`nextGridZoom`); na aba Fotos (sempre corrida) só as colunas (`zoomStep(regularOnly)`). As lembranças ficam no lugar ao selecionar (só sem toque) e são calculadas no `CollectionViewModel` fora do fio principal. Todo gesto de arrastar tem alternativa por ações de acessibilidade (`customActions`).
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
- Já feitos pelo mantenedor desde o início: GitHub Pages ativo (pasta `docs/` da `main`) e repositório público (o aviso de versão depende disso). Não lembrar de novo. Opcional: instalar o app Claude do GitHub para revisão por PR.

## Contexto atual
- Etapas 4 (cofre), 5 (editor: `features/editor`, salva cópia JPEG via `data/media/MediaWriter`, aberto pelo ⋮ do visualizador), 6 (limpeza: capturas, vídeos grandes, duplicadas exatas por SHA-256; falta compressão e "parecidas") e 8 (backup ZIP via SAF) escritas e com CI verde. Falta: etapa 7 (Lugares/Documentos), vistoria no aparelho.
- **Planejado (pedido do mantenedor, 28/11/2026):** "riscar áreas" no Copiar texto, isto é, riscar com o dedo as partes da imagem a **ignorar** no OCR (o recorte retangular, `TextAreaPicker`, já existe; riscar é a interação que falta). Entra depois do seletor de cor.
- Já feito após a primeira vistoria: abas com posição salva, animações do Boom, rolagem rápida por pixels, Abrir com, Ajustes por categorias. Aguardando nova vistoria e a escolha do ícone (modelos A–F).
- Etapas 1 a 3 do roteiro escritas de uma vez (v0.1.0 ainda sem Release). Falta vistoria no aparelho: o primeiro CI decide se compila. Itens conhecidos da primeira entrega: sem "fotos destacadas" nem "agrupar parecidas" ainda; local do EXIF (GPS) não é lido (exigiria `ACCESS_MEDIA_LOCATION`, decisão do mantenedor).
