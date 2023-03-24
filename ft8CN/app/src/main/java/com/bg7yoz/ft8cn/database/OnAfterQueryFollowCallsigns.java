package com.bg7yoz.ft8cn.database;
/**
 * 查询关注的呼号回调
 * @author BGY70Z
 * @date 2023-03-20
 */

import java.util.ArrayList;

public interface OnAfterQueryFollowCallsigns {
    void doOnAfterQueryFollowCallsigns(ArrayList<String> callsigns);
}
