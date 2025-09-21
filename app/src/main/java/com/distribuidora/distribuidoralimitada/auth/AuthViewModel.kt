package com.distribuidora.distribuidoralimitada.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
        }
    }

    fun register(email: String, password: String) = viewModelScope.launch {
        try { auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            _error.emit(e.message ?: "Error en el registro")
            Log.e("AuthVM", "Registro: ${e.message}")
        }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        try { auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            _error.emit(e.message ?: "Error en el inicio de sesión")
            Log.e("AuthVM", "Login: ${e.message}")
        }
    }

    fun loginWithGoogle(idToken: String) = viewModelScope.launch {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
        } catch (e: Exception) {
            Log.e("AuthVM", "Google: ${e.message}")
        }
    }

    fun logout() {
        auth.signOut()
    }
}
