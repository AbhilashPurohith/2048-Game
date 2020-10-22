package com.abhi.projects.test2048;

import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

public class OnSwipeTouchListener extends GestureDetector.SimpleOnGestureListener {

    private final class GestureListener extends SimpleOnGestureListener {

    }

    //Override this method.
    public boolean onSwipe() {
        return false;
    }

}