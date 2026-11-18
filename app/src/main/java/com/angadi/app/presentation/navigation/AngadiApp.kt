package com.angadi.app.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.angadi.app.domain.model.Product
import com.angadi.app.presentation.components.ProductActions
import com.angadi.app.presentation.screens.addresses.AddressEditorScreen
import com.angadi.app.presentation.screens.addresses.AddressesScreen
import com.angadi.app.presentation.screens.assistant.AssistantScreen
import com.angadi.app.presentation.screens.basket.BasketScreen
import com.angadi.app.presentation.screens.checkout.CheckoutScreen
import com.angadi.app.presentation.screens.explore.ExploreScreen
import com.angadi.app.presentation.screens.home.HomeCallbacks
import com.angadi.app.presentation.screens.home.HomeScreen
import com.angadi.app.presentation.screens.orders.OrderDetailScreen
import com.angadi.app.presentation.screens.orders.OrdersScreen
import com.angadi.app.presentation.screens.orders.ReorderSummaryDialog
import com.angadi.app.presentation.screens.product.ProductDetailSheet
import com.angadi.app.presentation.screens.profile.ProfileCallbacks
import com.angadi.app.presentation.screens.profile.ProfileEditScreen
import com.angadi.app.presentation.screens.profile.ProfileScreen
import com.angadi.app.presentation.screens.saved.SavedScreen
import com.angadi.app.presentation.viewmodel.AddressViewModel
import com.angadi.app.presentation.viewmodel.AppViewModelFactory
import com.angadi.app.presentation.viewmodel.AssistantViewModel
import com.angadi.app.presentation.viewmodel.CheckoutViewModel
import com.angadi.app.presentation.viewmodel.ExploreViewModel
import com.angadi.app.presentation.viewmodel.OrdersViewModel
import com.angadi.app.presentation.viewmodel.ProfileViewModel
import com.angadi.app.presentation.viewmodel.ReviewsViewModel
import com.angadi.app.presentation.viewmodel.ShopViewModel

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val SAVED = "saved"
    const val BASKET = "basket"
    const val PROFILE = "profile"
    const val ASSISTANT = "assistant"
    const val CHECKOUT = "checkout"
    const val ORDERS = "orders"
    const val ORDER = "order/{id}"
    const val ADDRESSES = "addresses"
    const val ADDRESS_EDIT = "address_edit/{id}"
    const val PROFILE_EDIT = "profile_edit"

    fun order(id: Long) = "order/$id"
    fun addressEdit(id: Long?) = "address_edit/${id ?: 0}"
}

