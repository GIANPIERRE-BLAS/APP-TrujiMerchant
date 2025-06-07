package com.trujidelivery.trujimerchantt.modelo

import com.google.firebase.firestore.PropertyName

data class Producto(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var precio: Double = 0.0,
    var imagenUrl: String = "",
    var comercioId: String = "",
    var categoriaId: String = "",

    @get:PropertyName("isDiscounted")
    @set:PropertyName("isDiscounted")
    var isDiscounted: Boolean = false,

    var discountPercentage: Double? = 0.0,
    var discountedPrice: Double? = 0.0,
    var created_at: Any? = null
)
