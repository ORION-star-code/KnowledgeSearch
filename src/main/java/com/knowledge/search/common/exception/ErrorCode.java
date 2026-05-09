package com.knowledge.search.common.exception;

public final class ErrorCode {

    public static final String ARTICLE_NOT_FOUND = "ARTICLE_NOT_FOUND";
    public static final String CATEGORY_NOT_FOUND = "CATEGORY_NOT_FOUND";
    public static final String TAG_NOT_FOUND = "TAG_NOT_FOUND";
    public static final String CATEGORY_NAME_EXISTS = "CATEGORY_NAME_EXISTS";
    public static final String TAG_NAME_EXISTS = "TAG_NAME_EXISTS";
    public static final String CATEGORY_IN_USE = "CATEGORY_IN_USE";
    public static final String TAG_IN_USE = "TAG_IN_USE";
    public static final String SYNC_FAIL_NOT_FOUND = "SYNC_FAIL_NOT_FOUND";
    public static final String INVALID_SYNC_PAYLOAD = "INVALID_SYNC_PAYLOAD";

    private ErrorCode() {
    }
}
