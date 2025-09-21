package com.distribuidora.distribuidoralimitada

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
import com.distribuidora.distribuidoralimitada.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var googleClient: GoogleSignInClient

    // Launcher para el flujo de Google
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrEmpty()) {
                    Toast.makeText(this, "No se recibió idToken de Google", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
                // Intercambia el idToken por credencial Firebase
                authViewModel.loginWithGoogle(idToken)
            } catch (e: ApiException) {
                // Códigos comunes:
                // 10 = DEVELOPER_ERROR (falta SHA-1/Google activado en Firebase)
                // 7  = NETWORK_ERROR
                // 12501 = canceled
                val msg = when (e.statusCode) {
                    10 -> "Error de configuración (código 10). Verifica SHA-1 en Firebase y que Google esté habilitado."
                    7  -> "Error de red. Revisa tu conexión."
                    12501 -> "Inicio con Google cancelado."
                    else -> "Google Sign-In falló (${e.statusCode}). ${e.localizedMessage}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura el cliente de Google con tu web client id (client_type:3)
        googleClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        setupButtonClickListeners()
        observeViewModel()
    }

    private fun setupButtonClickListeners() {
        // Email/Password
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Por favor, ingresa email y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        // Google
        binding.btnGoogle.setOnClickListener {
            // Limpia sesión previa de Google para evitar cuentas “pegadas”
            googleClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleClient.signInIntent)
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authViewModel.user.collect { user ->
                        if (user != null) {
                            goHome()
                        }
                    }
                }
                launch {
                    authViewModel.error.collect { errorMessage ->
                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun goHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
