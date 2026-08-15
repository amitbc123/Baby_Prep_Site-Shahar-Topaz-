package com.oryareach.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds shopping items and important dates — Phase 3's port of the web app's remaining data. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `shopping_items` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `estimated_price` INTEGER,
                `actual_price` INTEGER,
                `priority` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `assignee` TEXT,
                `note` TEXT,
                `link` TEXT,
                `alternatives` TEXT NOT NULL,
                `chosen_alternative_id` TEXT,
                `workspace_id` TEXT NOT NULL,
                `created_by` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `deleted_at` INTEGER,
                `version` INTEGER NOT NULL,
                `sync_status` TEXT NOT NULL,
                `client_mutation_id` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_items_sync_status` ON `shopping_items` (`sync_status`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_shopping_items_workspace_id_updated_at` " +
                "ON `shopping_items` (`workspace_id`, `updated_at`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_shopping_items_workspace_id_category` " +
                "ON `shopping_items` (`workspace_id`, `category`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `important_dates` (
                `id` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `wish` TEXT,
                `workspace_id` TEXT NOT NULL,
                `created_by` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `deleted_at` INTEGER,
                `version` INTEGER NOT NULL,
                `sync_status` TEXT NOT NULL,
                `client_mutation_id` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_important_dates_sync_status` ON `important_dates` (`sync_status`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_important_dates_workspace_id_updated_at` " +
                "ON `important_dates` (`workspace_id`, `updated_at`)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_important_dates_date` ON `important_dates` (`date`)")
    }
}

/** Adds `app_settings` — the couple's shared due date and baby name, for the home dashboard. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `app_settings` (
                `id` TEXT NOT NULL,
                `dueDate` TEXT NOT NULL,
                `babyName` TEXT,
                `workspace_id` TEXT NOT NULL,
                `created_by` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `deleted_at` INTEGER,
                `version` INTEGER NOT NULL,
                `sync_status` TEXT NOT NULL,
                `client_mutation_id` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_app_settings_sync_status` ON `app_settings` (`sync_status`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_app_settings_workspace_id_updated_at` " +
                "ON `app_settings` (`workspace_id`, `updated_at`)",
        )
    }
}
