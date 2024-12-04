package com.example.amerinst

data class Nota(
    val estudianteNombre: String,
    val estudianteApellido: String,
    val estudianteId: Int,
    val materiaNombre: String,
    val nota: Float,
    val bimestre: Int,
    val tipo: String
)
