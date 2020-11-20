package com.dede.tester.ui.favorite

import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.baidu.mapapi.favorite.FavoritePoiInfo
import com.dede.tester.R
import com.dede.tester.ext.toast
import com.dede.tester.ui.mock.MockFragment
import kotlinx.android.synthetic.main.fragment_favorite.*


class FavoriteFragment : Fragment() {

    private val favoriteViewModel: FavoriteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoriteViewModel.list.observe(viewLifecycleOwner) {
            recycler_view.adapter = Adapter(it)
        }
        favoriteViewModel.loadFavorite()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(android.R.id.text1)
        val desc = view.findViewById<TextView>(android.R.id.text2)
    }

    inner class Adapter(val list: List<FavoritePoiInfo>) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflate =
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
            val outValue = TypedValue()
            requireContext().theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue, true
            )
            inflate.background = ContextCompat.getDrawable(requireContext(), outValue.resourceId)
            return VH(inflate)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val favoritePoiInfo = list[position]
            holder.name.text = favoritePoiInfo.poiName
            val pt = favoritePoiInfo.pt
            holder.desc.text = "${pt?.longitude}, ${pt?.latitude}"
            holder.itemView.setOnClickListener {
                favoriteViewModel.moveFirst(favoritePoiInfo)
                val bundle = Bundle()
                bundle.putParcelable(MockFragment.EXTRA_LOCATION, favoritePoiInfo.pt)
                bundle.putString(MockFragment.EXTRA_NAME, favoritePoiInfo.poiName)
                findNavController().popBackStack(R.id.nav_mock, true)
                findNavController().navigate(R.id.nav_mock, bundle)
            }
            holder.itemView.setOnLongClickListener {
                showMenu(favoritePoiInfo)
                return@setOnLongClickListener false
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private fun showMenu(info: FavoritePoiInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_edit_favorite)
            .setItems(R.array.edit_favorite_array) { _, which ->
                when (which) {
                    0 -> showRenameDialog(info)
                    1 -> showDelDialog(info)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showRenameDialog(info: FavoritePoiInfo) {
        val editText = EditText(requireContext())
        editText.setText(info.poiName)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_rename_favorite)
            .setView(editText)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val toString = editText.text.toString()
                if (TextUtils.isEmpty(toString)) {
                    toast(R.string.tip_not_empty)
                    return@setPositiveButton
                }
                info.poiName(toString)
                FavoriteHelper.update(info)
                favoriteViewModel.loadFavorite()
            }
            .show()
    }

    private fun showDelDialog(info: FavoritePoiInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_title_del_favorite)
            .setMessage(R.string.dialog_msg_del_favorite)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                FavoriteHelper.delFavorite(info.id)
                favoriteViewModel.loadFavorite()
            }
            .show()
    }

}