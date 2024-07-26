package com.example.idsign.recycleView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.idsign.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private Context context;

    public TaskAdapter(Context context, List<Task> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.taskDescription.setText(task.getDescription());
        if (task.isCompleted()) {
            holder.taskProgressBar.setVisibility(View.GONE);
            holder.taskCompletedIcon.setVisibility(View.VISIBLE);
        } else {
            holder.taskProgressBar.setVisibility(View.VISIBLE);
            holder.taskCompletedIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskDescription;
        ProgressBar taskProgressBar;
        ImageView taskCompletedIcon;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskProgressBar = itemView.findViewById(R.id.taskProgressBar);
            taskCompletedIcon = itemView.findViewById(R.id.taskCompletedIcon);
        }
    }

    public void updateTask(int position, boolean isCompleted) {
        taskList.get(position).setCompleted(isCompleted);
        notifyItemChanged(position);
    }
}
