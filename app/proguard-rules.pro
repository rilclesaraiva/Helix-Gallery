# Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
# Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

# Regras do R8 (encolher e otimizar o APK de release). As bibliotecas
# (Compose, Media3, Coil, DataStore…) já trazem as regras delas.

# Os nomes do código não são embaralhados: o APK encolhe e fica otimizado do
# mesmo jeito, mas um erro no celular mostra classe e linha de verdade.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
