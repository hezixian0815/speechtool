package com.aispeech.lite.dm.update;

import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillContext;
import com.aispeech.export.Vocab;
import com.aispeech.export.listeners.AIUpdateListener;

public interface IUpdater {
    /**
     * 上传词库
     *
     * @param vocabs   {@link Vocab}
     * @param listener {@link AIUpdateListener}
     */
    void updateVocabs(AIUpdateListener listener, Vocab... vocabs);

    /**
     * 更新产品配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link ProductContext}
     */
    void updateProductContext(AIUpdateListener listener, ProductContext context);

    /**
     * 更新技能配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link SkillContext}
     */
    void updateSkillContext(AIUpdateListener listener, SkillContext context);

}
