package com.bg7yoz.ft8cn.database;

public interface OnAfterQueryConfig {
    void doOnBeforeQueryConfig(String KeyName);
    void doOnAfterQueryConfig(String KeyName,String Value);
}