private enum class Tab(val route: String, val label: String, val symbol: String) {
    HOME(Routes.HOME, "Home", "⌂"), EXPLORE(Routes.EXPLORE, "Explore", "⌕"), SAVED(Routes.SAVED, "Saved", "♡"),
    BASKET(Routes.BASKET, "Basket", "▤"), PROFILE(Routes.PROFILE, "You", "○")
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AngadiApp(shopViewModel: ShopViewModel = viewModel(factory = AppViewModelFactory)) {
    val navController = rememberNavController()
    val exploreVm: ExploreViewModel = viewModel(factory = AppViewModelFactory)
    val checkoutVm: CheckoutViewModel = viewModel(factory = AppViewModelFactory)
    val ordersVm: OrdersViewModel = viewModel(factory = AppViewModelFactory)
    val addressVm: AddressViewModel = viewModel(factory = AppViewModelFactory)
    val profileVm: ProfileViewModel = viewModel(factory = AppViewModelFactory)
    val reviewsVm: ReviewsViewModel = viewModel(factory = AppViewModelFactory)
    val assistantVm: AssistantViewModel = viewModel(factory = AppViewModelFactory)

    val shop by shopViewModel.state.collectAsStateWithLifecycle()
    val explore by exploreVm.state.collectAsStateWithLifecycle()
    val checkout by checkoutVm.state.collectAsStateWithLifecycle()
    val orders by ordersVm.orders.collectAsStateWithLifecycle()
    val addresses by addressVm.addresses.collectAsStateWithLifecycle()
    val addressEditor by addressVm.editor.collectAsStateWithLifecycle()
    val profileEditor by profileVm.editor.collectAsStateWithLifecycle()
    val assistant by assistantVm.state.collectAsStateWithLifecycle()
    val reviews by reviewsVm.state.collectAsStateWithLifecycle()
    val selectedProduct by shopViewModel.selectedProduct.collectAsStateWithLifecycle()
    val reorderResult by ordersVm.reorderResult.collectAsStateWithLifecycle()
    val notice by ordersVm.notice.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { shopViewModel.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); ordersVm.consumeNotice() } }
    LaunchedEffect(Unit) {
        checkoutVm.orderPlaced.collect { id ->
            navController.navigate(Routes.order(id)) { popUpTo(Routes.HOME) }
            snackbar.showSnackbar("Order placed! Payment is demo-only in this build.")
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    val actions = remember(shop.favorites, shop.ratingOverrides) {
        ProductActions(
            favorites = shop.favorites, ratingOverrides = shop.ratingOverrides,
            onOpen = { shopViewModel.openProduct(it) }, onAdd = { shopViewModel.addToCart(it) }, onToggleFavorite = { shopViewModel.toggleFavorite(it) },
        )
    }
    val openAssistant = { navController.navigate(Routes.ASSISTANT) { launchSingleTop = true } }
    val openBasket = { navController.openTab(Routes.BASKET) }
    val defaultAddress = checkout.addresses.firstOrNull { it.isDefault } ?: checkout.addresses.firstOrNull()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, windowInsets = WindowInsets.navigationBars) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.openTab(tab.route) },
                        modifier = Modifier.testTag("tab_${tab.name}"),
                        icon = {
                            Box {
                                Text(tab.symbol, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                                if (tab == Tab.BASKET && shop.cartCount > 0) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-5).dp).size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(shop.cartCount.coerceAtMost(99).toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }
                                }
                            }
                        },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { insets ->
        NavHost(
            navController = navController, startDestination = Routes.HOME, modifier = Modifier.padding(insets),
            enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(130)) },
            popEnterTransition = { fadeIn(tween(180)) }, popExitTransition = { fadeOut(tween(130)) },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = shop, categories = explore.categories, defaultAddressLine = defaultAddress?.let { "${it.label.display} · ${it.singleLine}" }, actions = actions,
                    callbacks = HomeCallbacks(
                        onSearch = { navController.openTab(Routes.EXPLORE) },
                        onCategory = { exploreVm.setCategory(it); navController.openTab(Routes.EXPLORE) },
                        onExplore = { navController.openTab(Routes.EXPLORE) },
                        onAssistant = { openAssistant() }, onSaved = { navController.openTab(Routes.SAVED) }, onBasket = { openBasket() },
                        onAddresses = { navController.navigate(Routes.ADDRESSES) }, onClearRecent = shopViewModel::clearRecentlyViewed,
                        onCoupons = { navController.openTab(Routes.BASKET) },
                    ),
                )
            }
            composable(Routes.EXPLORE) { ExploreScreen(explore, exploreVm, shop.cartCount, actions, { openAssistant() }, { openBasket() }) }
            composable(Routes.SAVED) { SavedScreen(shop.favoriteProducts, shop.cartCount, actions, { openAssistant() }, { openBasket() }, { navController.openTab(Routes.EXPLORE) }) }
            composable(Routes.BASKET) {
                BasketScreen(
                    lines = shop.cartLines, breakdown = shop.breakdown, cartCount = shop.cartCount, onAssistant = { openAssistant() }, onBasket = {},
                    onBrowse = { navController.openTab(Routes.EXPLORE) },
                    onChangeQuantity = { line, delta -> shopViewModel.changeQuantity(line.product, delta) },
                    onRemove = { shopViewModel.removeFromCart(it.product.id) },
                    onCheckout = { checkoutVm.refreshSlots(); navController.navigate(Routes.CHECKOUT) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    state = shop, orderCount = orders?.size ?: 0, assistant = assistant,
                    callbacks = ProfileCallbacks(
                        onAssistant = { openAssistant() }, onBasket = { openBasket() },
                        onEditProfile = { profileVm.startEditing(); navController.navigate(Routes.PROFILE_EDIT) },
                        onOrders = { navController.navigate(Routes.ORDERS) }, onAddresses = { navController.navigate(Routes.ADDRESSES) },
                        onTheme = shopViewModel::setThemeMode, onRecentLimit = shopViewModel::setRecentLimit, onClearRecent = shopViewModel::clearRecentlyViewed,
                        onServerDraft = assistantVm::setServerDraft, onSaveServer = {
                            assistantVm.saveServer()
                            shopViewModel.showMessage("Assistant server address saved")
                        }, onTestServer = assistantVm::testServer,
                    ),
                )
            }
            composable(Routes.PROFILE_EDIT) {
                ProfileEditScreen(profileEditor, profileVm, onBack = { navController.popBackStack() }, onSaved = {
                    navController.popBackStack()
                    shopViewModel.showMessage("Profile saved")
                })
            }
            composable(Routes.ASSISTANT) { AssistantScreen(assistant, { navController.popBackStack() }, assistantVm::setInput, assistantVm::send) }
            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    checkout, checkoutVm, onBack = { navController.popBackStack() },
                    onManageAddresses = { navController.navigate(Routes.ADDRESSES) },
                    onAddAddress = { addressVm.startEditing(null, shop.profile.name, shop.profile.phone); navController.navigate(Routes.addressEdit(null)) },
                )
            }
            composable(Routes.ORDERS) {
                OrdersScreen(orders, { navController.popBackStack() }, { navController.navigate(Routes.order(it)) }, ordersVm::reorder, { navController.openTab(Routes.EXPLORE) })
            }
            composable(Routes.ORDER, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val order by remember(id) { ordersVm.order(id) }.collectAsStateWithLifecycle(initialValue = null)
                OrderDetailScreen(order, loading = orders == null, onBack = { navController.popBackStack() }, onCancel = ordersVm::cancel, onSimulateNext = ordersVm::simulateNext, onReorder = ordersVm::reorder)
            }
            composable(Routes.ADDRESSES) {
                AddressesScreen(
                    addresses, onBack = { navController.popBackStack() },
                    onAdd = { addressVm.startEditing(null, shop.profile.name, shop.profile.phone); navController.navigate(Routes.addressEdit(null)) },
                    onEdit = { addressVm.startEditing(it); navController.navigate(Routes.addressEdit(it)) },
                    onDelete = addressVm::delete, onSetDefault = addressVm::setDefault,
                )
            }
            composable(Routes.ADDRESS_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                AddressEditorScreen(addressEditor, onUpdate = addressVm::update, onSave = addressVm::save, onSaved = {
                    navController.popBackStack()
                    shopViewModel.showMessage("Address saved")
                }, onBack = { navController.popBackStack() })
            }
        }
    }

    selectedProduct?.let { product -> ProductSheetHost(product, shop.favorites, reviews, shopViewModel, reviewsVm) }
    reorderResult?.let { result ->
        ReorderSummaryDialog(result, onDismiss = ordersVm::dismissReorder, onViewBasket = { ordersVm.dismissReorder(); navController.openTab(Routes.BASKET) })
    }
}

@Composable
private fun ProductSheetHost(
    product: Product,
    favorites: Set<Int>,
    reviews: com.angadi.app.presentation.viewmodel.ReviewsUiState,
    shopViewModel: ShopViewModel,
    reviewsVm: ReviewsViewModel,
) {
    LaunchedEffect(product.id) { reviewsVm.show(product.id) }
    ProductDetailSheet(
        product = product, isFavorite = product.id in favorites, reviews = reviews,
        onClose = shopViewModel::closeProduct, onToggleFavorite = { shopViewModel.toggleFavorite(product) },
        onAdd = { qty -> shopViewModel.addToCart(product, qty) },
        onSubmitReview = { rating, body, done -> reviewsVm.submit(rating, body, done) }, onDeleteReview = reviewsVm::delete,
    )
}
