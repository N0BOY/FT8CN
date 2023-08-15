package com.bg7yoz.ft8cn.html;

import android.annotation.SuppressLint;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.util.HashMap;

public class ImportTaskList extends HashMap<Integer, ImportTaskList.ImportTask> {

    /**
     * 获取上传的任务，以session为key
     *
     * @param session session
     * @return 任务的HTML
     */
    public String getTaskHTML(int session) {
        ImportTask task = this.get(session);
        if (task == null) {
            return GeneralVariables.getStringFromResource(R.string.null_task_html);
        }
        return task.getHtml();
    }
    public void cancelTask(int session){
        ImportTask task = this.get(session);
        if (task != null) {
           task.setStatus(ImportState.CANCELED);
        }
    }

    /**
     * 检查任务是不是在运行。
     *
     * @param session 任务ID
     * @return false 没有任务或任务结束
     */
    public boolean checkTaskIsRunning(int session) {
        ImportTask task = this.get(session);
        if (task == null) {
            return false;
        } else {
            return task.status == ImportState.STARTING || task.status == ImportState.IMPORTING;
        }
    }

    /**
     * 添加任务到列表，要确保线程安全
     *
     * @param session session
     * @param task    任务
     */
    public synchronized ImportTask addTask(int session, ImportTask task) {
        this.put(session, task);
        return task;
    }

    public ImportTask addTask(int session) {
        return addTask(session, new ImportTask(session));
    }


    enum ImportState {
        STARTING, IMPORTING, FINISHED, CANCELED
    }

    public static class ImportTask {


        int session;//session，用于记录上传会话，是一个hash
        public int count = 0;//解析出总的数据量
        public int importedCount = 0;//导入的数量
        public int readErrorCount = 0;//读取数据错误数量
        public int processCount = 0;
        public int updateCount = 0;//更新的数量
        public int invalidCount = 0;//无效的QSL
        public int newCount = 0;//新导入的数量
        public ImportState status = ImportState.STARTING;//状态:0:开始，1：运行，2：结束，3：取消
        String message = "";//任务消息描述
        String errorMsg = "";

        @SuppressLint("DefaultLocale")
        public String getHtml() {
            String htmlHeader = "<table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n";
            String htmlEnder = "</table>\n";
            String progress = String.format("<FONT COLOR=\"BLUE\">%s %.1f%%(%d/%d)</FONT>\n", GeneralVariables.getStringFromResource(R.string.import_progress_html)
                    , count == 0 ? 0 : processCount * 100f / count, processCount, count);
            String cell = "<tr><td>%s</td></tr>\n";
            String errorHtml = status == ImportState.FINISHED || status == ImportState.CANCELED ? errorMsg : "";
            String doCancelButton = status == ImportState.FINISHED || status == ImportState.CANCELED ? ""
                    : String.format("<br><a href=\"cancelTask?session=%d\"><button>%s</button></a><br>"
                       , session,GeneralVariables.getStringFromResource(R.string.import_cancel_button));
            return htmlHeader
                    + String.format(cell, progress)
                    + String.format(cell, String.format(GeneralVariables.getStringFromResource(R.string.import_read_error_count_html), readErrorCount))
                    + String.format(cell, String.format(GeneralVariables.getStringFromResource(R.string.import_new_count_html), newCount))
                    + String.format(cell, String.format(GeneralVariables.getStringFromResource(R.string.import_update_count_html), updateCount))
                    + String.format(cell, String.format(GeneralVariables.getStringFromResource(R.string.import_invalid_count_html), invalidCount))
                    + String.format(cell, String.format(GeneralVariables.getStringFromResource(R.string.import_readed_html), importedCount))
                    + String.format(cell, message)
                    + String.format(cell, errorHtml)
                    + htmlEnder
                    + doCancelButton;
        }

        public ImportTask(int session) {
            this.session = session;
        }

        public void setStatus(ImportState status) {
            this.status = status;
            setStateMSG(status);
        }

        private void setStateMSG(ImportState state) {
            switch (state) {
                case IMPORTING:
                    this.message = String.format("<FONT COLOR=\"BLUE\"><B>%s</B></FONT>"
                            ,GeneralVariables.getStringFromResource(R.string.log_importing_html));
                    break;
                case FINISHED:
                    this.message = String.format("<FONT COLOR=\"GREEN\"><B>%s</B></FONT>"
                            ,GeneralVariables.getStringFromResource(R.string.log_import_finished_html));
                    break;
                case CANCELED:
                    this.message = String.format("<FONT COLOR=\"RED\"><B>%s</B></FONT>"
                            ,GeneralVariables.getStringFromResource(R.string.import_canceled_html));
                    break;
            }
        }


    }
}
