package com.distribuidora.distribuidoralimitada

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
import com.distribuidora.distribuidoralimitada.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtonClickListeners()
        observeViewModel()
    }

    private fun setupButtonClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()

            if (email.isNotEmpty() && pass.length >= 6) {
                authViewModel.register(email, pass)
            } else {
                Toast.makeText(this, "Email y clave (mín. 6 caracteres) son requeridos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observa el StateFlow del usuario
                launch {
                    authViewModel.user.collect { user ->
                        if (user != null) {
                            goHome()
                        }
                    }
                }

                // Observa el SharedFlow de errores
                launch {
                    authViewModel.error.collect { errorMessage ->
                        Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
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