package com.aispeech.lite;

public enum SemanticType {

    /**
     * 离线导航
     *
     * @deprecated 请参考 {@link #BUILDIN}
     */
    @Deprecated
    NAVI(1),

    /**
     * DUI本地技能
     */
    DUI(1 << 1),

    /**
     * 本地内置语义，与AIDUI
     * {@link #AIDUI}
     */
    BUILDIN(1 << 2),


    /**
     * 本地内置语义
     */
    AIDUI(1 << 2),

    /**
     * 本地内置语义bcdv2，与 AIDUI 互斥
     */
    BCDV2(1 << 3),

    /**
     * 混合语义：离线导航+DUI本地技能
     *
     * @deprecated 请参考 {@link #MIX}
     */
    @Deprecated
    MIX_NAVI_DUI(NAVI.getType() | DUI.getType()),

    /**
     * 混合语义：内置语义+DUI本地技能
     */
    MIX_BUILDIN_DUI(BUILDIN.getType() | DUI.getType()),

    /**
     * 混合语义：内置语义+离线导航
     */
    MIX_BUILDIN_NAVI(BUILDIN.getType() | NAVI.getType()),

    /**
     * 混合语义：目前包含离线导航+DUI本地技能，后续继续扩展
     */
    MIX(NAVI.getType() | DUI.getType() | BUILDIN.getType()),

    /**
     * 混合语义：目前包含离线bcdv2+DUI本地技能，后续继续扩展
     */
    MIX_BCDV2_DUI(DUI.getType() | BCDV2.getType());


    private int type;

    SemanticType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
