package com.bg7yoz.ft8cn.log;
/**
 * 查询呼号日志的回调。
 * @author BGY70Z
 * @date 2023-03-20
 */

import java.util.ArrayList;

public interface OnQueryQSLCallsign {
     void afterQuery(ArrayList<QSLCallsignRecord> records);
}
