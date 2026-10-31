package dev.anilbeesetti.nextplayer.settings.screens.mod69

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.ui.base.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class Mod69PreferencesViewModel(
    private val preferencesRepository: PreferencesRepository,
    @InjectedParam internal var output: Output,
) : MviViewModel<Mod69PreferencesUiState, Mod69PreferencesUiEvent>() {

    data class Output(val navigateUp: () -> Unit)

    private val stateInternal = MutableStateFlow(
        Mod69PreferencesUiState(
            player = preferencesRepository.playerPreferences.value,
            app = preferencesRepository.applicationPreferences.value,
        ),
    )
    override val state: StateFlow<Mod69PreferencesUiState> = stateInternal.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.playerPreferences.collect { p -> stateInternal.update { it.copy(player = p) } }
        }
        viewModelScope.launch {
            preferencesRepository.applicationPreferences.collect { a -> stateInternal.update { it.copy(app = a) } }
        }
    }

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** All settings (app + player + 69 audio/subtitle look) as one JSON text. */
    fun exportSettings(context: android.content.Context): String {
        val o = org.json.JSONObject()
        o.put("format", "69-next-player-settings-1")
        o.put("player", json.encodeToString(PlayerPreferences.serializer(), preferencesRepository.playerPreferences.value))
        o.put("app", json.encodeToString(ApplicationPreferences.serializer(), preferencesRepository.applicationPreferences.value))
        for (name in listOf("mod69_audio", "mod69_subs")) {
            val sp = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
            val m = org.json.JSONObject()
            sp.all.forEach { (k, v) -> if (v is String) m.put(k, v) }
            o.put(name, m)
        }
        return o.toString(2)
    }

    /** @return true when the file was a settings export and was applied. */
    fun importSettings(context: android.content.Context, text: String): Boolean {
        val o = runCatching { org.json.JSONObject(text) }.getOrNull() ?: return false
        if (o.optString("format") != "69-next-player-settings-1") return false
        val player = runCatching { json.decodeFromString(PlayerPreferences.serializer(), o.getString("player")) }.getOrNull()
        val app = runCatching { json.decodeFromString(ApplicationPreferences.serializer(), o.getString("app")) }.getOrNull()
        viewModelScope.launch {
            player?.let { p -> preferencesRepository.updatePlayerPreferences { p } }
            app?.let { a -> preferencesRepository.updateApplicationPreferences { a } }
        }
        for (name in listOf("mod69_audio", "mod69_subs")) {
            val m = o.optJSONObject(name) ?: continue
            val e = context.getSharedPreferences(name, android.content.Context.MODE_PRIVATE).edit()
            m.keys().forEach { k -> e.putString(k, m.optString(k)) }
            e.apply()
        }
        return player != null || app != null
    }

    override fun onAction(action: Mod69PreferencesUiEvent) {
        when (action) {
            Mod69PreferencesUiEvent.NavigateUp -> output.navigateUp()
            is Mod69PreferencesUiEvent.UpdatePlayer -> viewModelScope.launch {
                preferencesRepository.updatePlayerPreferences { action.transform(it) }
            }
            is Mod69PreferencesUiEvent.UpdateApp -> viewModelScope.launch {
                preferencesRepository.updateApplicationPreferences { action.transform(it) }
            }
        }
    }
}

@Stable
data class Mod69PreferencesUiState(
    val player: PlayerPreferences = PlayerPreferences(),
    val app: ApplicationPreferences = ApplicationPreferences(),
)

sealed interface Mod69PreferencesUiEvent {
    data object NavigateUp : Mod69PreferencesUiEvent
    data class UpdatePlayer(val transform: (PlayerPreferences) -> PlayerPreferences) : Mod69PreferencesUiEvent
    data class UpdateApp(val transform: (ApplicationPreferences) -> ApplicationPreferences) : Mod69PreferencesUiEvent
}
