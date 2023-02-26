package com.bg7yoz.ft8cn.log;

import java.util.ArrayList;

public interface OnQueryQSLCallsign {
     void afterQuery(ArrayList<QSLCallsignRecord> records);
}
