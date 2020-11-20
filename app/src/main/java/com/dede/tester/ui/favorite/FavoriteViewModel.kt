package com.dede.tester.ui.favorite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.baidu.mapapi.favorite.FavoritePoiInfo

class FavoriteViewModel : ViewModel() {

    val list = MutableLiveData<List<FavoritePoiInfo>>()

    fun loadFavorite() {
        list.value = FavoriteHelper.loadFavorite() ?: emptyList()
    }

    fun moveFirst(info: FavoritePoiInfo) {
        FavoriteHelper.update(info)
    }

}