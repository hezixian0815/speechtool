package com.aispeech.export.itn;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 词库还原的高级配置项
 */
public class Advanced {
    /**
     * 限定词库白名单内的才会encode，默认对词库---"sys.联系人"，进行 encode
     */
    protected ArrayList<String> vocabsWhiteList;
    /**
     * 限定技能白名单内的才会decode，默认为空。一般上传联系人前，设置当前技能id
     */
    protected ArrayList<String> skillIdsWhiteList;
    /**
     * 限定槽位黑名单内，在skillsWhiteList前提下，黑名单内的slot不会被还原
     */
    protected ArrayList<String> slotsBlackList;

    public Advanced() {
        reset();
    }

    public void addVocab(String name) {
        if (!vocabsWhiteList.contains(name)) {
            vocabsWhiteList.add(name);
        }
    }

    public void removeVocab(String name) {
        if (!vocabsWhiteList.contains(name)) {
            vocabsWhiteList.remove(name);
        }
    }


    public void addSkillId(String skillId) {
        if (!skillIdsWhiteList.contains(skillId)) {
            skillIdsWhiteList.add(skillId);
        }
    }

    public void removeSkillId(String skillId) {
        if (!skillIdsWhiteList.contains(skillId)) {
            skillIdsWhiteList.remove(skillId);
        }
    }

    public void addSlot(String name) {
        if (!slotsBlackList.contains(name)) {
            slotsBlackList.add(name);
        }
    }

    public void removeSlot(String name) {
        if (!slotsBlackList.contains(name)) {
            slotsBlackList.remove(name);
        }
    }

    public void reset() {
        vocabsWhiteList = new ArrayList<>(Arrays.asList(
                "sys.联系人"
        ));
        skillIdsWhiteList = new ArrayList<>();
        slotsBlackList = new ArrayList<>(Arrays.asList(
                "intent",
                "sys.序列号",
                "sys.页码"
        ));
    }
}
