package com.distribuidora.distribuidoralimitada

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
import com.distribuidora.distribuidoralimitada.databinding.ActivityRegisterBinding

/**
 * Actividad ÚNICA para el registro de usuarios.
 * Recibe el rol ("cliente" o "transportista") desde el Intent
 * para adaptar la UI y la lógica de registro.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModels()

    // Propiedad 'lazy' para obtener el rol del Intent. Si no viene, es "cliente" por defecto.
    private val role by lazy { intent.getStringExtra("USER_ROLE") ?: "cliente" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        observeViewModel()
    }

    /**
     * Adapta la interfaz de usuario según el rol del usuario.
     */
    private fun setupUI() {
        binding.tvTitle.text = if (role == "transportista") {
            "Registro de Transportista"
        } else {
            "Registro de Cliente"
        }
    }

    /**
     * Configura los listeners para los botones y otros elementos interactivos.
     */
    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString() // La contraseña no se trimea
            val nombre = binding.etNombre.text.toString().trim()

            // Validaciones de los campos de entrada.
            if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                toast("Por favor, completa todos los campos")
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("El formato del correo no es válido")
                return@setOnClickListener
            }
            if (pass.length < 6) {
                toast("La contraseña debe tener al menos 6 caracteres")
                return@setOnClickListener
            }

            // Si todo es válido, se llama al ViewModel para que inicie el registro.
            authViewModel.register(email, pass, nombre, role)
        }

        binding.tvGoToLogin.setOnClickListener {
            // Cierra esta actividad para volver a la pantalla anterior (login/auth).
            finish()
        }
    }

    /**
     * Observa los cambios en el ViewModel (usuario, errores, estado de carga).
     */
    private fun observeViewModel() {
        authViewModel.user.observe(this) { user ->
            if (user != null) {
                // Si el registro es exitoso, navega a la pantalla principal correspondiente.
                goToHomeByRole()
            }
        }
        authViewModel.error.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                // Si hay un error, lo muestra en un Toast.
                toast(errorMsg)
            }
        }
        authViewModel.loading.observe(this) { isLoading ->
            // Activa o desactiva los campos mientras se realiza el registro.
            setLoading(isLoading)
        }
    }

    /**
     * Lógica de navegación. Redirige al Home del Cliente o del Transportista
     * según el rol con el que se registró el usuario.
     */
    private fun goToHomeByRole() {
        val intent = if (role == "transportista") {
            Intent(this, TransportistaHomeActivity::class.java)
        } else {
            Intent(this, HomeActivity::class.java)
        }
        // Limpia el stack de actividades para que el usuario no pueda volver atrás.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra la actividad de registro.
    }

    /**
     * Controla el estado de carga de la UI.
     */
    private fun setLoading(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etNombre.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}