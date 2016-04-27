package com.fournodes.ud.locationtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.fournodes.ud.locationtest.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Usman on 26/4/2016.
 */
public class MultilineInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private LayoutInflater inflater;
    private View infoWindow;

    public MultilineInfoWindowAdapter(Context context) {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        if (infoWindow == null) {
            infoWindow = inflater.inflate(R.layout.infowindow_textview, null);
        }

        TextView title = (TextView) infoWindow.findViewById(R.id.infoWindowTitle);
        TextView snippet = (TextView) infoWindow.findViewById(R.id.infoWindowSnippet);

        title.setText(marker.getTitle());
        snippet.setText(marker.getSnippet());
        return infoWindow;
    }
}
