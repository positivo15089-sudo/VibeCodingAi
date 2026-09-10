# VibeCoding AI

Aplicativo Android nativo em Kotlin + Jetpack Compose para criação e evolução de projetos Android por conversa, sem exigir chave de API.

## O que funciona nesta versão

- Projetos isolados e persistidos com Room.
- Chat por projeto e comandos incrementais sobre o mesmo conjunto de arquivos.
- Motor `AiEngine` substituível; implementação inicial `RuleBasedLocalEngine` funciona offline e gera estrutura Android real a partir de templates.
- Editor de código, árvore/lista de arquivos, anexos, histórico e snapshots restauráveis.
- Exportação ZIP e importação ZIP com validação contra path traversal.
- Modo de prévia estrutural honesto, sem fingir execução de bytecode não compilado.
- Aba BUILD informa quando a toolchain não está disponível no Android.
- GitHub Actions compila e testa o próprio VibeCoding AI e publica `app-debug.apk` como Artifact.

## Limite intencional

O aplicativo não embute OpenAI/Gemini/Claude nem qualquer segredo no APK. O motor local leve não é um LLM completo; a arquitetura permite adicionar um motor on-device open-source futuramente. Compilar projetos Android arbitrários dentro do app exige uma toolchain isolada/serviço controlado e não é simulado.

## Build local

Requisitos: JDK 17, Android SDK 35, Build Tools 35.0.0 e Gradle 8.13.

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

APK esperado: `app/build/outputs/apk/debug/app-debug.apk`.

## Build no GitHub

Envie este repositório ao GitHub. O workflow `.github/workflows/build-apk.yml` roda automaticamente em push para `main`/`master` e também manualmente via **Actions > Build APK > Run workflow**. O APK fica em **Artifacts > VibeCodingAI-debug-apk**.
