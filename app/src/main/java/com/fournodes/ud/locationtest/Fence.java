package com.fournodes.ud.locationtest;

import android.app.PendingIntent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Usman on 16/2/2016.
 */
public class Fence {
    private int id;
    private String title;
    private String description;
    private Marker centerMarker;
    private Marker edgeMarker;
    private float radius;
    private Geofence area;
    private Circle visibleArea;
    private PendingIntent pendingIntent;
    private int transitionType;
    private String userId;
    private int onDevice;
    private double center_lat;
    private double center_lng;
    private double edge_lat;
    private double edge_lng;
    private String create_on;

    public String getCreate_on() {
        return create_on;
    }

    public void setCreate_on(String create_on) {
        this.create_on = create_on;
    }

    public double getCenter_lat() {
        return center_lat;
    }

    public void setCenter_lat(double center_lat) {
        this.center_lat = center_lat;
    }

    public double getCenter_lng() {
        return center_lng;
    }

    public void setCenter_lng(double center_lng) {
        this.center_lng = center_lng;
    }

    public double getEdge_lat() {
        return edge_lat;
    }

    public void setEdge_lat(double edge_lat) {
        this.edge_lat = edge_lat;
    }

    public double getEdge_lng() {
        return edge_lng;
    }

    public void setEdge_lng(double edge_lng) {
        this.edge_lng = edge_lng;
    }

    public String getUserId() {
        return userId;
    }

    public int getOnDevice() {
        return onDevice;
    }

    public void setOnDevice(int onDevice) {
        this.onDevice = onDevice;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Marker getCenterMarker() {
        return centerMarker;
    }

    public void setCenterMarker(Marker centerMarker) {
        this.centerMarker = centerMarker;
    }

    public float getRadius() {
        return radius;
    }

    public Circle getVisibleArea() {
        return visibleArea;
    }

    public void setVisibleArea(Circle visibleArea) {
        this.visibleArea = visibleArea;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Geofence getArea() {
        return area;
    }

    public void setArea(Geofence area) {
        this.area = area;
    }

    public Marker getEdgeMarker() {
        return edgeMarker;
    }

    public void setEdgeMarker(Marker edgeMarker) {
        this.edgeMarker = edgeMarker;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public int getTransitionType() {
        return transitionType;
    }

    public void setTransitionType(int transitionType) {
        this.transitionType = transitionType;
    }

    /****
     * Only removes the visible circle from the map, does not remove the actual geofence
     ***/
    public void removeFence() {
        centerMarker.remove();
        edgeMarker.remove();
        visibleArea.remove();
    }

}
