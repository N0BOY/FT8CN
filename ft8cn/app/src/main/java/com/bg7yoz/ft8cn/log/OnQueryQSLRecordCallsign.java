package com.bg7yoz.ft8cn.log;
/**
 * 查询通联日志的回调。
 * @author BGY70Z
 * @date 2023-03-20
 */

import java.util.ArrayList;

public interface OnQueryQSLRecordCallsign {
     void afterQuery(ArrayList<QSLRecordStr> records);
}
