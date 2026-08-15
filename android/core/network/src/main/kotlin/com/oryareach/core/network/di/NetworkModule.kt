package com.oryareach.core.network.di

import com.oryareach.core.network.SupabaseClientProvider
import com.oryareach.core.network.SupabaseRecordRemoteDataSource
import com.oryareach.core.sync.RecordRemoteDataSource
import io.github.jan.supabase.SupabaseClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Supplies the current workspace id. Declared here so `:app` can bind it without this module
 * depending on the session type, and so `SupabaseClient` never leaks out of `:core:network`.
 */
val workspaceIdQualifier = named("workspaceId")

val networkModule = module {
    single<SupabaseClient> { SupabaseClientProvider.create() }

    single<RecordRemoteDataSource> {
        @Suppress("UNCHECKED_CAST")
        SupabaseRecordRemoteDataSource(
            client = get(),
            workspaceId = get(workspaceIdQualifier) as () -> String?,
        )
    }
}
