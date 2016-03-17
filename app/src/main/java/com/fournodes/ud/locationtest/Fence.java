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
    private Marker endMarker;
    private float radius;
    private Geofence area;
    private Circle visibleArea;
    private PendingIntent pendingIntent;
    private boolean notifyEntry;
    private boolean notifyExit;
    private boolean notifyRoaming;
    private String notifyDevice;

    public String getNotifyDevice() {
        return notifyDevice;
    }

    public void setNotifyDevice(String notifyDevice) {
        this.notifyDevice = notifyDevice;
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

    public Marker getEndMarker() {
        return endMarker;
    }

    public void setEndMarker(Marker endMarker) {
        this.endMarker = endMarker;
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

    public boolean isNotifyEntry() {
        return notifyEntry;
    }

    public void setNotifyEntry(boolean notifyEntry) {
        this.notifyEntry = notifyEntry;
    }

    public boolean isNotifyExit() {
        return notifyExit;
    }

    public void setNotifyExit(boolean notifyExit) {
        this.notifyExit = notifyExit;
    }

    public boolean isNotifyRoaming() {
        return notifyRoaming;
    }

    public void setNotifyRoaming(boolean notifyRoaming) {
        this.notifyRoaming = notifyRoaming;
    }

    public void setNotifyAll(boolean notifyAll){
        this.notifyRoaming = true;
        this.notifyExit = true;
        this.notifyEntry = true;
    }

    /**** Only removes the visible circle from the map, does not remove the actual geofence ***/
    public void removeFence() {
        centerMarker.remove();
        endMarker.remove();
        visibleArea.remove();
    }
    public int getTransitionType(){
        // 1 - Entry
        // 2 - Exit
        // 4 - Dwell

        if (notifyEntry && notifyExit && notifyRoaming)
            return (1 | 2 | 4);
        else if (notifyEntry && notifyExit)
            return 1 | 2;
        else if (notifyEntry && notifyRoaming)
            return 1 | 4;
        else if(notifyExit && notifyRoaming)
            return 2 | 4;
        else if (notifyEntry)
            return 1;
        else if (notifyExit)
            return 2;
        else if (notifyRoaming)
            return 4;
        else
            return -1; //Error
    }
}
