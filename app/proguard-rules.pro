# Suprimir advertencias de bibliotecas SSL y Conscrypt/OpenJSSE
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Mantener Retrofit interfaces y sus métodos
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }

# Mantener clases de modelo y anotaciones JSON para serialización/deserialización con Gson
-keep class com.enlacedigital.CoordiApp.models.** { *; }

# Mantener métodos con anotaciones de Retrofit y Gson SerializedName
-keepclassmembers,allowobfuscation class * {
    @retrofit2.http.* <methods>;
    @com.google.gson.annotations.SerializedName <fields>;
}

# Mantener atributos de anotación para Retrofit y Gson
-keepattributes *Annotation*

# Evitar ofuscación en PreferencesHelper (SharedPreferences) y clases específicas de configuración de ubicación
-keep class com.enlacedigital.CoordiApp.singleton.** { *; }
-keepclassmembers class **.LocationRequest { *; }
-keep class com.enlacedigital.CoordiApp.LocationHelper { *; }
-keepclassmembers class **.SettingsClient { *; }

# Mantener clases esenciales en CoordiApp que interactúan con Retrofit y Gson
-keep class com.enlacedigital.CoordiApp.Registrar.RegistrandoFragmentValidar { *; }
-keep class com.enlacedigital.CoordiApp.Registrar.Checking { *; }

# Evitar advertencias en Retrofit, Gson y OkHttp3
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn com.google.gson.**

# Generación de reporte de uso
-printusage usage.txt