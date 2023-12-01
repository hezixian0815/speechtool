package com.aispeech.lite.fasp;

import com.aispeech.lite.BaseListener;

public interface FaspListener extends BaseListener {


    void onChs1DataReceived(int type, byte[] data);

    void onChs2DataReceived(int type, byte[] data);

    void onGotInputWavChan(int inputWavChan);

}
