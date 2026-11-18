package com.angadi.app.presentation.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.angadi.app.AngadiApplication
import com.angadi.app.AppContainer

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as AngadiApplication).container

/** Builds every ViewModel from the [AppContainer] so repositories are shared singletons. */
val AppViewModelFactory = viewModelFactory {
    initializer { container().let { ShopViewModel(it.shop, it.settings, it.profile, it.reviews) } }
    initializer { ExploreViewModel(container().shop) }
    initializer { container().let { CheckoutViewModel(it.shop, it.orders, it.addresses) } }
    initializer { OrdersViewModel(container().orders) }
    initializer { AddressViewModel(container().addresses) }
    initializer { ProfileViewModel(container().profile) }
    initializer { container().let { ReviewsViewModel(it.reviews, it.profile) } }
    initializer { container().let { AssistantViewModel(it.assistant, it.settings, it.shop) } }
}
