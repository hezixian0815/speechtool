package com.aispeech.export;


import java.util.ArrayList;
import java.util.List;

/**
 * 可以对技能配置进行增删改
 *
 * @author hehr
 */
public class SkillContext {
    /**
     * 技能Id
     */
    private String skillId;
    /**
     * 配置操作,支持配置的删除，设定操作。可选操作，默认 #OPTION_SET
     *
     * @see #OPTION_SET
     * @see #OPTION_DELETE
     */
    private String option;
    /**
     * 配置内容 {@link Setting}
     */
    private List<Setting> settings;

    public String getSkillId() {
        return skillId;
    }

    public String getOption() {
        return option;
    }

    public List<Setting> getSettings() {
        return settings;
    }

    /**
     * 配置操作：设置配置
     */
    public static final String OPTION_SET = "set";
    /**
     * 配置操作：删除配置
     */
    public static final String OPTION_DELETE = "delete";

    public SkillContext(String skillId, String option, List<Setting> settings) {
        this.skillId = skillId;
        this.option = option;
        this.settings = settings;
    }

    private SkillContext(Builder builder) {
        this(builder.getSkillId(), builder.getOption(), builder.getSettings());
    }

    public static class Builder {

        private String skillId;

        private String option;

        private List<Setting> settings;

        public String getSkillId() {
            return skillId;
        }

        public Builder setSkillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public String getOption() {
            return option;
        }

        public Builder setOption(String option) {
            this.option = option;
            return this;
        }

        public List<Setting> getSettings() {
            return settings;
        }

        public Builder setSettings(List<Setting> settings) {
            this.settings = settings;
            return this;
        }

        public Builder setSetting(final Setting setting) {
            this.settings = new ArrayList<Setting>() {{
                add(setting);
            }};
            return this;
        }

        public SkillContext build() {
            return new SkillContext(this);
        }
    }
}
