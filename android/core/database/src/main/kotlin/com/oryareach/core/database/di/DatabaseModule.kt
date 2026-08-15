package com.oryareach.core.database.di

import com.oryareach.core.database.DatabaseFactory
import com.oryareach.core.database.OrYareachDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { DatabaseFactory.create(androidContext(), get()) }
    single { get<OrYareachDatabase>().taskDao() }
    single { get<OrYareachDatabase>().menstrualCycleDao() }
    single { get<OrYareachDatabase>().syncOperationDao() }
}
