package com.bg7yoz.ft8cn.database;

/**
 * 保存配置信息的回调
 * @author BGY70Z
 * @date 2023-03-20
 */
public interface OnAfterWriteConfig {
    void doOnAfterWriteConfig(boolean writeDone);
}
