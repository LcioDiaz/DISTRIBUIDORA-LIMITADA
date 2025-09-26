package com.distribuidora.distribuidoralimitada

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
import com.distribuidora.distribuidoralimitada.databinding.ActivityTransportistaHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TransportistaHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransportistaHomeBinding
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransportistaHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupButtons()
        observeAuth()
        loadUserName()

        // ¡NUEVO! Llama a una función para empezar a rastrear la ubicación
        startLocationTracking()
    }

    private fun startLocationTracking() {
        // 1. Aquí debes comprobar y solicitar los permisos de ubicación.

        // 2. Si los permisos están concedidos, obtén la ubicación.
        // CUIDADO: Esto requiere manejo de permisos y comprobaciones de seguridad.
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // 3. Si obtienes la ubicación, guárdala en Firebase.
                    saveLocationToDatabase(location.latitude, location.longitude)
                }
            }
    }

    private fun saveLocationToDatabase(latitude: Double, longitude: Double) {
        val uid = authViewModel.user.value?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("ubicaciones_transportistas").child(uid)

        val locationData = mapOf(
            "lat" to latitude,
            "lng" to longitude,
            "timestamp" to System.currentTimeMillis()
        )

        // Usa push() para crear un historial de ubicaciones
        dbRef.push().setValue(locationData)
            .addOnSuccessListener {
                Toast.makeText(this, "Ubicación guardada", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtons() {
        binding.btnGoToControlTemperatura.setOnClickListener {
            startActivity(Intent(this, ControlTemperaturaActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
        }
    }

    private fun observeAuth() {
        authViewModel.user.observe(this) { user ->
            if (user == null) {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun loadUserName() {
        val uid = authViewModel.user.value?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java)
                if (nombre != null) {
                    binding.tvWelcome.text = "Bienvenido, $nombre"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Manejar error si es necesario
            }
        })
    }
}