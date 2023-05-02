package com.bg7yoz.ft8cn.database;

/**
 * 配置信息读取完毕的回调
 * @author BGY70Z
 * @date 2023-03-20
 */
public interface OnAfterQueryConfig {
    void doOnBeforeQueryConfig(String KeyName);
    void doOnAfterQueryConfig(String KeyName,String Value);
}
