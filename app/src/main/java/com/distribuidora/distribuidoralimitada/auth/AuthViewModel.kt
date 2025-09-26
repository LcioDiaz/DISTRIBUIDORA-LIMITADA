package com.distribuidora.distribuidoralimitada.auth

import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseClient.auth

    private val _user = MutableLiveData<FirebaseUser?>(auth.currentUser)
    val user: LiveData<FirebaseUser?> = _user

    private val _error = MutableLiveData("")
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _user.value = firebaseAuth.currentUser
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun register(email: String, pass: String, nombre: String, rol: String) {
        viewModelScope.launch {
            _loading.postValue(true)
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                authResult.user?.let { fbUser ->
                    val data = mapOf(
                        "nombre" to nombre,
                        "email" to email,
                        "rol" to rol
                    )
                    // Usamos el FirebaseClient centralizado
                    FirebaseClient.usersRef.child(fbUser.uid).setValue(data).await()
                    _user.postValue(fbUser)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register()", e)
                val msg = when (e) {
                    is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado."
                    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (mínimo 6)."
                    is FirebaseAuthInvalidCredentialsException -> "Correo inválido."
                    is FirebaseNetworkException -> "Sin conexión. Intenta nuevamente."
                    else -> "Error en el registro. Intenta más tarde."
                }
                _error.postValue(msg)
            } finally {
                _loading.postValue(false)
            }
        }
    }

    /**
     * El ViewModel  se encarga de iniciar sesión.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _user.postValue(auth.currentUser)
            } catch (e: Exception) {
                Log.e("AuthVM", "Error de login: ", e)
                val msg = when (e) {
                    is FirebaseAuthInvalidUserException -> "El usuario no existe."
                    is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta."
                    is FirebaseNetworkException -> "Sin conexión a internet."
                    else -> "No se pudo iniciar sesión. Intente más tarde."
                }
                _error.postValue(msg)
            }
        }
    }

    fun loginWithGoogle(idToken: String, rol: String) {
        viewModelScope.launch {
            _loading.postValue(true)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

                if (firebaseUser != null && isNewUser) {
                    // Si el usuario es nuevo, creamos su entrada en la DB con el rol correcto
                    val data = mapOf(
                        "nombre" to (firebaseUser.displayName ?: "Usuario"),
                        "email" to (firebaseUser.email ?: ""),
                        "rol" to rol
                    )
                    FirebaseClient.usersRef.child(firebaseUser.uid).setValue(data).await()
                }
                _user.postValue(firebaseUser)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loginWithGoogle()", e)
                _error.postValue("Error con Google. Verifica tu configuración.")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun logout() {
        auth.signOut()
        _user.postValue(null)
    }
}
