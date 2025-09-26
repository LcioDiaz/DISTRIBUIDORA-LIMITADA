package com.distribuidora.distribuidoralimitada

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.distribuidora.distribuidoralimitada.auth.AuthViewModel
import com.distribuidora.distribuidoralimitada.auth.FirebaseClient
import com.distribuidora.distribuidoralimitada.databinding.ActivityAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleClient: GoogleSignInClient

    // Obtiene el rol del Intent. Si no viene, será "cliente" por defecto.
    private val userRoleFromIntent by lazy { intent.getStringExtra("USER_ROLE") ?: "cliente" }

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (!token.isNullOrEmpty()) {
                authViewModel.loginWithGoogle(token, userRoleFromIntent)
            } else {
                Toast.makeText(this, "No se pudo obtener el token de Google.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Falló el inicio de sesión con Google.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.login(email, password)
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogle.setOnClickListener {
            googleClient.signOut().addOnCompleteListener {
                googleLauncher.launch(googleClient.signInIntent)
            }
        }

        binding.tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("USER_ROLE", userRoleFromIntent)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        authViewModel.user.observe(this) { user ->
            if (user != null) {
                // Cuando el login es exitoso, manejamos la redirección
                handleLoginSuccess()
            }
        }

        authViewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Lógica CENTRALIZADA de redirección post-login.
     * Lee el rol de la base de datos y redirige. Si el rol no existe,
     * lo crea usando el rol que viene desde LoginActivity.
     */
    private fun handleLoginSuccess() {
        val uid = authViewModel.user.value?.uid
        if (uid == null) {
            Toast.makeText(this, "Error: No se pudo obtener el ID de usuario.", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseClient.usersRef.child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild("rol")) {
                    // El rol ya existe en la base de datos, lo leemos y redirigimos.
                    val userRole = snapshot.child("rol").getValue(String::class.java)
                    redirectToHome(userRole)
                } else {
                    // El rol NO existe. Lo creamos usando el rol del Intent y luego redirigimos.
                    // Esto es crucial para usuarios que ya existían pero no tenían un rol asignado.
                    dbRef.child("rol").setValue(userRoleFromIntent)
                        .addOnSuccessListener {
                            redirectToHome(userRoleFromIntent)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@AuthActivity, "No se pudo establecer el rol.", Toast.LENGTH_LONG).show()
                            authViewModel.logout()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AuthActivity, "Error al verificar rol: ${error.message}", Toast.LENGTH_SHORT).show()
                authViewModel.logout()
            }
        })
    }

    /**
     * Función única para navegar a la pantalla de Home correcta.
     */
    private fun redirectToHome(role: String?) {
        val intent = if (role == "transportista") {
            Intent(this, TransportistaHomeActivity::class.java)
        } else {
            Intent(this, HomeActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}