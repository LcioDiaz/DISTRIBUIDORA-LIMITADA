package com.distribuidora.distribuidoralimitada.temperature

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TemperatureMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val defaultRange = TemperatureRange(DEFAULT_MIN_C, DEFAULT_MAX_C)
    private val firebaseRef = FirebaseDatabase.getInstance().getReference("temperatura")

    private val _uiState = MutableStateFlow(
        TemperatureMonitorUiState(
            range = loadPersistedRange(),
            loading = true
        )
    )
    val uiState: StateFlow<TemperatureMonitorUiState> = _uiState.asStateFlow()

    private val _alertEvents = MutableSharedFlow<TemperatureAlert>(extraBufferCapacity = 1)
    val alertEvents: SharedFlow<TemperatureAlert> = _alertEvents.asSharedFlow()

    private var lastAlertState: Boolean? = null

    private val firebaseListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val fahrenheit = snapshot.getValue(Double::class.java)
                ?: snapshot.getValue(Long::class.java)?.toDouble()
                ?: snapshot.getValue(String::class.java)?.toDoubleOrNull()

            _uiState.update { current ->
                val celsius = fahrenheit?.let { (it - 32.0) * 5.0 / 9.0 }
                val range = current.range
                val outOfRange = isOutOfRange(celsius, range)
                current.copy(
                    fahrenheit = fahrenheit,
                    celsius = celsius,
                    lastUpdatedMillis = System.currentTimeMillis(),
                    isOutOfRange = outOfRange,
                    loading = false,
                    errorMessage = null
                )
            }
            dispatchAlertIfNeeded()
        }

        override fun onCancelled(error: DatabaseError) {
            _uiState.update { current ->
                current.copy(
                    loading = false,
                    errorMessage = error.message
                )
            }
        }
    }

    init {
        firebaseRef.addValueEventListener(firebaseListener)
    }

    override fun onCleared() {
        super.onCleared()
        firebaseRef.removeEventListener(firebaseListener)
    }

    fun updateRange(minCelsius: Double, maxCelsius: Double) {
        val newRange = TemperatureRange(minCelsius, maxCelsius)
        persistRange(newRange)
        _uiState.update { current ->
            val outOfRange = isOutOfRange(current.celsius, newRange)
            current.copy(range = newRange, isOutOfRange = outOfRange)
        }
        dispatchAlertIfNeeded()
    }

    private fun dispatchAlertIfNeeded() {
        val state = _uiState.value
        val currentAlertState = state.isOutOfRange
        if (currentAlertState != lastAlertState) {
            lastAlertState = currentAlertState
            viewModelScope.launch {
                _alertEvents.emit(
                    TemperatureAlert(
                        isOutOfRange = currentAlertState,
                        temperatureCelsius = state.celsius,
                        temperatureFahrenheit = state.fahrenheit,
                        range = state.range
                    )
                )
            }
        }
    }

    fun acknowledgeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadPersistedRange(): TemperatureRange {
        val minPersisted =
            sharedPreferences.getString(KEY_MIN_C, null)?.toDoubleOrNull()
        val maxPersisted =
            sharedPreferences.getString(KEY_MAX_C, null)?.toDoubleOrNull()

        return if (minPersisted != null && maxPersisted != null && minPersisted < maxPersisted) {
            TemperatureRange(minPersisted, maxPersisted)
        } else {
            persistRange(defaultRange)
            defaultRange
        }
    }

    private fun persistRange(range: TemperatureRange) {
        sharedPreferences.edit()
            .putString(KEY_MIN_C, range.minCelsius.toString())
            .putString(KEY_MAX_C, range.maxCelsius.toString())
            .apply()
    }

    private fun isOutOfRange(celsius: Double?, range: TemperatureRange?): Boolean {
        return if (celsius != null && range != null) {
            celsius < range.minCelsius || celsius > range.maxCelsius
        } else {
            false
        }
    }

    companion object {
        private const val PREFS_NAME = "temperature_range_prefs"
        private const val KEY_MIN_C = "min_celsius"
        private const val KEY_MAX_C = "max_celsius"
        private const val DEFAULT_MIN_C = -10.0
        private const val DEFAULT_MAX_C = 10.0
    }
}

// --- UI State & domain models ---

data class TemperatureMonitorUiState(
    val fahrenheit: Double? = null,
    val celsius: Double? = null,
    val lastUpdatedMillis: Long? = null,
    val range: TemperatureRange? = null,
    val isOutOfRange: Boolean = false,
    val loading: Boolean = true,
    val errorMessage: String? = null
) {
    val formattedFahrenheit: String
        get() = fahrenheit?.let { String.format("%.2f °F", it) } ?: "--"

    val formattedCelsius: String
        get() = celsius?.let { String.format("%.2f °C", it) } ?: "--"

    val formattedLastUpdated: String
        get() = lastUpdatedMillis?.let { millis ->
            val seconds = (System.currentTimeMillis() - millis) / 1000
            when {
                seconds < 5 -> "Actualizado hace un instante"
                seconds < 60 -> "Actualizado hace $seconds s"
                else -> "Actualizado hace ${seconds / 60} min"
            }
        } ?: "Sin lecturas"
}

data class TemperatureRange(
    val minCelsius: Double,
    val maxCelsius: Double
)

data class TemperatureAlert(
    val isOutOfRange: Boolean,
    val temperatureCelsius: Double?,
    val temperatureFahrenheit: Double?,
    val range: TemperatureRange?,
)