package com.fournodes.ud.locationtest.interfaces;

import com.fournodes.ud.locationtest.objects.Fence;

import java.util.List;

/**
 * Created by Usman on 20/4/2016.
 */
public interface FenceListInterface {
    void activeFenceList(List<Fence> fenceListActive, String className);
    void allFenceList(List<Fence> fenceListAll);
}
