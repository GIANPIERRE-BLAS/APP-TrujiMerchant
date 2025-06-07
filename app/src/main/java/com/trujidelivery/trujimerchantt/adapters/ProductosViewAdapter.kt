package com.trujidelivery.trujimerchantt

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.trujidelivery.trujimerchantt.modelo.Producto
import android.util.Log

class ProductosViewAdapter(
    private var productos: MutableList<Producto>,
    private val onEditClick: (Producto) -> Unit,
    private val onDeleteClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosViewAdapter.ProductoViewHolder>() {

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImagen: ImageView = itemView.findViewById(R.id.ivImagen)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvPrecioOriginal: TextView = itemView.findViewById(R.id.tvPrecioOriginal)
        val tvPrecioFinal: TextView = itemView.findViewById(R.id.tvPrecioFinal)
        val tvDescuento: TextView = itemView.findViewById(R.id.tvDescuento)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]

        val isD = producto.isDiscounted
        val dp = producto.discountPercentage ?: 0.0
        val dpPrice = producto.discountedPrice ?: 0.0
        val pr = producto.precio

        Log.d("ADAPTADOR", "Producto: ${producto.nombre}, isD=$isD, dp=$dp, dpPrice=$dpPrice, pr=$pr")

        holder.tvNombre.text = producto.nombre
        holder.tvDescripcion.text = producto.descripcion

        Glide.with(holder.itemView.context)
            .load(producto.imagenUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(holder.ivImagen)

        if (isD && dp > 0.0 && dpPrice > 0.0 && dpPrice < pr) {
            holder.tvPrecioOriginal.text = "S/ ${String.format("%.2f", pr)}"
            holder.tvPrecioOriginal.visibility = View.VISIBLE
            holder.tvPrecioOriginal.paintFlags = holder.tvPrecioOriginal.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            holder.tvPrecioFinal.text = "S/ ${String.format("%.2f", dpPrice)}"
            holder.tvPrecioFinal.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_dark))

            holder.tvDescuento.text = "-${String.format("%.0f", dp)}%"
            holder.tvDescuento.visibility = View.VISIBLE
        } else {
            holder.tvPrecioOriginal.visibility = View.GONE
            holder.tvDescuento.visibility = View.GONE

            holder.tvPrecioFinal.text = "S/ ${String.format("%.2f", pr)}"
            holder.tvPrecioFinal.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        }

        holder.btnEdit.setOnClickListener { onEditClick(producto) }
        holder.btnDelete.setOnClickListener { onDeleteClick(producto) }
    }


    override fun getItemCount(): Int = productos.size

    fun updateProductos(nuevosProductos: List<Producto>) {
        productos.clear()
        productos.addAll(nuevosProductos)
        notifyDataSetChanged()
    }

    fun removeProducto(position: Int) {
        if (position in productos.indices) {
            productos.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
