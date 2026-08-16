# SQLCipher loads its native library and support-helper classes reflectively, so R8 cannot
# see the uses and would strip them.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }
