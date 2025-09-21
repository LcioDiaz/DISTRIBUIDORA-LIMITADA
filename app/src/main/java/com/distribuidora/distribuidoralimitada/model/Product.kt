package com.distribuidora.distribuidoralimitada.model

data class Product(
    val id: Int,
    val name: String,
    val price: Int,
    val coldChain: Boolean = false
)