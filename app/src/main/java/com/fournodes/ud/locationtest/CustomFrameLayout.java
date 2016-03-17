package com.fournodes.ud.locationtest;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by Usman on 16/2/2016.
 */
public class CustomFrameLayout extends FrameLayout {
    public CustomFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ((MainActivity) getContext()).delegate.mapDragStart();
                break;

            case MotionEvent.ACTION_UP:
                ((MainActivity) getContext()).delegate.mapDragStop();
                break;
        }

        return super.dispatchTouchEvent(ev);
    }
}
