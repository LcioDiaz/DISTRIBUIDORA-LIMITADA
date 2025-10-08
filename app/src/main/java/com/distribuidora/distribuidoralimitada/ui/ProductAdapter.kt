package com.distribuidora.distribuidoralimitada.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.distribuidora.distribuidoralimitada.databinding.ItemProductBinding
import com.distribuidora.distribuidoralimitada.model.Product

/**
 * Adaptador para el RecyclerView que muestra la lista de productos.
 * Utiliza View Binding para una interacción más segura y eficiente con las vistas.
 *
 * @param items La lista de productos a mostrar.
 * @param qty La lista mutable que almacena la cantidad de cada producto.
 * @param onChange Una función lambda que se ejecuta cada vez que la cantidad de un producto cambia.
 */
class ProductAdapter(
    private val items: List<Product>,
    private val qty: MutableList<Int>,
    private val onChange: () -> Unit
) : RecyclerView.Adapter<ProductAdapter.VH>() {
    // El ViewHolder contiene una instancia del Binding para el layout del item.
    inner class VH(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Se infla el layout del item usando la clase de Binding generada.
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }
    // Devuelve el número total de items en la lista.
    override fun getItemCount() = items.size
     // Vincula los datos de un producto en una posición específica con las vistas del ViewHolder.

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos] // Obtiene el producto en la posición actual.
        // Accede a las vistas a través del objeto 'binding' de forma segura.
        h.binding.tvName.text = p.name
        h.binding.tvPrice.text = "$${p.price}"
        // Muestra u oculta la etiqueta "Cadena de Frío" según el producto.
        h.binding.tvColdTag.visibility = if (p.coldChain) View.VISIBLE else View.GONE
        h.binding.tvQty.text = qty[pos].toString()

        // Listener para el botón de añadir (+).
        h.binding.btnPlus.setOnClickListener {
            qty[pos]++ // Incrementa la cantidad.
            h.binding.tvQty.text = qty[pos].toString()
            onChange() // Notifica el cambio para actualizar los totales.
        }
        // Listener para el botón de restar (-).
        h.binding.btnMinus.setOnClickListener {
            if (qty[pos] > 0) {
                qty[pos]-- // Decrementa la cantidad si es mayor que cero.
                h.binding.tvQty.text = qty[pos].toString()
                onChange() // Notifica el cambio.
            }
        }
    }
}