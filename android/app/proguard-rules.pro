# Keep Kotlinx Serialization models for offline KB parsing and AI proxy JSON payloads
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aegis.survival.data.**$$serializer { *; }
-keepclassmembers class com.aegis.survival.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.aegis.survival.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aegis.survival.update.**$$serializer { *; }
-keepclassmembers class com.aegis.survival.update.** {
    *** Companion;
}
-keepclasseswithmembers class com.aegis.survival.update.** {
    kotlinx.serialization.KSerializer serializer(...);
}
