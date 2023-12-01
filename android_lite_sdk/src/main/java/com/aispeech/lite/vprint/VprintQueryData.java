package com.aispeech.lite.vprint;


import com.aispeech.common.Log;

import java.util.List;

public class VprintQueryData {

    private int[] modelSize;
    private byte[][] modelBin;

    public int num() {
        return modelSize == null ? 0 : modelSize.length;
    }

    public int[] getModelSize() {
        return modelSize;
    }

    public void setModelSize(int[] modelSize) {
        this.modelSize = modelSize;
    }

    public byte[][] getModelBin() {
        return modelBin;
    }

    public void setModelBin(byte[][] modelBin) {
        this.modelBin = modelBin;
    }

    public VprintQueryData(int[] modelSize, byte[][] modelBin) {
        this.modelSize = modelSize;
        this.modelBin = modelBin;
    }

    public static VprintQueryData toVprintQueryData(List<VprintSqlEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            Log.d("VprintQueryData", "entityList is Empty");
            return new VprintQueryData(new int[0], new byte[0][0]);
        } else {
            int size = entityList.get(0) != null && entityList.get(0).getData() != null ?
                    entityList.get(0).getData().length : 100;

            int[] modelSize = new int[entityList.size()];
            byte[][] modelBin = new byte[entityList.size()][size];
            for (int i = 0; i < entityList.size(); i++) {
                if (entityList.get(i) != null && entityList.get(i).getData() != null) {
                    modelBin[i] = entityList.get(i).getData();
                    modelSize[i] = modelBin[i].length;
                } else {
                    Log.d("VprintQueryData", "data in entityList is null");
                }
            }
            return new VprintQueryData(modelSize, modelBin);
        }

    }
}
