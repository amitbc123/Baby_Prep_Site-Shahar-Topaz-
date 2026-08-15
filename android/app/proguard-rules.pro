# Keep libsodium's JNA-bound native bindings; reflection-driven, so R8 cannot see the uses.
-keep class com.goterl.lazysodium.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn java.awt.**
