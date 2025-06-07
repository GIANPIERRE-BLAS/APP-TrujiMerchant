package com.trujidelivery.trujimerchantt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trujidelivery.trujimerchantt.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val auth = FirebaseAuth.getInstance()
    private var isNavigating = false

    companion object {
        const val REQUEST_CREATE_COMMERCE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        if (auth.currentUser == null) {
            Toast.makeText(this, "Por favor, inicia sesión primero", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigationView()
        showWelcomeScreen()

        binding.btnCreateCommerce.setOnClickListener {
            val intent = Intent(this, RegisterCommerceActivity::class.java)
            startActivityForResult(intent, REQUEST_CREATE_COMMERCE)
        }

        binding.btnViewNegocios.setOnClickListener {
            startActivity(Intent(this, BusinessListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
        showWelcomeScreen()
    }

    private fun setupBottomNavigationView() {
        binding.bottomNavigationView.selectedItemId = R.id.menu_home
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            if (isNavigating) return@setOnItemSelectedListener false

            when (item.itemId) {
                R.id.menu_home -> {
                    showWelcomeScreen()
                    true
                }
                R.id.menu_negocios -> {
                    isNavigating = true
                    startActivity(Intent(this, BusinessListActivity::class.java))
                    true
                }
                R.id.menu_productos -> {
                    checkForBusinesses()
                    false
                }
                R.id.menu_profile -> {
                    isNavigating = true
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun showWelcomeScreen() {
        binding.progressBar.visibility = View.GONE
        binding.welcomeMessage.text = "Hola, bienvenido. En TrujiMerchant podrás crear tu negocio y subir tus productos para delivery."
        binding.welcomeMessage.visibility = View.VISIBLE
        binding.btnCreateCommerce.visibility = View.VISIBLE
        binding.btnViewNegocios.visibility = View.VISIBLE
        binding.recyclerViewNegocios.visibility = View.GONE
        binding.tvProductCount.visibility = View.GONE
    }

    private fun checkForBusinesses() {
        if (isNavigating) return

        val usuarioId = auth.currentUser?.uid ?: return
        isNavigating = true
        binding.progressBar.visibility = View.VISIBLE

        val db = FirebaseFirestore.getInstance()
        db.collection("comercios")
            .whereEqualTo("usuarioId", usuarioId)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE

                if (snapshot.isEmpty || snapshot.documents.isEmpty()) {
                    Toast.makeText(this, "Primero crea un negocio para ver productos", Toast.LENGTH_SHORT).show()
                    binding.bottomNavigationView.selectedItemId = R.id.menu_home
                    isNavigating = false
                    return@addOnSuccessListener
                }

                val negocioDocument = snapshot.documents.firstOrNull()
                val negocio = negocioDocument?.toObject(Negocio::class.java)?.apply {
                    id = negocioDocument.id
                }

                if (negocio != null && !negocio.id.isNullOrBlank() && !negocio.categoriaId.isNullOrBlank()) {
                    val intent = Intent(this, ViewProductsActivity::class.java).apply {
                        putExtra("COMERCIO_ID", negocio.id)
                        putExtra("CATEGORIA_ID", negocio.categoriaId)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Error al cargar información del negocio", Toast.LENGTH_SHORT).show()
                    binding.bottomNavigationView.selectedItemId = R.id.menu_home
                    isNavigating = false
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al verificar negocios: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.bottomNavigationView.selectedItemId = R.id.menu_home
                isNavigating = false
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREATE_COMMERCE && resultCode == RESULT_OK) {
            startActivity(Intent(this, BusinessListActivity::class.java))
        }
    }
}