package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;
import com.aispeech.export.MultiModal;

import org.json.JSONObject;

/**
 * description:同步多模态数据参数
 * author: WangBaoBao
 * created on: 2020/11/18 11:51
 */
public class MultiModalParams {

    private MultiModal mMultiModal;

    private static final String KEY_TOPIC = "topic";
    private static final String VALUE_TOPIC = "dm.context.sync";

    public MultiModalParams(MultiModal multiModal) {
        this.mMultiModal = multiModal;
    }

    public JSONObject toJSON() {
        JSONObject multiModal = JSONUtil.build(mMultiModal.toString());
        if (multiModal != null) {
            JSONUtil.putQuietly(multiModal, KEY_TOPIC, VALUE_TOPIC);
        }
        return multiModal;
    }

}
