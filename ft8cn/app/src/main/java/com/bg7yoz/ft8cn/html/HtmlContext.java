package com.bg7yoz.ft8cn.html;
/**
 * Http服务内容的出框架。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.database.Cursor;

import com.bg7yoz.ft8cn.BuildConfig;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

public class HtmlContext {
    private static final String HTML_HEAD = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"> <html><head><title>FT8CN</title>\n" +
            " <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=2.0, user-scalable=yes\" /> "+
            "<style type=\"text/css\">\n" +
            " <!--\n" +
            "  BODY {\n" +
            "\tBACKGROUND-COLOR: #ffffff\n" +
            " }\n" +
            "  A {\tTEXT-DECORATION: none }\n" +
            "  A:visited {\tCOLOR: #0000cf; TEXT-DECORATION: none }\n" +
            "  A:link {\tCOLOR: #0000cf; TEXT-DECORATION: none }\n" +
            "  A:active {\tCOLOR: #0000cf; TEXT-DECORATION: underline }\n" +
            "  A:hover {\tCOLOR: #0000cf; TEXT-DECORATION: underline }\n" +
            "  OL {\tCOLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif }\n" +
            "  UL {\tCOLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif }\n" +
            "  P {\tCOLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif }\n" +
            "  BODY {\tCOLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif }\n" +
            "  FIELDSET {max-width:  fit-content}\n" +
            "  TD {\tCOLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif; FONT-SIZE: 8pt}\n" +
            "  TR {\tBACKGROUND-COLOR: WHITE;COLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif }\n" +
            "  TR.bbb {\tBACKGROUND-COLOR: #DDF0FE; COLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif }\n" +
            "  TH {\tBACKGROUND-COLOR: C8C2FF;COLOR: #333333; FONT-FAMILY: tahoma,helvetica,sans-serif;FONT-SIZE: 8pt; }\n" +
            "  FONT.title {\tBACKGROUND-COLOR: white; COLOR: #363636; FONT-FAMILY:tahoma,helvetica,verdana,lucida console,utopia; FONT-SIZE: 10pt; FONT-WEIGHT: bold }\n" +
            "  FONT.sub {\tBACKGROUND-COLOR: white; COLOR: #000000; FONT-FAMILY:tahoma,helvetica,verdana,lucida console,utopia; FONT-SIZE: 10pt }\n" +
            "  FONT.layer {\tCOLOR: #ff0000; FONT-FAMILY: courrier,sans-serif,arial,helvetica; FONT-SIZE: 8pt; TEXT-ALIGN: left }\n" +
            "  TD.title {\tBACKGROUND-COLOR: #6200EE; COLOR: #FFFFFF; FONT-FAMILY:tahoma,helvetica,verdana,lucida console,utopia; FONT-SIZE: 10pt; FONT-WEIGHT: bold; HEIGHT: 20px; TEXT-ALIGN: left}\n" +
            "  TD.sub {\tBACKGROUND-COLOR: #DCDCDC; COLOR: #555555; FONT-FAMILY: tahoma,helvetica,verdana,lucida console,utopia; FONT-SIZE: 10pt; FONT-WEIGHT: bold; HEIGHT: 18px; TEXT-ALIGN: left }\n" +
            "  TD.content {\tBACKGROUND-COLOR: white; COLOR: #000000; FONT-FAMILY:tahoma,arial,helvetica,verdana,lucida console,utopia; FONT-SIZE: 8pt; TEXT-ALIGN: left; VERTICAL-ALIGN: middle }\n" +
            "  TD.default {\t COLOR: #000000; FONT-FAMILY:tahoma,arial,helvetica,verdana,lucida console,utopia; FONT-SIZE: 8pt; }\n" +
            "  TD.bbb {\tBACKGROUND-COLOR: #EDFFFE; COLOR: #000000; FONT-FAMILY:tahoma,arial,helvetica,verdana,lucida console,utopia; FONT-SIZE: 8pt; }\n" +
            "  TD.border {\tBACKGROUND-COLOR: #cccccc; COLOR: black; FONT-FAMILY:tahoma,helvetica,verdana,lucida console,utopia; FONT-SIZE: 10pt; HEIGHT: 25px }\n" +
            "  TD.border-HILIGHT {\tBACKGROUND-COLOR: #ffffcc; COLOR: black; FONT-FAMILY:verdana,arial,helvetica,lucida console,utopia; FONT-SIZE: 10pt; HEIGHT: 25px }\n" +
            //"  TH.default {\tBACKGROUND-COLOR: WHITE; COLOR: #333333; FONT-FAMILY:tahoma,arial,helvetica,verdana,lucida console,utopia; FONT-SIZE: 8pt; }\n" +
            "-->\n" +
            "</style>\n" +
            "</head>\n" +
            "\n";
    private static final String HTML_TITLE = "<table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"><tr><td class=\"title\" colspan=\"15\">" +
            "Welcome to FT8CN "+ BuildConfig.VERSION_NAME+"</a></td></tr><tr><td class=\"default\" colspan=\"15\"><a href=/>"
                    +GeneralVariables.getStringFromResource(R.string.html_return)
            +"</a></td></tr></table>\n";
    private static final String HTML_FOOTER = "<table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
            "<tr><td class=\"title\">BG7YOZ</td></tr><tr><td class=\"default\" colspan=\"15\">" +
            "<a href=/>"+GeneralVariables.getStringFromResource(R.string.html_return)+"</a></td></tr></table>\n";

    private static String HTML_BODY(String context) {
        return "<body>" + HTML_TITLE + "<br>"+context+"\n<br>" + HTML_FOOTER + "</body></html>";
    }

    public static String HTML_STRING(String context) {
        return HTML_HEAD + HTML_BODY(context);
    }


    public static String DEFAULT_HTML() {
        return HTML_STRING("<table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/debug>"
                + GeneralVariables.getStringFromResource(R.string.html_track_operation_information) +"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/showHash>"
                        +GeneralVariables.getStringFromResource(R.string.html_track_callsign_hash_table)
                +"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/newMessage>"
                        +GeneralVariables.getStringFromResource(R.string.html_trace_parsed_messages)+"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/callsignGrid>"
                        +GeneralVariables.getStringFromResource(R.string.html_trace_callsign_and_grid_correspondence_table)
                +"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/message>"
                        +GeneralVariables.getStringFromResource(R.string.html_query_swl_message)
                +"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/QSOSWLMSG>"
                    +GeneralVariables.getStringFromResource(R.string.html_query_qso_swl)
                +"</a></td></tr>" +

                "<tr><td class=\"default\" colspan=\"15\"><a href=/config>"
                        +GeneralVariables.getStringFromResource(R.string.html_query_configuration_information)
                +"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/allTable>"
                        +GeneralVariables.getStringFromResource(R.string.html_query_all_table)+"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/followCallsigns>"
                        +GeneralVariables.getStringFromResource(R.string.html_manage_tracking_callsign)+"</a></td></tr>" +
                //"<tr><td class=\"default\" colspan=\"15\"><a href=/QSLCallsigns>"
                //        +GeneralVariables.getStringFromResource(R.string.html_manage_communication_callsigns)+"</a></td></tr>" +

                "<tr><td class=\"default\" colspan=\"15\"><a href=/showQSLCallsigns>"
                        +GeneralVariables.getStringFromResource(R.string.html_show_communication_callsigns)+"</a></td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/getCallsignQTH>"
                +GeneralVariables.getStringFromResource(R.string.html_callsign_qth)+"</a></td></tr>" +

                "<tr><td class=\"default\" colspan=\"15\"><a href=/QSOLogs>"//查询日志
                +GeneralVariables.getStringFromResource(R.string.html_query_logs)+"</a></td></tr>" +

                "<tr><td class=\"default\" colspan=\"15\"><a href=/QSLTable>"
                        +GeneralVariables.getStringFromResource(R.string.html_export_log)
                +"</a>"+GeneralVariables.getStringFromResource(R.string.html_to_the_third_party)+"</td></tr>" +
                "<tr><td class=\"default\" colspan=\"15\"><a href=/ImportLog>"
                        +GeneralVariables.getStringFromResource(R.string.html_import_log)
                +"</a>"+GeneralVariables.getStringFromResource(R.string.html_from_jtdx_lotw)+"</td></tr>" +

                "</table>");
    }

    /**
     * 生成表格的内容
     * @param sb html
     * @param s 内容
     * @return html
     */
    public static StringBuilder tableCell(StringBuilder sb, String... s){
        for (String c: s) {
            sb.append(String.format("<td>%s</td>",c));
        }
        sb.append("\n");
       return sb;
    }

    /**
     * 生成表格标题
     * @param sb html
     * @param s 标题
     * @return html
     */
    public static StringBuilder tableCellHeader(StringBuilder sb, String... s){
        for (String c: s) {
            sb.append(String.format("<th>%s</th>",c));
        }
        sb.append("\n");
        return sb;
    }
    @SuppressLint("DefaultLocale")
    public static StringBuilder tableKeyRow(StringBuilder sb , Boolean darkMode, String key, int value){
        sb.append(String.format("<tr %s><td>%s</td><td>%d</td></tr>\n"
                ,darkMode ? "class=\"bbb\"" : ""
                ,key,value));
        return sb;
    }

    public static StringBuilder tableKeyRow(StringBuilder sb ,Boolean darkMode,String key,String value){
        sb.append(String.format("<tr %s><td>%s</td><td>%s</td></tr>\n"
                ,darkMode ? "class=\"bbb\"" : ""
                ,key,value));
        return sb;
    }

    public static StringBuilder tableRowBegin(StringBuilder sb){
       return tableRowBegin(sb,false,false);
    }
    public static StringBuilder tableRowBegin(StringBuilder sb,Boolean alignCenter,boolean darkMode){
       sb.append(String.format("<tr %s %s>"
               ,alignCenter?"align=center":""
                ,darkMode ? "class=\"bbb\"":""));
       return sb;
    }
    public static StringBuilder tableRowEnd(StringBuilder sb){
        sb.append("</tr>");
        return sb;
    }

    public static StringBuilder tableBegin(StringBuilder sb){
        return tableBegin(sb,false,1,true);
    }
    public static StringBuilder tableBegin(StringBuilder sb,boolean hasBorder,boolean fullWidth){
        return tableBegin(sb,hasBorder,1,fullWidth);
    }
    @SuppressLint("DefaultLocale")
    public static StringBuilder tableBegin(StringBuilder sb, boolean hasBorder, int cellpadding,boolean fullWidth){
        sb.append(String.format("<table bgcolor=#a1a1a1 %s cellpadding=\"%d\" cellspacing=\"0\" %s >"
                ,hasBorder ? "border=\"1\"" :""
                ,cellpadding
                ,fullWidth ? "width=\"100%\"":""));
        return sb;
    }

    public static StringBuilder tableEnd(StringBuilder sb){
        sb.append("</table>");
        return sb;
    }
    public static String ListTableContext(Cursor cursor,boolean fullWidth){
        return ListTableContext(cursor,false,0,fullWidth);
    }
    public static String ListTableContext(Cursor cursor,boolean hasBorder,int cellpadding ,boolean fullWidth) {
        StringBuilder result = new StringBuilder();
        HtmlContext.tableBegin(result,hasBorder,cellpadding,fullWidth).append("\n");

        //写字段名
        HtmlContext.tableRowBegin(result);
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            HtmlContext.tableCellHeader(result,cursor.getColumnName(i));
        }
        HtmlContext.tableRowEnd(result).append("\n");
        int order=0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,false,order % 2 !=0);
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                HtmlContext.tableCell(result,(cursor.getString(i)!=null) ? cursor.getString(i) :"");
            }
            HtmlContext.tableRowEnd(result).append("\n");
            order++;
        }
        HtmlContext.tableEnd(result).append("\n");
        cursor.close();
        return result.toString();
    }

}
