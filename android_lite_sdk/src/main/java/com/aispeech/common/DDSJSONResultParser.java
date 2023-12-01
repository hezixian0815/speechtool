package com.aispeech.common;

import org.json.JSONObject;

/**
 * dds 识别服务解析结果
 *
 * @author hehr
 */
public class DDSJSONResultParser {

    /**
     * 识别最终识别结果
     */
    private StringBuilder textBuilder;

    public DDSJSONResultParser() {
        textBuilder = new StringBuilder();
    }

    public DDSResultParseBean parse(String jsoStr) {

        DDSResultParseBean bean = new DDSResultParseBean();

        bean.jso = JSONUtil.build(jsoStr);

        if (bean.jso != null) {

            Object eof = JSONUtil.optQuietly(bean.jso, "eof");
            if (eof != null) bean.setEof((Integer) eof);


            Object text = JSONUtil.optQuietly(bean.jso, "text");

            if (text != null) {
                textBuilder.append(text);
            }

            Object var = JSONUtil.optQuietly(bean.jso, "var");

            bean.setText(textBuilder.toString() + (var == null ? "" : var));

            //修改实时识别结果
            JSONUtil.putQuietly(bean.jso , "text" ,textBuilder.toString() + (var == null ? "" : var));

            Object sessionId = JSONUtil.optQuietly(bean.jso, "sessionId");
            if (sessionId != null) bean.setSessionId((String) sessionId);

            Object recordId = JSONUtil.optQuietly(bean.jso, "recordId");
            if (recordId != null) bean.setRecordId((String) recordId);

            Object skillId = JSONUtil.optQuietly(bean.jso, "skillId");
            if (skillId != null) bean.setSkillId((String) skillId);

            Object contextId = JSONUtil.optQuietly(bean.jso, "contextId");
            if (contextId != null) bean.setContextId((String) contextId);

            Object nlu = JSONUtil.optQuietly(bean.jso, "nlu");
            if (nlu != null) bean.setNlu((JSONObject) nlu);

            Object dm = JSONUtil.optQuietly(bean.jso, "dm");
            if (dm != null) bean.setDm((JSONObject) dm);

            Object error = JSONUtil.optQuietly(bean.jso, "error");
            if (error != null) bean.setError((JSONObject) error);

        }

        return bean;

    }

    public void destroy(){
        this.textBuilder = null;
    }

    public class DDSResultParseBean {

        private JSONObject jso;

        /**
         * 中间临时识别结果
         */
        private String var;
        /**
         * 识别结果
         */
        private String text;
        /**
         * 识别结果
         */
        private String context;

        /**
         * 对话结果
         */
        private JSONObject dm;

        /**
         *
         */
        private JSONObject nlu;

        /**
         * 识别是否结束标志
         */
        private int eof;

        private String recordId;

        /**
         * 对话id
         */
        private String sessionId;

        /**
         * 技能id
         */
        private String skillId;

        private String contextId;

        private JSONObject error;

        public JSONObject getError() {
            return error;
        }

        public void setError(JSONObject error) {
            this.error = error;
        }

        public String getContextId() {
            return contextId;
        }

        public void setContextId(String contextId) {
            this.contextId = contextId;
        }


        public String getSkillId() {
            return skillId;
        }

        public void setSkillId(String skillId) {
            this.skillId = skillId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public JSONObject getJso() {
            return jso;
        }

        public void setJso(JSONObject jso) {
            this.jso = jso;
        }

        public String getVar() {
            return var;
        }

        public void setVar(String var) {
            this.var = var;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public JSONObject getDm() {
            return dm;
        }

        public void setDm(JSONObject dm) {
            this.dm = dm;
        }

        public JSONObject getNlu() {
            return nlu;
        }

        public void setNlu(JSONObject nlu) {
            this.nlu = nlu;
        }

        public int getEof() {
            return eof;
        }

        public void setEof(int eof) {
            this.eof = eof;
        }

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }


        @Override
        public String toString() {
            return "DDSJSONResultParser{" +
                    "jso=" + jso +
                    ", var='" + var + '\'' +
                    ", text='" + text + '\'' +
                    ", context='" + context + '\'' +
                    ", dm=" + dm +
                    ", nlu=" + nlu +
                    ", eof=" + eof +
                    ", recordId='" + recordId + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    ", skillId='" + skillId + '\'' +
                    ", contextId='" + contextId + '\'' +
                    ", error=" + error +
                    '}';
        }
    }


}
