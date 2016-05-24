package com.fournodes.ud.locationtest.objects;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Usman on 16/2/2016.
 */
public class Fence implements Comparable<Fence> {

    private int fenceId;
    private String title;
    private String description;
    private double centerLat;
    private double centerLng;
    private double edgeLat;
    private double edgeLng;
    private float radius;
    private String assignment;
    private int onDevice;
    private Circle circle;
    private int notifyId;
    private int lastEvent = 2;
    private int distanceFromEdge;
    private int isActive;

    private Marker centerMarker;
    private Marker edgeMarker;

    public Marker getEdgeMarker() {
        return edgeMarker;
    }

    public void setEdgeMarker(Marker edgeMarker) {
        this.edgeMarker = edgeMarker;
    }

    public Marker getCenterMarker() {
        return centerMarker;
    }

    public void setCenterMarker(Marker centerMarker) {
        this.centerMarker = centerMarker;
    }

    public int getNotifyId() {
        return notifyId;
    }

    public void setNotifyId(int notifyId) {
        this.notifyId = notifyId;
    }

    public String getAssignment() {return assignment;}

    public void setAssignment(String assignment) {this.assignment = assignment;}

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public int getDistanceFromEdge() {
        return distanceFromEdge;
    }

    public void setDistanceFromEdge(int distanceFromEdge) {this.distanceFromEdge = distanceFromEdge;}

    public int getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(int lastEvent) {
        this.lastEvent = lastEvent;
    }

    public double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(double centerLat) {
        this.centerLat = centerLat;
    }

    public double getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(double centerLng) {
        this.centerLng = centerLng;
    }

    public double getEdgeLat() {
        return edgeLat;
    }

    public void setEdgeLat(double edgeLat) {
        this.edgeLat = edgeLat;
    }

    public double getEdgeLng() {
        return edgeLng;
    }

    public void setEdgeLng(double edgeLng) {
        this.edgeLng = edgeLng;
    }

    public int getOnDevice() {
        return onDevice;
    }

    public void setOnDevice(int onDevice) {
        this.onDevice = onDevice;
    }

    public int getFenceId() {
        return fenceId;
    }

    public void setFenceId(int fenceId) {
        this.fenceId = fenceId;
    }

    public float getRadius() {
        return radius;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    public void setRadius(float radius) {
        this.radius = radius;
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


    /****
     * Only removes the visible circle from the map, does not remove the actual geofence
     ***/
    public void removeFence() {
        circle.remove();
    }


    @Override
    public int compareTo(Fence fence) {

        if (distanceFromEdge > fence.distanceFromEdge) {
            return 1;
        }
        else if (distanceFromEdge < fence.distanceFromEdge) {
            return -1;
        }
        else {
            return 0;
        }
    }
}
