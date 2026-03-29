package com.example.projet;

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

/** Адаптер для режима Помодоро — отдельный список задач */
public class PomodoroAdapter extends RecyclerView.Adapter<PomodoroAdapter.VH> {

    public interface OnFirstTaskChangedListener {
        void onFirstTaskChanged(int tomatoes);
    }

    private final List<TaskAdapter.Task> tasks;
    private ItemTouchHelper touchHelper;
    private OnFirstTaskChangedListener firstTaskListener;

    public PomodoroAdapter(List<TaskAdapter.Task> tasks) {
        this.tasks = tasks;
    }

    public void setTouchHelper(ItemTouchHelper helper) { this.touchHelper = helper; }

    public void setOnFirstTaskChangedListener(OnFirstTaskChangedListener l) {
        this.firstTaskListener = l;
    }

    public void onItemMoved(int from, int to) {
        Collections.swap(tasks, from, to);
        notifyItemMoved(from, to);
        if ((from == 0 || to == 0) && firstTaskListener != null && !tasks.isEmpty()) {
            firstTaskListener.onFirstTaskChanged(tasks.get(0).tomatoes);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_pomodoro, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TaskAdapter.Task task = tasks.get(position);
        h.title.setText(task.title);
        h.description.setText(task.description);
        h.tvTomatoes.setText("🍅" + task.tomatoes);

        h.btnMinus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            TaskAdapter.Task t = tasks.get(pos);
            if (t.tomatoes > 1) {
                t.tomatoes--;
                h.tvTomatoes.setText("🍅" + t.tomatoes);
                if (pos == 0 && firstTaskListener != null)
                    firstTaskListener.onFirstTaskChanged(t.tomatoes);
            }
        });

        h.btnPlus.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;
            TaskAdapter.Task t = tasks.get(pos);
            t.tomatoes++;
            h.tvTomatoes.setText("🍅" + t.tomatoes);
            if (pos == 0 && firstTaskListener != null)
                firstTaskListener.onFirstTaskChanged(t.tomatoes);
        });

        h.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && touchHelper != null) {
                touchHelper.startDrag(h);
            }
            return false;
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
                if (pos == 0 && firstTaskListener != null) {
                    firstTaskListener.onFirstTaskChanged(tasks.isEmpty() ? 0 : tasks.get(0).tomatoes);
                }
            }
        });
    }

    private void showEditDialog(android.content.Context ctx, TaskAdapter.Task task, int pos) {
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_add_task, null);
        TextInputEditText inputTitle = dialogView.findViewById(R.id.input_title);
        TextInputEditText inputDesc  = dialogView.findViewById(R.id.input_description);
        inputTitle.setText(task.title);
        inputDesc.setText(task.description);
        new AlertDialog.Builder(ctx)
                .setView(dialogView)
                .setPositiveButton(ctx.getString(R.string.btn_add), (d, w) -> {
                    String t  = inputTitle.getText() != null ? inputTitle.getText().toString().trim() : "";
                    String de = inputDesc.getText()  != null ? inputDesc.getText().toString().trim()  : "";
                    if (!t.isEmpty()) { task.title = t; task.description = de; notifyItemChanged(pos); }
                })
                .setNegativeButton(ctx.getString(R.string.btn_cancel), null)
                .show();
    }

    @Override public int getItemCount() { return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, description, dragHandle, tvTomatoes;
        MaterialButton btnMinus, btnPlus, editButton, removeButton;

        VH(@NonNull View v) {
            super(v);
            title        = v.findViewById(R.id.task_title);
            description  = v.findViewById(R.id.task_description);
            dragHandle   = v.findViewById(R.id.drag_handle);
            tvTomatoes   = v.findViewById(R.id.tv_tomatoes);
            btnMinus     = v.findViewById(R.id.btn_tomato_minus);
            btnPlus      = v.findViewById(R.id.btn_tomato_plus);
            editButton   = v.findViewById(R.id.task_edit_button);
            removeButton = v.findViewById(R.id.task_remove_button);
        }
    }
}
