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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.databinding.ActivityAddProductBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AddProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddProductBinding
    private var imageUri: Uri? = null
    private lateinit var commerceId: String
    private lateinit var categoryId: String
    private val IMGBB_API_KEY = "84da5659d600368953be77e1e476a634"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.ivProductImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Por favor, inicia sesión primero", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        commerceId = intent.getStringExtra("commerceId") ?: ""
        categoryId = intent.getStringExtra("categoryId") ?: ""

        if (commerceId.isBlank() || categoryId.isBlank()) {
            Log.e("AddProductActivity", "ID de comercio o categoría no proporcionado")
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvDiscountPercentageLabel.visibility = View.GONE
        binding.etDiscountPercentage.visibility = View.GONE
        binding.etDiscountPercentage.isEnabled = false
        binding.tvDiscountedPriceLabel.visibility = View.GONE
        binding.etDiscountedPrice.visibility = View.GONE
        binding.etDiscountedPrice.isEnabled = false

        binding.cbApplyDiscount.setOnCheckedChangeListener { _, isChecked ->
            binding.tvDiscountPercentageLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.etDiscountPercentage.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.etDiscountPercentage.isEnabled = isChecked
            binding.tvDiscountedPriceLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.etDiscountedPrice.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.etDiscountPercentage.text.clear()
                binding.etDiscountedPrice.text.clear()
            } else {
                calculateDiscountedPrice()
            }
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (binding.cbApplyDiscount.isChecked) {
                    calculateDiscountedPrice()
                }
            }
        }
        binding.etPrice.addTextChangedListener(textWatcher)
        binding.etDiscountPercentage.addTextChangedListener(textWatcher)

        binding.btnSelectProductImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnAddProduct.setOnClickListener {
            addProduct()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun calculateDiscountedPrice() {
        val priceText = binding.etPrice.text.toString().trim()
        val discountText = binding.etDiscountPercentage.text.toString().trim()

        val price = priceText.toDoubleOrNull() ?: 0.0
        val discountPercentage = discountText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0

        val discountedPrice = if (discountPercentage > 0) {
            price * (1 - discountPercentage / 100)
        } else {
            price
        }

        binding.etDiscountedPrice.setText(String.format("%.2f", discountedPrice))
    }

    private fun addProduct() {
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val discountPercentageText = binding.etDiscountPercentage.text.toString().trim()

        if (name.isEmpty() || description.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "El precio debe ser un número válido mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        val isDiscounted = binding.cbApplyDiscount.isChecked
        val discountPercentage = if (isDiscounted) discountPercentageText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0 else 0.0
        val discountedPrice = if (isDiscounted && discountPercentage > 0) {
            price * (1 - discountPercentage / 100)
        } else {
            null
        }

        if (imageUri == null) {
            Toast.makeText(this, "Por favor, selecciona una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAddProduct.isEnabled = false
        binding.btnAddProduct.text = "Guardando..."

        convertirUriABase64(imageUri!!) { base64 ->
            if (base64 != null) {
                subirImagenAImgBB(base64) { imageUrl ->
                    if (imageUrl != null) {
                        saveProductToFirestore(name, description, price, imageUrl, isDiscounted, discountPercentage, discountedPrice)
                    } else {
                        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                        binding.btnAddProduct.isEnabled = true
                        binding.btnAddProduct.text = "Agregar y Finalizar"
                    }
                }
            } else {
                Toast.makeText(this, "No se pudo convertir la imagen", Toast.LENGTH_SHORT).show()
                binding.btnAddProduct.isEnabled = true
                binding.btnAddProduct.text = "Agregar y Finalizar"
            }
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
                runOnUiThread {
                    callback(null)
                }
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

    private fun saveProductToFirestore(
        name: String,
        description: String,
        price: Double,
        imageUrl: String,
        isDiscounted: Boolean,
        discountPercentage: Double,
        discountedPrice: Double?
    ) {
        val productData = hashMapOf(
            "nombre" to name,
            "descripcion" to description,
            "precio" to price,
            "imagenUrl" to imageUrl,
            "isDiscounted" to isDiscounted,
            "discountPercentage" to if (isDiscounted) discountPercentage else null,
            "discountedPrice" to discountedPrice,
            "created_at" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("comercios")
            .document(commerceId)
            .collection("productos")
            .add(productData)
            .addOnSuccessListener { documentReference ->
                val productId = documentReference.id
                db.collection("categorias")
                    .document(categoryId)
                    .collection("negocios")
                    .document(commerceId)
                    .collection("productos")
                    .document(productId)
                    .set(productData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al guardar producto: ${e.message}", Toast.LENGTH_SHORT).show()
                        binding.btnAddProduct.isEnabled = true
                        binding.btnAddProduct.text = "Agregar y Finalizar"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar producto: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnAddProduct.isEnabled = true
                binding.btnAddProduct.text = "Agregar y Finalizar"
            }
    }
}