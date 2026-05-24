# Add project specific ProGuard rules here.
-keep class com.gaitguard.app.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Gson / JSON baseline
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
