# OneShot Professional Camera - R8 / ProGuard Optimization & Obfuscation Rules

# 1. Camera2 / CameraX Metadata & Vendor Extensions
# Keep Camera2 metadata keys and CameraX interop reflection objects
-keep class android.hardware.camera2.** { *; }
-keep class androidx.camera.camera2.interop.** { *; }
-keep class androidx.camera.core.** { *; }
-keepclassmembers class * extends androidx.camera.core.CameraEffect { *; }
-keepclassmembers class * implements androidx.camera.core.SurfaceProcessor { *; }

# 2. OpenGL ES 3.0 Texture & Shader Processors
# Prevent stripping of GL renderer methods invoked across EGL threads
-keep class dev.oneshot.camera.gl.** { *; }

# 3. Motion Photo Container & XMP Metadata
# Preserve XMP constants and Motion Photo byte alignment logic
-keep class dev.oneshot.camera.MotionPhotoWriter { *; }
-keep class dev.oneshot.camera.Mp4RecoveryInspector { *; }

# 4. Settings DataStore & State Models
-keep class dev.oneshot.data.** { *; }

# 5. Native & Coroutines reflection safety
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn java.lang.invoke.MethodHandle
-dontwarn java.lang.invoke.MethodHandles$Lookup
