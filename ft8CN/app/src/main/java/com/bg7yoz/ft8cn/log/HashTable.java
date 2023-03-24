package com.bg7yoz.ft8cn.log;
/**
 * 用于记录SWL QSO所用的哈希表类型 ，因为要记录上方的呼号，所以要有2个String KEY，HashMap并不合适，
 * 这里采用谷歌的guava:31.1-jre库
 *
 * BG7YOZ
 * 2023-03-20
 *
 */

import androidx.annotation.Nullable;

import com.google.common.collect.Table;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class HashTable implements Table {
    @Override
    public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
        return false;
    }

    @Override
    public boolean containsRow(@Nullable Object rowKey) {
        return false;
    }

    @Override
    public boolean containsColumn(@Nullable Object columnKey) {
        return false;
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return false;
    }

    @Nullable
    @Override
    public @org.checkerframework.checker.nullness.qual.Nullable Object get(@Nullable Object rowKey, @Nullable Object columnKey) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Nullable
    @Override
    public @org.checkerframework.checker.nullness.qual.Nullable Object put(Object rowKey, Object columnKey, Object value) {
        return null;
    }

    @Override
    public void putAll(Table table) {

    }

    @Nullable
    @Override
    public @org.checkerframework.checker.nullness.qual.Nullable Object remove(@Nullable Object rowKey, @Nullable Object columnKey) {
        return null;
    }

    @Override
    public Map row(Object rowKey) {
        return null;
    }

    @Override
    public Map column(Object columnKey) {
        return null;
    }

    @Override
    public Set<Cell> cellSet() {
        return null;
    }

    @Override
    public Set rowKeySet() {
        return null;
    }

    @Override
    public Set columnKeySet() {
        return null;
    }

    @Override
    public Collection values() {
        return null;
    }

    @Override
    public Map rowMap() {
        return null;
    }

    @Override
    public Map columnMap() {
        return null;
    }
}
