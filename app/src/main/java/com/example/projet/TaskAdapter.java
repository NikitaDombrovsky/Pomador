package com.example.projet;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Collections;
import java.util.List;

/** Адаптер для обычного режима Таймер */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    public enum Status {
        PENDING, DOING, DONE;
        public Status next() {
            Status[] v = values();
            return v[(ordinal() + 1) % v.length];
        }
    }

    public static class Task {
        public String title;
        public String description;
        public Status status = Status.PENDING;
        public int tomatoes = 1;

        public Task(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private final List<Task> tasks;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Task task = tasks.get(position);
        h.title.setText(task.title);
        h.description.setText(task.description);

        applyStatus(h.statusButton, task.status, h.itemView.getContext());
        h.statusButton.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            tasks.get(pos).status = tasks.get(pos).status.next();
            applyStatus(h.statusButton, tasks.get(pos).status, v.getContext());
        });

        h.editButton.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            showEditDialog(h.itemView.getContext(), tasks.get(pos), pos);
        });

        h.removeButton.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                tasks.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }

    private void applyStatus(MaterialButton btn, Status status, android.content.Context ctx) {
        switch (status) {
            case PENDING:
                btn.setText(ctx.getString(R.string.status_pending));
                btn.setTextColor(Color.parseColor("#E53935"));
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935")));
                break;
            case DOING:
                btn.setText(ctx.getString(R.string.status_doing));
                btn.setTextColor(Color.parseColor("#FFA000"));
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFA000")));
                break;
            case DONE:
                btn.setText(ctx.getString(R.string.status_done));
                btn.setTextColor(Color.parseColor("#43A047"));
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#43A047")));
                break;
        }
    }

    private void showEditDialog(android.content.Context ctx, Task task, int pos) {
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_task, null);
        TextInputEditText inputTitle = dialogView.findViewById(R.id.input_title);
        TextInputEditText inputDesc  = dialogView.findViewById(R.id.input_description);
        inputTitle.setText(task.title);
        inputDesc.setText(task.description);
        new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setPositiveButton(ctx.getString(R.string.btn_add), (d, w) -> {
                    String t = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
                    String de = inputDesc.getText() != null ? inputDesc.getText().toString().trim() : "";
                    if (!t.isEmpty()) { task.title = t; task.description = de; notifyItemChanged(pos); }
                })
                .setNegativeButton(ctx.getString(R.string.btn_cancel), null)
                .show();
    }

    @Override public int getItemCount() { return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, description;
        MaterialButton statusButton, editButton, removeButton;

        VH(@NonNull View v) {
            super(v);
            title        = v.findViewById(R.id.task_title);
            description  = v.findViewById(R.id.task_description);
            statusButton = v.findViewById(R.id.task_status_button);
            editButton   = v.findViewById(R.id.task_edit_button);
            removeButton = v.findViewById(R.id.task_remove_button);
        }
    }
}
