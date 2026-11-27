# ARCHITECTURE.md

Detalhes técnicos do Helix Gallery. Regras e decisões ficam no [`CLAUDE.md`](CLAUDE.md).

## Módulos
```
app                      MainActivity, HelixApp (permissão + NavHost), MainTabs (Fotos/Álbuns + barra flutuante)
features/photos          CollectionScreen: grade de uma origem (Todas, Álbum, Favoritas, Vídeos, Capturas, Lixeira), seleção e ações
features/albums          AlbumsScreen: coleções prontas + pastas do aparelho
features/viewer          ViewerScreen: carrossel (HorizontalPager), ZoomableImage, VideoPage (Media3), detalhes/EXIF
features/settings        SettingsScreen, AboutScreen, UpdatePromptHost (aviso de versão nova)
data/media               MediaRepository (MediaStore): lista, observador, pedidos de lixeira/apagar/favoritar
data/updates             Consulta da última Release no GitHub, adiar/pular aviso
core/model               Classes puras: MediaItem, MediaSource, AlbumSummary, GridEntry, UserPreferences
core/common              navigateOnce/popBackStackOnce, permissões de mídia, compartilhar, abrir link
core/datastore           PreferencesRepository (tema, cor dinâmica, layout e colunas)
core/designsystem        HelixTheme, motion, componentes (grade, rolagem rápida, barra flutuante, Ajustes, logo, painel)
build-logic              plugins de convenção helix.android.application|library|compose|feature
```
Dependências: `app → features → data → core`. Features não se importam; o `app` liga uma à outra pela navegação.

## Mídia
- `MediaStoreRepository` lê a tabela `Files` do MediaStore (imagens e vídeos) duas vezes: ativos (`MATCH_EXCLUDE`) e lixeira (`MATCH_ONLY`). Um `ContentObserver` (com debounce) refaz a leitura quando algo muda; o resultado é um `StateFlow` compartilhado (`WhileSubscribed`).
- Ordenação por `takenMs` (data da foto, ou a de inclusão quando falta). Favoritos e lixeira são os do sistema.
- Apagar, lixeira e favoritar devolvem um `IntentSender` (`MediaStore.create*Request`); a tela abre com `StartIntentSenderForResult`. A caixa de confirmação é do Android.
- Miniaturas e visualização: Coil 3 (`coil-video` para quadros de vídeo, `coil-gif`), encapsulado em `HelixImage` (só o design system conhece o Coil).

## Grade
`buildEntries(items, layout)` produz `GridEntry.Header`/`Media` (Regular sem títulos, Dia, Mês). `MediaGrid` é um `LazyVerticalGrid` com títulos de linha inteira; `GridScrubber` é a rolagem rápida por data (alça na borda direita, balão com mês/ano ao arrastar).

## Visualizador
`HorizontalPager` sobre a lista da origem; foto com zoom por pinça/duplo toque (o pager só rola com zoom em 1×); vídeo com `PlayerView` (ExoPlayer criado só para a página à vista). Detalhes vêm do MediaStore + EXIF (`android.media.ExifInterface`).

## Build e CI
`.github/workflows/ci.yml` (modelo do documento de estrutura): job `ci` em todo push (testes + `assembleRelease` assinado → artefato "build para vistoria"; erro vira comentário no commit) e job `release` (só quando `APP_VERSION` não tem tag e o CHANGELOG tem a seção da versão).
