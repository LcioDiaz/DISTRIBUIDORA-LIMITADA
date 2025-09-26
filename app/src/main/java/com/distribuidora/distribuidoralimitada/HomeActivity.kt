package com.distribuidora.distribuidoralimitada

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
// 1. Importamos nuestro FirebaseClient
import com.distribuidora.distribuidoralimitada.auth.FirebaseClient
import com.distribuidora.distribuidoralimitada.databinding.ActivityHomeBinding
import com.distribuidora.distribuidoralimitada.model.Product
import com.distribuidora.distribuidoralimitada.ui.ProductAdapter
import com.distribuidora.distribuidoralimitada.util.Haversine
import com.distribuidora.distribuidoralimitada.util.ReglasEnvio
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class HomeActivity : AppCompatActivity() {


    private lateinit var b: ActivityHomeBinding
    private val authVM: AuthViewModel by viewModels()
    private val reglasEnvio = ReglasEnvio()
    // Hacemos que la instancia de auth también use el FirebaseClient por consistencia
    private val auth = FirebaseClient.auth

    private val products = listOf(
        Product(1, "Arroz 1kg", 1200, false),
        Product(2, "Leche 1L", 1100, false),
        Product(3, "Carne congelada 1kg", 8500, true),
        Product(4, "Mariscos congelados 1kg", 9900, true),
        Product(5, "Aceite 1L", 2500, false)
    )
    private val qty = MutableList(products.size) { 0 }

    private var clientLat: Double? = null
    private var clientLng: Double? = null

    private val locationPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocation()
        } else {
            Toast.makeText(this, "El permiso de ubicación es necesario para calcular el envío.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupAuthObserver()
        setupUI()
        updateTotals()
    }

    private fun setupAuthObserver() {
        authVM.user.observe(this) { user ->
            if (user == null) {
                val intent = Intent(this, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun setupUI() {
        b.rvProducts.layoutManager = LinearLayoutManager(this)
        b.rvProducts.adapter = ProductAdapter(products, qty) { updateTotals() }

        b.btnLogout.setOnClickListener { authVM.logout() }
        b.btnCalcDistance.setOnClickListener { requestLocationPermission() }
    }

    private fun requestLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            fetchLocation()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle("Permiso de Ubicación Necesario")
                    .setMessage("Para calcular el costo de envío, necesitamos acceder a tu ubicación.")
                    .setPositiveButton("Entendido") { _, _ ->
                        locationPerms.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                    .show()
            } else {
                locationPerms.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        Toast.makeText(this, "Obteniendo ubicación...", Toast.LENGTH_SHORT).show()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    updateClientLocation(location.latitude, location.longitude)
                    saveLocationToFirebase(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación. Asegúrate de que el GPS esté activado.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener ubicación: ${e.message}", Toast.LENGTH_LONG).show()
            }

        b.root.postDelayed({ cancellationTokenSource.cancel() }, 10_000)
    }

    private fun saveLocationToFirebase(latitude: Double, longitude: Double) {
        val user = auth.currentUser ?: return

        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "lastUpdated" to ServerValue.TIMESTAMP
        )

        //  referencia a FirebaseClient
        FirebaseClient.usersRef.child(user.uid).child("location")
            .setValue(locationData)
            .addOnSuccessListener {
                Log.d("FirebaseDB", "Ubicación guardada para el usuario ${user.uid}")
                Toast.makeText(this, "Ubicación actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseDB", "Error al guardar la ubicación", e)
                Toast.makeText(this, "No se pudo guardar la ubicación en la base de datos.", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateClientLocation(lat: Double, lng: Double) {
        clientLat = lat
        clientLng = lng
        updateTotals()
    }

    private fun updateTotals() {
        val subtotal = products.indices.sumOf { products[it].price * qty[it] }.toDouble()
        b.tvSubtotal.text = "Subtotal: $${subtotal.toInt()}"

        var total = subtotal

        if (clientLat != null && clientLng != null) {
            val shippingCost = reglasEnvio.calcularCostoEnvio(subtotal, clientLat!!, clientLng!!)
            val distancia = Haversine.distanceKm(
                reglasEnvio.latitudTienda, reglasEnvio.longitudTienda, clientLat!!, clientLng!!
            )
            b.tvDistance.text = "Distancia: ${String.format("%.2f", distancia)} km"
            b.tvShipping.text = "Despacho: $${shippingCost.toInt()}"
            total += shippingCost
        } else {
            b.tvDistance.text = "Distancia: Desconocida"
            b.tvShipping.text = "Despacho: Calcula tu envío"
        }

        b.tvTotal.text = "Total: $${total.toInt()}"
    }
}