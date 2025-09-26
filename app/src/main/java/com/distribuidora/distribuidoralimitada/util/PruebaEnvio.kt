package com.distribuidora.distribuidoralimitada.util

fun main() {
    probarReglasDeEnvioActualizadas()
}

fun probarReglasDeEnvioActualizadas() {
    val reglas = ReglasEnvio()

    // --- Coordenadas de prueba ---
    // Cliente cercano (~10 km)
    val latClienteCercano = -33.5000
    val lonClienteCercano = -70.7000

    // Cliente lejano (~25 km, fuera del radio de 20 km)
    val latClienteLejano = -33.6000
    val lonClienteLejano = -70.8000

    println("--- INICIO DE PRUEBAS DE REGLAS DE ENVÍO ---")

    // Caso 1: Compra > $50.000, DENTRO del radio -> Envío GRATIS
    val costo1 = reglas.calcularCostoEnvio(60000.0, latClienteCercano, lonClienteCercano)
    println("Caso 1: Compra > $50.000, cercano -> Costo: $${costo1.toInt()} (Esperado: 0)")

    // Caso 2: Compra > $50.000, FUERA del radio -> Tarifa $150/km
    val costo2 = reglas.calcularCostoEnvio(60000.0, latClienteLejano, lonClienteLejano)
    println("Caso 2: Compra > $50.000, lejano   -> Costo: $${costo2.toInt()} (Esperado: > 0)")

    // Caso 3: Compra entre $25.000 y $49.999 -> Tarifa $150/km
    val costo3 = reglas.calcularCostoEnvio(35000.0, latClienteCercano, lonClienteCercano)
    println("Caso 3: Compra $35.000, cercano   -> Costo: $${costo3.toInt()} (Esperado: > 0, tarifa $150/km)")

    // Caso 4: Compra < $25.000 -> Tarifa $300/km
    val costo4 = reglas.calcularCostoEnvio(15000.0, latClienteCercano, lonClienteCercano)
    println("Caso 4: Compra < $25.000, cercano   -> Costo: $${costo4.toInt()} (Esperado: > 0, tarifa $300/km, aprox. el doble que Caso 3)")

    println("--- FIN DE PRUEBAS ---")
}