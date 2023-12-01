package com.aispeech.net.http;

import java.io.IOException;

public class NoImplementHttpException extends IOException {

    public NoImplementHttpException() {
        super();
    }

    public NoImplementHttpException(String s) {
        super(s);
    }

    public NoImplementHttpException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NoImplementHttpException(Throwable throwable) {
        super(throwable);
    }
}
