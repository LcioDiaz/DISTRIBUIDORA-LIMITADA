package com.distribuidora.distribuidoralimitada.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * Objeto Singleton  centralizar el acceso a las instancias de Firebase.
 * Esto evita crear múltiples instancias en diferentes Activities y ViewModels.
 */
object FirebaseClient {

    // Instancia perezosa para FirebaseAuth. Solo se crea una vez cuando se necesita.
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Instancia perezosa para FirebaseDatabase.
    val db: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    // Referencia perezosa a la colección "users".
    val usersRef by lazy {
        db.getReference("users")
    }
}