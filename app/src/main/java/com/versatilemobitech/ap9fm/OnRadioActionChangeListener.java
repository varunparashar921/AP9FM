package com.versatilemobitech.ap9fm;

/**
 * Created by Santosh on 3/15/2016.
 */
public interface OnRadioActionChangeListener {

    void onRadioStarted();

    void onRadioStopped();

    void onRadioBuffering();

    void onRadioError(String errorMsg);

}
