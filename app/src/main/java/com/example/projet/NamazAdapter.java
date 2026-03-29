package com.example.projet;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NamazAdapter extends RecyclerView.Adapter<NamazAdapter.VH> {

    private final String[][] steps;
    private boolean[] done;

    public NamazAdapter(String[][] steps, boolean[] done) {
        this.steps = steps;
        this.done  = done;
    }

    public void reset(boolean[] newDone) {
        this.done = newDone;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_namaz, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String[] step = steps[position];
        h.num.setText((position + 1) + ".");
        h.name.setText(step[0] + " " + step[1]);
        h.time.setText(step[2]);
        if (done[position]) {
            h.doneIcon.setText("✅");
            h.name.setTextColor(Color.parseColor("#43A047"));
        } else {
            h.doneIcon.setText("");
            h.name.setTextColor(Color.parseColor("#E8E8E8"));
        }
    }

    @Override public int getItemCount() { return steps.length; }

    static class VH extends RecyclerView.ViewHolder {
        TextView num, name, time, doneIcon;
        VH(@NonNull View v) {
            super(v);
            num      = v.findViewById(R.id.tv_namaz_num);
            name     = v.findViewById(R.id.tv_namaz_name);
            time     = v.findViewById(R.id.tv_namaz_time);
            doneIcon = v.findViewById(R.id.tv_namaz_done);
        }
    }
}
