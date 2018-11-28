package com.scirra;

/**
 * Created by Iain on 13/04/2018.
 */

public class TaskCounter extends Object {
    private int mValue;
    private boolean mFailed = false;
    public TaskCounter(int value) {
        mValue = value;
    }
    public void decrement() {
        mValue--;
    }
    public void fail() {
        mFailed = true;
    }
    public boolean hasFailed() {
        return mFailed;
    }
    public boolean isComplete() {
        return mValue <= 0;
    }
}
