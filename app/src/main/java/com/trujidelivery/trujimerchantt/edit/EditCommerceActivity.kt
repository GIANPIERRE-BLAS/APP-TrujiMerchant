package com.trujidelivery.trujimerchantt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class EditCommerceActivity : AppCompatActivity() {
    private lateinit var etCommerceName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etAddress: EditText
    private lateinit var ivCommerceImage: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnSaveChanges: Button
    private lateinit var btnCancel: Button
    private var imageUri: Uri? = null
    private lateinit var negocioId: String
    private lateinit var categoriaId: String
    private var currentImageUrl: String = ""
    private val IMGBB_API_KEY = "84da5659d600368953be77e1e476a634"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            ivCommerceImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_commerce)
        supportActionBar?.hide()

        etCommerceName = findViewById(R.id.etCommerceName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etAddress = findViewById(R.id.etAddress)
        ivCommerceImage = findViewById(R.id.ivCommerceImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnCancel = findViewById(R.id.btnCancel)

        negocioId = intent.getStringExtra("NEGOCIO_ID") ?: ""
        categoriaId = intent.getStringExtra("CATEGORIA_ID") ?: ""
        val nombre = intent.getStringExtra("nombre") ?: ""
        val direccion = intent.getStringExtra("direccion") ?: ""
        currentImageUrl = intent.getStringExtra("imagenUrl") ?: ""

        if (negocioId.isBlank() || categoriaId.isBlank()) {
            Log.e("EditCommerceActivity", "ID de negocio o categoría no proporcionado")
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etCommerceName.setText(nombre)
        etAddress.setText(direccion)
        Glide.with(this)
            .load(currentImageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivCommerceImage)

        val categories = arrayOf("Restaurantes", "Farmacias", "Juguerías", "Supermercados")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        val categoryIndex = categories.indexOfFirst { it == getCategoryName(categoriaId) }
        if (categoryIndex >= 0) {
            spinnerCategory.setSelection(categoryIndex)
        }

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSaveChanges.setOnClickListener {
            saveChanges()
        }

        btnCancel.setOnClickListener {
            val intent = Intent(this, BusinessListActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun getCategoryName(categoryId: String): String {
        return when (categoryId) {
            "restaurantes" -> "Restaurantes"
            "farmacias" -> "Farmacias"
            "juguerias" -> "Juguerías"
            "supermercados" -> "Supermercados"
            else -> "Restaurantes"
        }
    }

    private fun saveChanges() {
        val name = etCommerceName.text.toString().trim()
        val categoryName = spinnerCategory.selectedItem.toString()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveChanges.isEnabled = false
        btnSaveChanges.text = "Guardando..."

        if (imageUri != null) {
            convertirUriABase64(imageUri!!) { base64 ->
                if (base64 != null) {
                    Log.d("EditCommerceActivity", "Imagen convertida a Base64, longitud: ${base64.length}")
                    subirImagenAImgBB(base64) { imageUrl ->
                        if (imageUrl != null) {
                            Log.d("EditCommerceActivity", "Imagen subida, URL: $imageUrl")
                            updateNegocio(name, categoryName, address, imageUrl)
                        } else {
                            Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                            btnSaveChanges.isEnabled = true
                            btnSaveChanges.text = "Guardar Cambios"
                        }
                    }
                } else {
                    Toast.makeText(this, "No se pudo convertir la imagen a Base64", Toast.LENGTH_SHORT).show()
                    btnSaveChanges.isEnabled = true
                    btnSaveChanges.text = "Guardar Cambios"
                }
            }
        } else {
            Log.d("EditCommerceActivity", "Usando imagen existente: $currentImageUrl")
            updateNegocio(name, categoryName, address, currentImageUrl)
        }
    }

    private fun convertirUriABase64(uri: Uri, callback: (String?) -> Unit) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.let {
                val bytes = it.readBytes()
                if (bytes.size > 700000) {
                    Log.e("EditCommerceActivity", "Imagen demasiado grande: ${bytes.size} bytes")
                    Toast.makeText(this, "La imagen es demasiado grande, elige una más pequeña", Toast.LENGTH_SHORT).show()
                    callback(null)
                    return
                }
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                callback(base64String)
                it.close()
            } ?: run {
                Log.e("EditCommerceActivity", "No se pudo abrir el inputStream")
                callback(null)
            }
        } catch (e: Exception) {
            Log.e("EditCommerceActivity", "Error al convertir imagen a Base64", e)
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
                Log.e("EditCommerceActivity", "Fallo en la solicitud a ImgBB", e)
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
                                Log.e("EditCommerceActivity", "Respuesta de ImgBB no exitosa: $responseBody")
                                callback(null)
                            }
                        } else {
                            Log.e("EditCommerceActivity", "Respuesta no exitosa: ${response.code}")
                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("EditCommerceActivity", "Error al procesar respuesta de ImgBB", e)
                        callback(null)
                    }
                }
            }
        })
    }

    private fun updateNegocio(name: String, categoryName: String, address: String, imageUrl: String) {
        db.collection("categorias")
            .whereEqualTo("nombre", categoryName)
            .get()
            .addOnSuccessListener { snapshot ->
                val newCategoriaId = if (snapshot.isEmpty) {
                    val newCategory = db.collection("categorias").document()
                    newCategory.set(hashMapOf("nombre" to categoryName))
                    newCategory.id
                } else {
                    snapshot.documents.first().id
                }

                val negocioData = hashMapOf(
                    "nombre" to name,
                    "direccion" to address,
                    "imagenUrl" to imageUrl,
                    "categoriaId" to newCategoriaId,
                    "usuarioId" to auth.currentUser?.uid
                )

                db.collection("comercios")
                    .document(negocioId)
                    .set(negocioData)
                    .addOnSuccessListener {
                        db.collection("categorias")
                            .document(newCategoriaId)
                            .collection("negocios")
                            .document(negocioId)
                            .set(negocioData)
                            .addOnSuccessListener {
                                Log.d("EditCommerceActivity", "Negocio actualizado en Firestore")
                                Toast.makeText(this, "Negocio actualizado exitosamente", Toast.LENGTH_SHORT).show()
                                btnSaveChanges.isEnabled = true
                                btnSaveChanges.text = "Guardar Cambios"
                                setResult(RESULT_OK)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditCommerceActivity", "Error al actualizar negocio en categoría", e)
                                Toast.makeText(this, "Error al actualizar negocio: ${e.message}", Toast.LENGTH_SHORT).show()
                                btnSaveChanges.isEnabled = true
                                btnSaveChanges.text = "Guardar Cambios"
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditCommerceActivity", "Error al actualizar negocio", e)
                        Toast.makeText(this, "Error al actualizar negocio: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnSaveChanges.isEnabled = true
                        btnSaveChanges.text = "Guardar Cambios"
                    }
            }
            .addOnFailureListener { e ->
                Log.e("EditCommerceActivity", "Error al buscar categoría", e)
                Toast.makeText(this, "Error al buscar categoría: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSaveChanges.isEnabled = true
                btnSaveChanges.text = "Guardar Cambios"
            }
    }
}