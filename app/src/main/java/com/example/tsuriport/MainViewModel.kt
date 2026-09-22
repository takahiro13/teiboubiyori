package com.example.tsuriport

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tsuriport.data.Forecast
import com.example.tsuriport.data.Port
import com.example.tsuriport.data.Ports
import com.example.tsuriport.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Loading : UiState
    data class Success(val forecast: Forecast) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _port = MutableStateFlow(
        Ports.byId(app, prefs.getString(KEY_PORT, null)) ?: Ports.default(app),
    )
    val port: StateFlow<Port> = _port.asStateFlow()

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        refresh()
    }

    fun selectPort(port: Port) {
        if (port == _port.value) return
        _port.value = port
        prefs.edit().putString(KEY_PORT, port.id).apply()
        refresh()
    }

    fun refresh() {
        job?.cancel()
        _state.value = UiState.Loading
        job = viewModelScope.launch {
            _state.value = try {
                UiState.Success(WeatherRepository.fetch(_port.value))
            } catch (e: Exception) {
                UiState.Error("データを取得できませんでした。通信状況を確認してください。\n(${e.message})")
            }
        }
    }

    private companion object {
        const val KEY_PORT = "port"
    }
}
