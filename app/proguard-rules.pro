# Camera2/CameraX metadata keys are resolved reflectively by the framework in places,
# and our own vendor-tag code paths pass Key objects straight through. Keep them.
-keep class android.hardware.camera2.** { *; }
-keepclassmembers class * extends androidx.camera.core.CameraEffect { *; }
