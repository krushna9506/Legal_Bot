package com.legalai.assistant.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.legalai.assistant.service.NotificationHelper;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DESC = "extra_desc";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, 1);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String desc = intent.getStringExtra(EXTRA_DESC);

        NotificationHelper.showDeadlineNotification(
                context,
                taskId,
                "Court / Legal Deadline: " + (title != null ? title : ""),
                desc != null ? desc : "Statutory deadline requires your attention."
        );
    }
}
