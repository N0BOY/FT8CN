package com.bg7yoz.ft8cn.log;

import java.util.ArrayList;

public interface OnQueryQSLRecordCallsign {
     void afterQuery(ArrayList<QSLRecordStr> records);
}
