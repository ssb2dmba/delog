/**
 * Delog
 * Copyright (C) 2023 dmba.info
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package `in`.delog.di.modules

import `in`.delog.db.AppDatabase
import `in`.delog.repository.*
import `in`.delog.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val mainViewModel = module {

    single {
        val database = get<AppDatabase>()
        database.identDao()
    }

    single { FeedRepositoryImpl(get(),get()) }
    factory<IdentRepository> { (FeedRepositoryImpl(get(), get())) }
    single { IdentListViewModel(get(),get()) }
    viewModel { IdentAndAboutViewModel(get(), get(), get(), get()) }
    single { MessageRepositoryImpl(get()) }
    factory<MessageRepository> { (MessageRepositoryImpl(get())) }
    factory<MessageTreeRepository> { (MessageTreeRepositoryImpl(get())) }
    viewModel { MessageListViewModel(get(), get(), get(),get()) }

    factory<DraftRepository> { (DraftRepositoryImpl(get())) }
    viewModel { DraftListViewModel(get(), get()) }
    viewModel { ContactListViewModel(get(), get()) }

    single { ContactRepositoryImpl(get()) }
    factory<ContactRepository> { (ContactRepositoryImpl(get())) }

    single { AboutRepositoryImpl(get()) }
    factory<AboutRepository> { (AboutRepositoryImpl(get())) }

    viewModel { DraftViewModel(get(), get(), get()) }
    single { BottomBarViewModel() }
}
