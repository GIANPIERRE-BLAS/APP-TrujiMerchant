package com.trujidelivery.trujimerchantt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.databinding.ActivityViewProductsBinding
import com.trujidelivery.trujimerchantt.modelo.Producto

class ViewProductsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewProductsBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ProductosViewAdapter
    private val productos = mutableListOf<Producto>()
    private var commerceId: String = ""
    private var categoryId: String = ""

    companion object {
        const val REQUEST_ADD_PRODUCT = 1002
        const val REQUEST_EDIT_PRODUCT = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        commerceId = intent.getStringExtra("COMERCIO_ID") ?: ""
        categoryId = intent.getStringExtra("CATEGORIA_ID") ?: ""
        if (commerceId.isBlank() || categoryId.isBlank()) {
            Toast.makeText(this, "Error: Datos incompletos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        adapter = ProductosViewAdapter(
            productos,
            onEditClick = { producto ->
                val intent = Intent(this, EditProductActivity::class.java).apply {
                    putExtra("PRODUCTO_ID", producto.id)
                    putExtra("COMERCIO_ID", commerceId)
                    putExtra("CATEGORIA_ID", categoryId)
                    putExtra("nombre", producto.nombre)
                    putExtra("descripcion", producto.descripcion)
                    putExtra("precio", producto.precio)
                    putExtra("imagenUrl", producto.imagenUrl)
                    putExtra("isDiscounted", producto.isDiscounted ?: false)
                    putExtra("discountPercentage", producto.discountPercentage ?: 0.0)
                    putExtra("discountedPrice", producto.discountedPrice ?: producto.precio)
                }
                startActivityForResult(intent, REQUEST_EDIT_PRODUCT)
            },
            onDeleteClick = { producto ->
                showDeleteProductoConfirmationDialog(producto)
            }
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter
        binding.btnAddProduct.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java).apply {
                putExtra("commerceId", commerceId)
                putExtra("categoryId", categoryId)
            }
            startActivityForResult(intent, REQUEST_ADD_PRODUCT)
        }
        loadProducts()
    }

    private fun loadProducts() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("comercios")
            .document(commerceId)
            .collection("productos")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    return@addSnapshotListener
                }

                productos.clear()
                snapshot?.documents?.forEach { document ->
                    val producto = document.toObject(Producto::class.java)
                    if (producto != null) {
                        producto.id = document.id
                        producto.comercioId = commerceId
                        producto.categoriaId = categoryId

                        Log.d("ADAPTADOR", "Producto desde Firestore: nombre=${producto.nombre}, isDiscounted=${producto.isDiscounted}, discountPercentage=${producto.discountPercentage}, discountedPrice=${producto.discountedPrice}, precio=${producto.precio}")

                        val dp = producto.discountPercentage ?: 0.0
                        val dpPrice = producto.discountedPrice ?: 0.0
                        val pr = producto.precio

                        if (producto.isDiscounted && dp > 0.0) {
                            if (dpPrice <= 0.0 || dpPrice >= pr) {
                                producto.discountedPrice = pr * (1 - dp / 100)
                            }
                        } else {
                            producto.discountedPrice = pr
                        }

                        productos.add(producto)
                    }
                }
                adapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
            }
    }


    private fun showDeleteProductoConfirmationDialog(producto: Producto) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que deseas eliminar ${producto.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteProducto(producto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProducto(producto: Producto) {
        binding.progressBar.visibility = View.VISIBLE
        val batch = db.batch()
        batch.delete(
            db.collection("comercios")
                .document(commerceId)
                .collection("productos")
                .document(producto.id)
        )
        batch.delete(
            db.collection("categorias")
                .document(categoryId)
                .collection("negocios")
                .document(commerceId)
                .collection("productos")
                .document(producto.id)
        )
        batch.commit()
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Producto eliminado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al eliminar producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_PRODUCT, REQUEST_EDIT_PRODUCT -> {
                    loadProducts()
                }
            }
        }
    }
}
