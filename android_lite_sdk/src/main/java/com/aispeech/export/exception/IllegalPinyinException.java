package com.aispeech.export.exception;

/**
 * 非法拼音
 *
 * @author hehr
 */
public class IllegalPinyinException extends IllegalArgumentException {

    public IllegalPinyinException() {
        super("Illegal pinyin,check if the parameter is legal pinyin");
    }

    public IllegalPinyinException(String exception) {
        super(exception);
    }
}
