package com.dede.tester.ui.favorite

import com.baidu.mapapi.favorite.FavoriteManager
import com.baidu.mapapi.favorite.FavoritePoiInfo
import com.baidu.mapapi.model.LatLng

object FavoriteHelper {

    fun loadFavorite(): List<FavoritePoiInfo>? {
        FavoriteManager.getInstance().init()
        val allFavPois = FavoriteManager.getInstance().allFavPois as? ArrayList<FavoritePoiInfo>
        allFavPois?.sortByDescending { it.timeStamp }
        FavoriteManager.getInstance().destroy()
        return allFavPois
    }

    fun addFavorite(point: LatLng, name: String?) {
        val favoritePoiInfo = FavoritePoiInfo()
        favoritePoiInfo.pt(point)
        favoritePoiInfo.poiName(name ?: "收藏")
        FavoriteManager.getInstance().init()
        FavoriteManager.getInstance().add(favoritePoiInfo)
        FavoriteManager.getInstance().destroy()
    }

    fun update(info: FavoritePoiInfo) {
        FavoriteManager.getInstance().init()
        FavoriteManager.getInstance().updateFavPoi(info.id, info)
        FavoriteManager.getInstance().destroy()
    }

    fun delFavorite(id: String) {
        FavoriteManager.getInstance().init()
        FavoriteManager.getInstance().deleteFavPoi(id)
        FavoriteManager.getInstance().destroy()
    }
}