package com.distribuidora.distribuidoralimitada.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.distribuidora.distribuidoralimitada.databinding.ItemProductBinding // Importante: Usar la clase de Binding
import com.distribuidora.distribuidoralimitada.model.Product

class ProductAdapter(
    private val items: List<Product>,
    private val qty: MutableList<Int>,
    private val onChange: () -> Unit
) : RecyclerView.Adapter<ProductAdapter.VH>() {

    // El ViewHolder ahora contiene una instancia del Binding, no una View genérica.
    inner class VH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Se infla el layout usando la clase de Binding, que es más seguro y eficiente.
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]

        // Accedemos a las vistas a través del objeto 'binding' de forma segura.
        // No más findViewById!
        h.binding.tvName.text = p.name
        h.binding.tvPrice.text = "$${p.price}"
        h.binding.tvColdTag.visibility = if (p.coldChain) View.VISIBLE else View.GONE
        h.binding.tvQty.text = qty[pos].toString()

        h.binding.btnPlus.setOnClickListener {
            qty[pos]++ // Incrementamos la cantidad
            h.binding.tvQty.text = qty[pos].toString()
            onChange()
        }

        h.binding.btnMinus.setOnClickListener {
            if (qty[pos] > 0) {
                qty[pos]-- // Decrementamos la cantidad
                h.binding.tvQty.text = qty[pos].toString()
                onChange()
            }
        }
    }
}