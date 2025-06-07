package com.trujidelivery.trujimerchantt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.databinding.ActivityEditProductBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class EditProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProductBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var imageUri: Uri? = null
    private var productId: String = ""
    private var commerceId: String = ""
    private var categoryId: String = ""
    private var currentImageUrl: String = ""
    private val IMGBB_API_KEY = "84da5659d600368953be77e1e476a634"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.ivProductImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        try {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Por favor, inicia sesión primero", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }
        } catch (e: SecurityException) {
            Log.e("EditProduct", "SecurityException: ${e.message}")
        }

        productId = intent.getStringExtra("PRODUCTO_ID") ?: ""
        commerceId = intent.getStringExtra("COMERCIO_ID") ?: ""
        categoryId = intent.getStringExtra("CATEGORIA_ID") ?: ""

        if (productId.isBlank() || commerceId.isBlank() || categoryId.isBlank()) {
            Log.e("EditProductActivity", "ID de producto, comercio o categoría no proporcionado")
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadProductDataFromFirebase()

        binding.btnSelectProductImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        binding.btnCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun loadProductDataFromFirebase() {
        db.collection("comercios")
            .document(commerceId)
            .collection("productos")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: ""
                    val descripcion = document.getString("descripcion") ?: ""
                    val precio = document.getDouble("precio") ?: 0.0
                    currentImageUrl = document.getString("imagenUrl") ?: ""
                    val isDiscounted = document.getBoolean("isDiscounted") ?: false
                    val discountPercentage = document.getDouble("discountPercentage")
                    val discountedPrice = document.getDouble("discountedPrice")

                    Log.d("EditProduct", "=== DATOS DESDE FIREBASE ===")
                    Log.d("EditProduct", "isDiscounted: $isDiscounted")
                    Log.d("EditProduct", "discountPercentage: $discountPercentage")
                    Log.d("EditProduct", "discountedPrice: $discountedPrice")

                    setupUI(nombre, descripcion, precio, isDiscounted, discountPercentage, discountedPrice)
                } else {
                    Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProduct", "Error cargando producto: ${e.message}")
                Toast.makeText(this, "Error cargando producto", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupUI(
        nombre: String,
        descripcion: String,
        precio: Double,
        isDiscounted: Boolean,
        discountPercentage: Double?,
        discountedPrice: Double?
    ) {
        binding.etProductName.setText(nombre)
        binding.etDescription.setText(descripcion)
        binding.etPrice.setText(String.format("%.2f", precio))

        val shouldCheckDiscount = isDiscounted && discountPercentage != null && discountPercentage > 0 && discountedPrice != null && discountedPrice > 0
        Log.d("EditProduct", "shouldCheckDiscount: $shouldCheckDiscount")

        binding.cbIsDiscounted.isChecked = shouldCheckDiscount

        if (shouldCheckDiscount && discountPercentage != null && discountedPrice != null) {
            binding.etDiscountPercentage.setText(String.format("%.2f", discountPercentage))
            binding.etDiscountedPrice.setText(String.format("%.2f", discountedPrice))
            Log.d("EditProduct", "Campos de descuento establecidos")
        }

        if (currentImageUrl.isNotBlank()) {
            Glide.with(this)
                .load(currentImageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProductImage)
        }

        updateDiscountFieldsVisibility(binding.cbIsDiscounted.isChecked)
        binding.cbIsDiscounted.setOnCheckedChangeListener { _, isChecked ->
            updateDiscountFieldsVisibility(isChecked)
            if (!isChecked) {
                binding.etDiscountPercentage.text?.clear()
                binding.etDiscountedPrice.text?.clear()
            } else {
                calculateDiscountedPrice()
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (binding.cbIsDiscounted.isChecked) {
                    calculateDiscountedPrice()
                }
            }
        }
        binding.etPrice.addTextChangedListener(textWatcher)
        binding.etDiscountPercentage.addTextChangedListener(textWatcher)
    }

    private fun updateDiscountFieldsVisibility(isDiscounted: Boolean) {
        binding.etDiscountPercentage.visibility = if (isDiscounted) View.VISIBLE else View.GONE
        binding.etDiscountedPrice.visibility = if (isDiscounted) View.VISIBLE else View.GONE
        binding.etDiscountedPrice.isEnabled = false
    }

    private fun calculateDiscountedPrice() {
        val price = binding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val discountPercentage = binding.etDiscountPercentage.text.toString().toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0

        if (price > 0 && discountPercentage > 0) {
            val discountedPrice = price * (1 - discountPercentage / 100)
            binding.etDiscountedPrice.setText(String.format("%.2f", discountedPrice))
        } else {
            binding.etDiscountedPrice.setText("")
        }
    }

    private fun saveChanges() {
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()

        if (name.isEmpty() || description.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "El precio debe ser un número válido mayor que 0", Toast.LENGTH_SHORT).show()
            return
        }

        val isDiscounted = binding.cbIsDiscounted.isChecked
        val discountPercentage = if (isDiscounted) {
            binding.etDiscountPercentage.text.toString().toDoubleOrNull()?.coerceIn(0.0, 100.0)
        } else {
            null
        }

        val discountedPrice = if (isDiscounted && discountPercentage != null && discountPercentage > 0) {
            price * (1 - discountPercentage / 100)
        } else {
            null
        }

        if (isDiscounted) {
            if (discountPercentage == null || discountPercentage <= 0 || discountPercentage >= 100) {
                Toast.makeText(this, "El porcentaje de descuento debe estar entre 0 y 100", Toast.LENGTH_SHORT).show()
                return
            }
            if (discountedPrice == null || discountedPrice <= 0 || discountedPrice >= price) {
                Toast.makeText(this, "Error en el cálculo del precio con descuento", Toast.LENGTH_SHORT).show()
                return
            }
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Guardando..."

        if (imageUri != null) {
            convertirUriABase64(imageUri!!) { base64 ->
                if (base64 != null) {
                    subirImagenAImgBB(base64) { imageUrl ->
                        if (imageUrl != null) {
                            updateProducto(name, description, price, imageUrl, isDiscounted, discountPercentage, discountedPrice)
                        } else {
                            resetSaveButton("Error al subir la imagen")
                        }
                    }
                } else {
                    resetSaveButton("No se pudo convertir la imagen")
                }
            }
        } else {
            updateProducto(name, description, price, currentImageUrl, isDiscounted, discountPercentage, discountedPrice)
        }
    }

    private fun resetSaveButton(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        binding.btnSave.isEnabled = true
        binding.btnSave.text = "Guardar Cambios"
    }

    private fun updateProducto(
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        isDiscounted: Boolean,
        discountPercentage: Double?,
        discountedPrice: Double?
    ) {
        val productData = hashMapOf(
            "nombre" to name,
            "descripcion" to description,
            "precio" to price,
            "imagenUrl" to imageUrl,
            "isDiscounted" to isDiscounted,
            "discountPercentage" to discountPercentage,
            "discountedPrice" to discountedPrice
        ) as Map<String, Any>

        val batch = db.batch()

        val comercioRef = db.collection("comercios")
            .document(commerceId)
            .collection("productos")
            .document(productId)
        batch.update(comercioRef, productData)

        val categoriaRef = db.collection("categorias")
            .document(categoryId)
            .collection("negocios")
            .document(commerceId)
            .collection("productos")
            .document(productId)
        batch.update(categoriaRef, productData)

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado exitosamente", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                resetSaveButton("Error al actualizar producto: ${e.message}")
            }
    }

    private fun convertirUriABase64(uri: Uri, callback: (String?) -> Unit) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.let {
                val bytes = it.readBytes()
                if (bytes.size > 700000) {
                    Toast.makeText(this, "La imagen es demasiado grande, elige una más pequeña", Toast.LENGTH_SHORT).show()
                    callback(null)
                    return
                }
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                callback(base64String)
                it.close()
            } ?: run {
                callback(null)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al convertir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            callback(null)
        }
    }

    private fun subirImagenAImgBB(base64: String, callback: (String?) -> Unit) {
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("key", IMGBB_API_KEY)
            .add("image", base64)
            .add("expiration", "0")
            .build()
        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { callback(null) }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    try {
                        val responseBody = response.body?.string()
                        if (response.isSuccessful && responseBody != null) {
                            val jsonResponse = JSONObject(responseBody)
                            if (jsonResponse.getBoolean("success")) {
                                val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                                callback(imageUrl)
                            } else {
                                callback(null)
                            }
                        } else {
                            callback(null)
                        }
                    } catch (e: Exception) {
                        callback(null)
                    }
                }
            }
        })
    }
}