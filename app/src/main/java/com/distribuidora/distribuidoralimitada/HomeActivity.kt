package com.distribuidora.distribuidoralimitada

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
import com.distribuidora.distribuidoralimitada.databinding.ActivityHomeBinding
import com.distribuidora.distribuidoralimitada.model.Product
import com.distribuidora.distribuidoralimitada.ui.ProductAdapter
import com.distribuidora.distribuidoralimitada.util.Haversine
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.roundToInt
import android.annotation.SuppressLint
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityHomeBinding
    private val authVM: AuthViewModel by viewModels()

    // Plaza/Bodega Palena (ajusta si quieres más precisión)
    private val BODEGA_LAT = -43.6167
    private val BODEGA_LNG = -71.8000

    private val products = listOf(
        Product(1, "Arroz 1kg", 1200, false),
        Product(2, "Leche 1L", 1100, false),
        Product(3, "Carne congelada 1kg", 8500, true),
        Product(4, "Mariscos congelados 1kg", 9900, true),
        Product(5, "Aceite 1L", 2500, false)
    )
    private val qty = MutableList(products.size) { 0 }
    private var distanceKm: Double? = null

    private val locationPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val ok = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) fetchDistance()
        else Toast.makeText(this, "Permiso de ubicación requerido", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        // 1) Observa el estado de sesión: cuando user == null, vamos a Login.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authVM.user.collect { user ->
                    if (user == null) {
                        startActivity(
                            Intent(this@HomeActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                        finish()
                    }
                }
            }
        }

        b.rvProducts.layoutManager = LinearLayoutManager(this)
        b.rvProducts.adapter = ProductAdapter(products, qty) {
            updateTotals()
            checkColdAlarm()
        }

        // 2) Logout: solo cerrar sesión. La navegación la hace el collector de arriba.
        b.btnLogout.setOnClickListener {
            b.btnLogout.isEnabled = false // evita doble click
            authVM.logout()

            // (Opcional) Si alguna vez usaste Google Sign-In en este dispositivo, también cierra:
            // try {
            //     val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            //     GoogleSignIn.getClient(this, gso).signOut()
            // } catch (_: Exception) {}
        }

        b.btnCalcDistance.setOnClickListener {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                fetchDistance()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder(this)
                        .setTitle("Permiso de ubicación necesario")
                        .setMessage("Necesitamos tu ubicación para calcular el costo de envío.")
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

        updateTotals()
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("MissingPermission")
    private fun fetchDistance() {
        val fused = LocationServices.getFusedLocationProviderClient(this)

        fused.lastLocation
            .addOnSuccessListener { last ->
                val now = System.currentTimeMillis()
                val fresh = last != null && (now - (last.time ?: 0L)) <= 2 * 60_000
                if (fresh) {
                    updateDistanceAndTotals(last!!.latitude, last.longitude)
                } else {
                    val cts = CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                updateDistanceAndTotals(loc.latitude, loc.longitude)
                            } else {
                                Toast.makeText(this, "No se pudo obtener ubicación actual", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error obteniendo ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    // timeout para no colgarse si el GPS no fija
                    b.root.postDelayed({ cts.cancel() }, 8_000)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error ubicación previa: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDistanceAndTotals(lat: Double, lng: Double) {
        // Debug visible (comenta si ya no lo necesitas)
        b.tvDistance.text = "Recibido: lat=${"%.5f".format(lat)}, lng=${"%.5f".format(lng)}"

        // Si llega (0,0) o fuera de rango, lo tomamos como inválido
        val invalid = (lat == 0.0 && lng == 0.0) || lat !in -90.0..90.0 || lng !in -180.0..180.0
        if (invalid) {
            Toast.makeText(this, "Ubicación inválida (0,0). Revisa que el GPS esté activo.", Toast.LENGTH_LONG).show()
            return
        }

        distanceKm = Haversine.distanceKm(lat, lng, BODEGA_LAT, BODEGA_LNG)
        b.tvDistance.text = "Distancia: ${String.format("%.2f", distanceKm)} km"
        updateTotals()
    }

    private fun updateTotals() {
        val subtotal = products.indices.sumOf { products[it].price * qty[it] }
        val d = distanceKm ?: 0.0
        val shipping = when {
            subtotal >= 50_000 && d <= 20.0 -> 0
            subtotal in 25_000..49_999      -> (150.0 * d).roundToInt()
            else                            -> (300.0 * d).roundToInt()
        }
        b.tvSubtotal.text = "Subtotal: $$subtotal"
        b.tvShipping.text = "Despacho: $$shipping"
        b.tvTotal.text = "Total: $${subtotal + shipping}"
    }

    private fun checkColdAlarm() {
        val hasCold = products.indices.any { products[it].coldChain && qty[it] > 0 }
        val tempTxt = b.etFreezerTemp.text?.toString()?.trim()
        val temp = tempTxt?.toDoubleOrNull()
        if (hasCold && temp != null && temp > -18.0) {
            AlertDialog.Builder(this)
                .setTitle("Alerta: cadena de frío")
                .setMessage("La temperatura actual supera -18°C y el carrito incluye congelados.")
                .setPositiveButton("Entendido", null)
                .show()
        }
    }
}
