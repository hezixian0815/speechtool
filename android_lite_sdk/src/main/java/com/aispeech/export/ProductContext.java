package com.aispeech.export;

import java.util.ArrayList;
import java.util.List;

/**
 * 可以对产品配置进行增删改。
 *
 * @author hehr
 */
public class ProductContext {

    public static class Builder {

        private String option;


        private List<Setting> settings;

        /**
         * 获取配置操作
         *
         * @return String
         */
        public String getOption() {
            return option;
        }

        /**
         * 配置操作,支持配置的删除，设定操作。可选操作，默认 #OPTION_SET
         * @param option  配置操作
         * @see #OPTION_SET
         * @see #OPTION_DELETE
         * @return {@link Builder}
         */
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

        public ProductContext build() {
            return new ProductContext(this);
        }
    }

    /**
     * 配置操作：设置配置
     */
    public static final String OPTION_SET = "set";
    /**
     * 配置操作：删除配置
     */
    public static final String OPTION_DELETE = "delete";

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

    public ProductContext(String option, List<Setting> setting) {
        this.option = option;
        this.settings = setting;
    }

    private ProductContext(Builder builder) {
        this(builder.getOption(), builder.getSettings());
    }

    /**
     * 获取配置操作
     *
     * @return String
     */
    public String getOption() {
        return option;
    }

    /**
     * 获取配置内容
     *
     * @return List
     */
    public List<Setting> getSettings() {
        return settings;
    }

}
