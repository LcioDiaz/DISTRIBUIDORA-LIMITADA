package com.distribuidora.distribuidoralimitada

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.distribuidora.distribuidoralimitada.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Actividad inicial que actúa como un selector de roles.
 * Desde aquí, el usuario elige si es cliente o transportista antes de proceder
 * a la pantalla de autenticación real (AuthActivity).
 */
class LoginActivity : AppCompatActivity() {

    // Objeto de binding para acceder a las vistas del layout.
    private lateinit var binding: ActivityLoginBinding
    // Instancia de FirebaseAuth para verificar si ya hay una sesión activa.
    private val auth = FirebaseAuth.getInstance()

    /**
     * El método onStart se ejecuta cada vez que la actividad se vuelve visible.
     * Es el lugar ideal para verificar el estado de autenticación.
     */
    override fun onStart() {
        super.onStart()
        /*
         * Bloque de verificación de sesión:
         * Si ya hay un usuario logueado (auth.currentUser no es nulo), no tiene sentido
         * mostrarle esta pantalla de selección de rol. Por lo tanto, lo redirigimos
         * directamente a AuthActivity, que se encargará de la lógica de redirección
         * final a la pantalla de Home correspondiente.
         */
        if (auth.currentUser != null) {
            val intent = Intent(this, AuthActivity::class.java).apply {
                // Las flags limpian el historial de actividades, evitando que el usuario
                // pueda volver a esta pantalla con el botón de "atrás".
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish() // Cierra LoginActivity.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Listener para el botón "Soy Cliente".
        binding.btnSoyCliente.setOnClickListener {
            // Navega a la pantalla de autenticación pasando el rol "cliente".
            navigateToAuth("cliente")
        }

        // Listener para el botón "Soy Transportista".
        binding.btnSoyTransportista.setOnClickListener {
            // Navega a la pantalla de autenticación pasando el rol "transportista".
            navigateToAuth("transportista")
        }
    }

    /**
     * Crea un Intent para ir a AuthActivity y le pasa el rol seleccionado.
     * AuthActivity usará este rol para saber a qué pantalla de registro debe ir
     * si el usuario pulsa el enlace "¿No tienes cuenta?".
     *
     * @param role El rol del usuario ("cliente" o "transportista").
     */
    private fun navigateToAuth(role: String) {
        val intent = Intent(this, AuthActivity::class.java).apply {
            putExtra("USER_ROLE", role)
        }
        startActivity(intent)
    }
}