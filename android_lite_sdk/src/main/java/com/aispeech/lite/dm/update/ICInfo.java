package com.aispeech.lite.dm.update;

import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillContext;
import com.aispeech.export.Vocab;

/**
 * cinfo 接口定义
 *
 * @author hehr
 */
public interface ICInfo {
    /**
     * 上传词库
     *
     * @param vocabs {@link Vocab}
     */
    void uploadVocabs(Vocab... vocabs);

    /**
     * 上传产品配置
     *
     * @param context {@link ProductContext}
     */
    void uploadProductContext(ProductContext context);

    /**
     * 上传技能配置
     *
     * @param context {@link SkillContext}
     */
    void uploadSkillContext(SkillContext context);
}
