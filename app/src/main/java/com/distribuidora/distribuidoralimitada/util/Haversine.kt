package com.distribuidora.distribuidoralimitada.util

import kotlin.math.*

object Haversine {
    private const val R_KM = 6371.0

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        require(lat1 in -90.0..90.0 && lat2 in -90.0..90.0 &&
                lon1 in -180.0..180.0 && lon2 in -180.0..180.0) { "Coordenadas fuera de rango" }

        val phi1 = lat1.toRad()
        val phi2 = lat2.toRad()
        val dPhi = (lat2 - lat1).toRad()
        val dLambda = (lon2 - lon1).toRad()

        val sinDphi = sin(dPhi / 2)
        val sinDlam = sin(dLambda / 2)

        // a puede salir ligeramente >1 o <0 por redondeo; lo acotamos
        val a = (sinDphi * sinDphi + cos(phi1) * cos(phi2) * sinDlam * sinDlam).coerceIn(0.0, 1.0)
        val c = 2 * asin(sqrt(a))
        return R_KM * c
    }
    private fun Double.toRad() = this * PI / 180.0
}

