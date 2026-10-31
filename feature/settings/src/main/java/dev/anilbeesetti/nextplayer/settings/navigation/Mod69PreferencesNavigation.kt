package dev.anilbeesetti.nextplayer.settings.navigation

import androidx.compose.runtime.SideEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.settings.screens.mod69.Mod69PreferencesScreen
import dev.anilbeesetti.nextplayer.settings.screens.mod69.Mod69PreferencesViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
object Mod69PreferencesRoute : NavKey

fun NavBackStack<NavKey>.navigateToMod69Preferences() {
    add(element = Mod69PreferencesRoute)
}

fun EntryProviderScope<NavKey>.mod69PreferencesEntry(onNavigateUp: () -> Unit) {
    entry<Mod69PreferencesRoute> {
        val output = Mod69PreferencesViewModel.Output(navigateUp = onNavigateUp)
        val viewModel = koinViewModel<Mod69PreferencesViewModel>(parameters = { parametersOf(output) })
        SideEffect { viewModel.output = output }
        Mod69PreferencesScreen(viewModel = viewModel)
    }
}
