package com.distribuidora.distribuidoralimitada

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.distribuidora.distribuidoralimitada.databinding.ActivityControlTemperaturaBinding
import com.google.firebase.database.FirebaseDatabase

class ControlTemperaturaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControlTemperaturaBinding
    private val database = FirebaseDatabase.getInstance().getReference("registros_temperatura")
    private val LIMITE_TEMPERATURA = -18.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityControlTemperaturaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnGuardarTemperatura.setOnClickListener {
            guardarYVerificarTemperatura()
        }
    }

    private fun guardarYVerificarTemperatura() {
        val tempStr = binding.etTemperatura.text.toString()
        if (tempStr.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese un valor", Toast.LENGTH_SHORT).show()
            return
        }

        val temperatura = tempStr.toDoubleOrNull()
        if (temperatura == null) {
            Toast.makeText(this, "Valor de temperatura inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val registro = mapOf(
            "valor" to temperatura,
            "timestamp" to System.currentTimeMillis()
        )

        val camionId = "camion_01"
        database.child(camionId).push().setValue(registro)
            .addOnSuccessListener {
                Toast.makeText(this, "Temperatura registrada: $temperatura°C", Toast.LENGTH_SHORT).show()
                binding.etTemperatura.text.clear()
                checkAlarma(temperatura)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkAlarma(temperaturaActual: Double) {
        if (temperaturaActual > LIMITE_TEMPERATURA) {
            mostrarAlarmaDeTemperatura(temperaturaActual)
        }
    }

    private fun mostrarAlarmaDeTemperatura(temperaturaActual: Double) {
        AlertDialog.Builder(this)
            .setTitle("¡ALERTA DE TEMPERATURA!")
            .setMessage("La temperatura registrada ($temperaturaActual°C) ha superado el límite de $LIMITE_TEMPERATURA°C. Verifique el sistema de refrigeración.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Entendido", null)
            .show()
    }
}