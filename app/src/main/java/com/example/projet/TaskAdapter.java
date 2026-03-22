package com.example.projet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public static class Task {
        public String title;
        public String description;

        public Task(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private final List<Task> tasks;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.title.setText(task.title);
        holder.description.setText(task.description);
        holder.removeButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                tasks.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description;
        MaterialButton removeButton;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            removeButton = itemView.findViewById(R.id.task_remove_button);
        }
    }
}
