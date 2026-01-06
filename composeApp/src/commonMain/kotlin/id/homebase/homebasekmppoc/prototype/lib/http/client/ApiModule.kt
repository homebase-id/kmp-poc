package id.homebase.homebasekmppoc.prototype.lib.http.client

import org.koin.dsl.module

val apiModule = module {
    single { HttpClientProvider.create() }
    single { ::CredentialsManager }
    single { ::AuthRepository }
    single { ::ApiService }
}

