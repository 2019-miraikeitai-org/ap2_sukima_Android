package com.example.sukima_android

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.model.Marker


class ToiletInfoWindowViewer(newActivity: Activity) : InfoWindowAdapter {

    private val infoWindowView: View
    override fun getInfoWindow(marker: Marker): View { // TextViewにTitle, Snippetをセットする.
        (infoWindowView.findViewById<View>(R.id.marker_infowindow_title) as TextView).text =
            marker.title
        (infoWindowView.findViewById<View>(R.id.marker_infowindow_snippet) as TextView).text =
            marker.snippet

        return infoWindowView
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    init {
        infoWindowView =
            newActivity.layoutInflater.inflate(R.layout.layout_marker_window, null)
    }
}