package com.trujidelivery.trujimerchantt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NegocioAdapter(
    private val negocios: List<Negocio>,
    private val onNegocioEdit: (Negocio) -> Unit,
    private val onNegocioDelete: (Negocio) -> Unit,
    private val onNegocioClick: (Negocio) -> Unit
) : RecyclerView.Adapter<NegocioAdapter.NegocioViewHolder>() {

    class NegocioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDireccion: TextView = itemView.findViewById(R.id.tvDireccion)
        val ivImagen: ImageView = itemView.findViewById(R.id.ivImagen)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NegocioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_negocio, parent, false)
        return NegocioViewHolder(view)
    }

    override fun onBindViewHolder(holder: NegocioViewHolder, position: Int) {
        val negocio = negocios[position]
        holder.tvNombre.text = negocio.nombre
        holder.tvDireccion.text = negocio.direccion
        Glide.with(holder.itemView.context)
            .load(negocio.imagenUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivImagen)
        holder.btnEdit.setOnClickListener { onNegocioEdit(negocio) }
        holder.btnDelete.setOnClickListener { onNegocioDelete(negocio) }
        holder.itemView.setOnClickListener { onNegocioClick(negocio) }
    }

    override fun getItemCount(): Int = negocios.size
}
data class Negocio(
    var id: String = "",
    var nombre: String = "",
    var direccion: String = "",
    var imagenUrl: String = "",
    var categoriaId: String = "",
    var usuarioId: String = ""
)