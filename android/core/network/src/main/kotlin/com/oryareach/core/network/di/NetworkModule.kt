package com.oryareach.core.network.di

import com.oryareach.core.network.SupabaseClientProvider
import com.oryareach.core.network.SupabaseRecordRemoteDataSource
import com.oryareach.core.network.auth.AuthRepository
import com.oryareach.core.network.auth.EncryptedSessionManager
import com.oryareach.core.network.auth.SupabaseAuthRepository
import com.oryareach.core.network.workspace.SupabaseWorkspaceRepository
import com.oryareach.core.network.workspace.WorkspaceRepository
import com.oryareach.core.security.KeystoreBlobStore
import com.oryareach.core.sync.RecordRemoteDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Supplies the current workspace id without this module depending on the session type. */
val workspaceIdQualifier = named("workspaceId")

val networkModule = module {
    single { KeystoreBlobStore(androidContext()) }
    single { EncryptedSessionManager(get()) }
    single<SessionManager> { get<EncryptedSessionManager>() }
    single<SupabaseClient> { SupabaseClientProvider.create(sessions = get()) }

    single<AuthRepository> { SupabaseAuthRepository(client = get(), sessionManager = get()) }
    single<WorkspaceRepository> { SupabaseWorkspaceRepository(get()) }

    single<RecordRemoteDataSource> {
        @Suppress("UNCHECKED_CAST")
        SupabaseRecordRemoteDataSource(
            client = get(),
            workspaceId = get(workspaceIdQualifier) as () -> String?,
        )
    }
}
