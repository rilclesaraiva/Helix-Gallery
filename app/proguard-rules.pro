# Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
# Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

# Regras do R8 (encolher e otimizar o APK de release). As bibliotecas
# (Compose, Media3, Coil, DataStore…) já trazem as regras delas.

# Os nomes do código não são embaralhados: o APK encolhe e fica otimizado do
# mesmo jeito, mas um erro no celular mostra classe e linha de verdade.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# ML Kit (Copiar texto): ele monta os componentes internos por reflexão, lendo os registradores
# (ComponentRegistrar) declarados no manifesto. Sem estas regras o R8 do release remove essas classes e
# TextRecognition.getClient falha com NullPointerException (visto no aparelho).
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }
