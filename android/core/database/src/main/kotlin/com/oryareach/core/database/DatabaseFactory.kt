package com.oryareach.core.database

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Builds the encrypted local database.
 *
 * Everything the app stores locally — tasks, cycle records, notes — sits inside this file,
 * so it is encrypted at rest with SQLCipher rather than relying on the app sandbox alone.
 * A rooted device, an offline disk image or an ADB backup yields ciphertext.
 */
object DatabaseFactory {

    fun create(context: Context, passphrase: DatabasePassphrase): OrYareachDatabase {
        System.loadLibrary("sqlcipher")

        val key = passphrase.get()
        // SupportOpenHelperFactory keeps a reference to the array, so it cannot be zeroed
        // here; SQLCipher wipes it once the database is opened.
        val factory = SupportOpenHelperFactory(key)

        return Room.databaseBuilder(context, OrYareachDatabase::class.java, OrYareachDatabase.NAME)
            .openHelperFactory(factory)
            // No fallbackToDestructiveMigration: losing local data on a schema change would
            // discard anything not yet synced. A missing migration must fail loudly instead.
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
