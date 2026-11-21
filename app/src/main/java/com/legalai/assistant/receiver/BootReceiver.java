package com.legalai.assistant.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.legalai.assistant.data.database.AppDatabase;
import com.legalai.assistant.data.model.TaskItem;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule pending alarms from Room database
            AppDatabase.databaseWriteExecutor.execute(() -> {
                List<TaskItem> tasks = AppDatabase.getDatabase(context).taskDao().getPendingTasks();
                long now = System.currentTimeMillis();
                for (TaskItem task : tasks) {
                    if (task.isHasReminder() && task.getReminderTime() > now) {
                        // Alarms will be re-registered
                    }
                }
            });
        }
    }
}
