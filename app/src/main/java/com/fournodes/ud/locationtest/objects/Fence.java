package com.fournodes.ud.locationtest.objects;

import android.app.PendingIntent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Usman on 16/2/2016.
 */
public class Fence implements Comparable<Fence> {
    private int id;

    private String userId;
    private String title;
    private String description;
    private String create_on;

    private Marker centerMarker;
    private Marker edgeMarker;

    private double center_lat;
    private double center_lng;
    private double edge_lat;
    private double edge_lng;
    private int radius;

    private Circle visibleArea;
    private PendingIntent pendingIntent;
    private Geofence area;
    private int transitionType;
    private int onDevice;

    private int lastEvent = 2;
    private int distanceFrom;
    private int isActive;

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public int getDistanceFrom() {
        return distanceFrom;
    }

    public void setDistanceFrom(int distanceFrom) {
        this.distanceFrom = distanceFrom;
    }

    public int getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(int lastEvent) {
        this.lastEvent = lastEvent;
    }

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

    public int getRadius() {
        return radius;
    }

    public Circle getVisibleArea() {
        return visibleArea;
    }

    public void setVisibleArea(Circle visibleArea) {
        this.visibleArea = visibleArea;
    }

    public void setRadius(float radius) {
        this.radius = (int) Math.ceil(radius);
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

    public void setPendingIntent(PendingIntent pendingIntent) {this.pendingIntent = pendingIntent;}

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


    @Override
    public int compareTo(Fence fence) {

        if (distanceFrom > fence.distanceFrom) {
            return 1;
        }
        else if (distanceFrom < fence.distanceFrom) {
            return -1;
        }
        else {
            return 0;
        }
    }
}
