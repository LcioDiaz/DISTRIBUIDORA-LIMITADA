package com.distribuidora.distribuidoralimitada

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.distribuidora.distribuidoralimitada.databinding.ActivityControlTemperaturaBinding
import com.distribuidora.distribuidoralimitada.temperature.TemperatureAlert
import com.distribuidora.distribuidoralimitada.temperature.TemperatureMonitorViewModel
import kotlinx.coroutines.launch

class ControlTemperaturaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlTemperaturaBinding
    private val viewModel: TemperatureMonitorViewModel by viewModels()

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isAlarmActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityControlTemperaturaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        collectUiState()
        collectAlerts()

        binding.btnGuardarRango.setOnClickListener {
            guardarRangosPersonalizados()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    private fun collectUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.loading && state.fahrenheit == null
                    binding.tvTemperaturaFahrenheit.text = state.formattedFahrenheit
                    binding.tvTemperaturaCelsius.text = state.formattedCelsius
                    binding.tvUltimaLectura.text = state.formattedLastUpdated

                    val range = state.range
                    if (range != null) {
                        if (!binding.etMinimo.hasFocus()) {
                            binding.etMinimo.setText(String.format("%.2f", range.minCelsius))
                        }
                        if (!binding.etMaximo.hasFocus()) {
                            binding.etMaximo.setText(String.format("%.2f", range.maxCelsius))
                        }
                    }

                    binding.tvEstado.text = when {
                        state.errorMessage != null -> getString(R.string.control_temp_error_estado)
                        state.fahrenheit == null -> getString(R.string.control_temp_sin_datos)
                        state.isOutOfRange -> getString(R.string.control_temp_fuera_de_rango)
                        else -> getString(R.string.control_temp_en_rango)
                    }

                    if (state.errorMessage != null) {
                        Toast.makeText(
                            this@ControlTemperaturaActivity,
                            state.errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.acknowledgeError()
                    }
                }
            }
        }
    }

    private fun collectAlerts() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.alertEvents.collect { event ->
                    handleAlert(event)
                }
            }
        }
    }

    private fun guardarRangosPersonalizados() {
        val min = binding.etMinimo.text.toString().replace(',', '.').toDoubleOrNull()
        val max = binding.etMaximo.text.toString().replace(',', '.').toDoubleOrNull()

        if (min == null || max == null) {
            Toast.makeText(this, R.string.control_temp_error_valores, Toast.LENGTH_SHORT).show()
            return
        }

        if (min >= max) {
            Toast.makeText(this, R.string.control_temp_error_rango, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.updateRange(min, max)
        Toast.makeText(this, R.string.control_temp_rango_actualizado, Toast.LENGTH_SHORT).show()
    }

    private fun handleAlert(event: TemperatureAlert) {
        if (event.isOutOfRange) {
            startAlarm(event)
        } else {
            stopAlarm()
        }
    }

    private fun startAlarm(event: TemperatureAlert) {
        if (isAlarmActive) return
        isAlarmActive = true

        val alertMessage = buildString {
            append(getString(R.string.control_temp_alerta_base))
            event.temperatureCelsius?.let {
                append("\n")
                append(getString(R.string.control_temp_alerta_valor, String.format("%.2f", it)))
            }
            event.range?.let {
                append("\n")
                append(
                    getString(
                        R.string.control_temp_alerta_rango,
                        String.format("%.2f", it.minCelsius),
                        String.format("%.2f", it.maxCelsius)
                    )
                )
            }
        }

        Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show()

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        mediaPlayer?.release()
        mediaPlayer = try {
            MediaPlayer().apply {
                setDataSource(this@ControlTemperaturaActivity, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (ex: Exception) {
            Toast.makeText(this, getString(R.string.control_temp_error_audio), Toast.LENGTH_SHORT)
                .show()
            null
        }

        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 300),
                    intArrayOf(0, 255, 0),
                    0
                )
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 500, 300), 0)
            }
        }
    }

    private fun stopAlarm() {
        if (!isAlarmActive) return
        isAlarmActive = false

        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
    }
}
