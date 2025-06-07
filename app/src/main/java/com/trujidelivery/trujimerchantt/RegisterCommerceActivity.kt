package com.trujidelivery.trujimerchantt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class RegisterCommerceActivity : AppCompatActivity() {
    private lateinit var etCommerceName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etAddress: EditText
    private lateinit var ivCommerceImage: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnRegisterCommerce: Button
    private var imageUri: Uri? = null
    private val IMGBB_API_KEY = "84da5659d600368953be77e1e476a634"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            ivCommerceImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_commerce)
        supportActionBar?.hide()

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Toast.makeText(this, "Por favor, inicia sesión primero", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        etCommerceName = findViewById(R.id.etCommerceName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etAddress = findViewById(R.id.etAddress)
        ivCommerceImage = findViewById(R.id.ivCommerceImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnRegisterCommerce = findViewById(R.id.btnRegisterCommerce)

        val categories = arrayOf("Restaurantes", "Farmacias", "Juguerías", "Supermercados")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRegisterCommerce.setOnClickListener {
            registerCommerce()
        }
    }

    private fun registerCommerce() {
        val name = etCommerceName.text.toString().trim()
        val categoryName = spinnerCategory.selectedItem.toString()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty() || address.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Por favor, completa todos los campos y selecciona una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        btnRegisterCommerce.isEnabled = false
        btnRegisterCommerce.text = "Subiendo imagen..."

        convertirUriABase64(imageUri!!) { base64 ->
            if (base64 != null) {
                subirImagenAImgBB(base64) { imageUrl ->
                    if (imageUrl != null) {
                        procesarRegistroConImagenUrl(name, categoryName, address, base64, imageUrl)
                    } else {
                        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                        btnRegisterCommerce.isEnabled = true
                        btnRegisterCommerce.text = "Registrar Negocio"
                    }
                }
            } else {
                Toast.makeText(this, "No se pudo convertir la imagen a Base64", Toast.LENGTH_SHORT).show()
                btnRegisterCommerce.isEnabled = true
                btnRegisterCommerce.text = "Registrar Negocio"
            }
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

    private fun procesarRegistroConImagenUrl(name: String, categoryName: String, address: String, base64: String, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("categorias")
            .whereEqualTo("nombre", categoryName)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    db.collection("categorias")
                        .add(hashMapOf(
                            "nombre" to categoryName,
                            "imagen_base64" to null,
                            "imagenUrl" to ""
                        ))
                        .addOnSuccessListener { categoryDoc ->
                            saveNegocio(categoryDoc.id, name, address, base64, imageUrl)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al crear categoría: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnRegisterCommerce.isEnabled = true
                            btnRegisterCommerce.text = "Registrar Negocio"
                        }
                } else {
                    val categoryId = snapshot.documents.first().id
                    saveNegocio(categoryId, name, address, base64, imageUrl)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al buscar categoría: ${e.message}", Toast.LENGTH_SHORT).show()
                btnRegisterCommerce.isEnabled = true
                btnRegisterCommerce.text = "Registrar Negocio"
            }
    }

    private fun saveNegocio(categoryId: String, name: String, address: String, base64: String, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val negocioData = hashMapOf(
            "nombre" to name,
            "direccion" to address,
            "imagen_base64" to base64,
            "imagenUrl" to imageUrl,
            "created_at" to System.currentTimeMillis().toString(),
            "categoriaId" to categoryId,
            "usuarioId" to usuarioId
        )

        db.collection("comercios")
            .add(negocioData)
            .addOnSuccessListener { comercioDoc ->
                val comercioId = comercioDoc.id
                db.collection("categorias")
                    .document(categoryId)
                    .collection("negocios")
                    .document(comercioId)
                    .set(negocioData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Negocio registrado exitosamente!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al registrar negocio en categoría: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnRegisterCommerce.isEnabled = true
                        btnRegisterCommerce.text = "Registrar Negocio"
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar negocio: ${e.message}", Toast.LENGTH_SHORT).show()
                btnRegisterCommerce.isEnabled = true
                btnRegisterCommerce.text = "Registrar Negocio"
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
            } ?: callback(null)
        } catch (e: Exception) {
            callback(null)
            Toast.makeText(this, "Error al convertir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}