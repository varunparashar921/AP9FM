package com.versatilemobitech.ap9fm;

/**
 * Created by Santosh on 3/15/2016.
 */
public interface RadioCallBacks {

    void startRadio();

    void stopRadio();

    boolean isRadioPlaying();

    void addOnRadioActionChange(OnRadioActionChangeListener onRadioActionChangeListener);

    void removeRadioActionChange(OnRadioActionChangeListener onRadioActionChangeListener);

}
