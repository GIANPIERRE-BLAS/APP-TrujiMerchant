package com.trujidelivery.trujimerchantt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.databinding.ActivityBusinessListBinding

class BusinessListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBusinessListBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val negocios = mutableListOf<Negocio>()
    private lateinit var negocioAdapter: NegocioAdapter

    companion object {
        const val REQUEST_CREATE_COMMERCE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Por favor, inicia sesión primero", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupAdapter()
        loadBusinesses()

        binding.btnCreateCommerce.setOnClickListener {
            val intent = Intent(this, RegisterCommerceActivity::class.java)
            startActivityForResult(intent, REQUEST_CREATE_COMMERCE)
        }
    }

    private fun setupAdapter() {
        negocioAdapter = NegocioAdapter(
            negocios,
            onNegocioEdit = { negocio ->
                Log.d("BusinessListActivity", "Editando negocio: ${negocio.id}, ${negocio.nombre}")
                if (negocio.id.isNotBlank() && negocio.categoriaId.isNotBlank()) {
                    val intent = Intent(this, EditCommerceActivity::class.java).apply {
                        putExtra("NEGOCIO_ID", negocio.id)
                        putExtra("CATEGORIA_ID", negocio.categoriaId)
                        putExtra("nombre", negocio.nombre)
                        putExtra("direccion", negocio.direccion)
                        putExtra("imagenUrl", negocio.imagenUrl)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Error: Datos del negocio incompletos", Toast.LENGTH_SHORT).show()
                }
            },
            onNegocioDelete = { negocio ->
                showDeleteNegocioConfirmationDialog(negocio)
            },
            onNegocioClick = { negocio ->
                val intent = Intent(this, ViewProductsActivity::class.java).apply {
                    putExtra("COMERCIO_ID", negocio.id)
                    putExtra("CATEGORIA_ID", negocio.categoriaId)
                }
                startActivity(intent)
            }
        )
        binding.rvBusinesses.apply {
            layoutManager = LinearLayoutManager(this@BusinessListActivity)
            adapter = negocioAdapter
        }
    }

    private fun loadBusinesses() {
        val usuarioId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.rvBusinesses.visibility = View.GONE

        db.collection("comercios")
            .whereEqualTo("usuarioId", usuarioId)
            .get()
            .addOnSuccessListener { snapshot ->
                negocios.clear()
                for (document in snapshot.documents) {
                    val negocio = document.toObject(Negocio::class.java)?.apply {
                        id = document.id
                    }
                    if (negocio != null && negocio.nombre.isNotBlank()) {
                        negocios.add(negocio)
                    }
                }
                negocioAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                binding.rvBusinesses.visibility = View.VISIBLE
                if (negocios.isEmpty()) {
                    binding.tvTitle.text = "No tienes negocios creados"
                } else {
                    binding.tvTitle.text = "Tus Negocios"
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("BusinessListActivity", "Error al cargar negocios", e)
                Toast.makeText(this, "Error al cargar negocios: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.tvTitle.text = "Error al cargar negocios"
            }
    }

    private fun showDeleteNegocioConfirmationDialog(negocio: Negocio) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Negocio")
            .setMessage("¿Estás seguro de que deseas eliminar ${negocio.nombre}? Esta acción eliminará también todos los productos asociados.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteNegocio(negocio)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteNegocio(negocio: Negocio) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("comercios")
            .document(negocio.id)
            .collection("productos")
            .get()
            .addOnSuccessListener { productSnapshot ->
                val batch = db.batch()
                for (productDoc in productSnapshot.documents) {
                    batch.delete(productDoc.reference)
                    batch.delete(
                        db.collection("categorias")
                            .document(negocio.categoriaId)
                            .collection("negocios")
                            .document(negocio.id)
                            .collection("productos")
                            .document(productDoc.id)
                    )
                }
                batch.delete(db.collection("comercios").document(negocio.id))
                batch.delete(
                    db.collection("categorias")
                        .document(negocio.categoriaId)
                        .collection("negocios")
                        .document(negocio.id)
                )
                batch.commit()
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Negocio eliminado exitosamente", Toast.LENGTH_SHORT).show()
                        loadBusinesses()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Log.e("BusinessListActivity", "Error al eliminar negocio", e)
                        Toast.makeText(this, "Error al eliminar negocio: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e("BusinessListActivity", "Error al obtener productos para eliminar", e)
                Toast.makeText(this, "Error al eliminar negocio: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREATE_COMMERCE && resultCode == RESULT_OK) {
            loadBusinesses()
        }
    }
}