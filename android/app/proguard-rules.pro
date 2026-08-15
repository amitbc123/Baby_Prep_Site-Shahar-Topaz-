# Bouncy Castle ships JCE provider classes that reference desktop-JDK APIs absent on Android.
# We only use the lightweight org.bouncycastle.crypto.* API, so the rest can be stripped.
-dontwarn javax.naming.**
-dontwarn org.bouncycastle.jce.provider.**
