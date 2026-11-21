package com.legalai.assistant.ui.tasks;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.legalai.assistant.R;
import com.legalai.assistant.data.model.TaskItem;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface TaskActionListener {
        void onToggleCompleted(TaskItem task, boolean isCompleted);
        void onDeleteTask(TaskItem task);
    }

    private final Context context;
    private List<TaskItem> taskList;
    private final TaskActionListener listener;

    public TaskAdapter(Context context, List<TaskItem> taskList, TaskActionListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    public void updateList(List<TaskItem> newList) {
        this.taskList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDesc.setText(task.getDescription());
        holder.tvDueDate.setText(task.getDueDate() != null && !task.getDueDate().isEmpty() ? "Due: " + task.getDueDate() : "No deadline set");

        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(task.isCompleted());

        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(context.getResources().getColor(R.color.text_tertiary));
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(context.getResources().getColor(R.color.text_primary));
        }

        String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
        holder.tvPriority.setText(priority);
        if ("HIGH".equals(priority)) {
            holder.tvPriority.setBackgroundResource(R.drawable.bg_tag_citation);
            holder.tvPriority.setTextColor(context.getResources().getColor(R.color.citation_tag_text));
        } else {
            holder.tvPriority.setBackgroundResource(R.drawable.bg_tag_statute);
            holder.tvPriority.setTextColor(context.getResources().getColor(R.color.statute_tag_text));
        }

        holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onToggleCompleted(task, isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteTask(task));
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbCompleted;
        TextView tvTitle;
        TextView tvDesc;
        TextView tvPriority;
        TextView tvDueDate;
        ImageButton btnDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompleted = itemView.findViewById(R.id.cbTaskCompleted);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvDueDate = itemView.findViewById(R.id.tvTaskDueDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}
