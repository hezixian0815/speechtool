package com.aispeech.export;

/**
 * dm 暴露给外部信息
 */
public class DmInfo {
    private String skillId;
    private String skill;
    private String sessionId;
    private String recordId;
    private boolean endSkillDm;
    private String errId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public boolean isEndSkillDm() {
        return endSkillDm;
    }

    public void setEndSkillDm(boolean endSkillDm) {
        this.endSkillDm = endSkillDm;
    }

    public String getErrId() {
        return errId;
    }

    public void setErrId(String errId) {
        this.errId = errId;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }
}
