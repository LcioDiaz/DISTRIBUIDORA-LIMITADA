package com.distribuidora.distribuidoralimitada.util

/**
 * Clase que encapsula la lógica y las reglas de negocio para el cálculo del costo de envío.
 */
class ReglasEnvio {
    // Coordenadas de la tienda física.
    val latitudTienda = -43.6167
    val longitudTienda = -71.8000

    /**
     * Calcula el costo de envío basado en el total de la compra y la distancia al cliente.
     *
     * @param totalCompra El monto total de la compra.
     * @param latitudCliente La latitud de la dirección de entrega.
     * @param longitudCliente La longitud de la dirección de entrega.
     * @return El costo del envío en pesos.
     */
    fun calcularCostoEnvio(totalCompra: Double, latitudCliente: Double, longitudCliente: Double): Double {
        // Calcula la distancia en kilómetros usando la fórmula de Haversine.
        val distancia = Haversine.distanceKm(
            latitudTienda, longitudTienda,
            latitudCliente, longitudCliente
        )

        // Regla 1: Despacho gratuito por compras sobre $50.000 dentro de un radio de 20 km.
        if (totalCompra > 50000 && distancia <= 20) {
            return 0.0 // Costo de envío es cero.
        }

        // Regla 2: Tarifa de $150/km para compras entre $25.000 y $49.999.
        if (totalCompra in 25000.0..49999.0) {
            return distancia * 150
        }

        // Regla 3: Tarifa de $300/km para compras menores a $25.000.
        if (totalCompra < 25000) {
            return distancia * 300
        }

        // Caso por defecto (ej. compras sobre $50.000 pero fuera del radio de 20km).
        // Se aplica la tarifa estándar de $150/km.
        return distancia * 150
    }
}