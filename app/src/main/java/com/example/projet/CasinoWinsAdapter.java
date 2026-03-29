package com.example.projet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CasinoWinsAdapter extends RecyclerView.Adapter<CasinoWinsAdapter.VH> {

    private final List<String> wins;

    public CasinoWinsAdapter(List<String> wins) { this.wins = wins; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_casino_win, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.combo.setText(wins.get(position));
        h.label.setText("🎉");
    }

    @Override public int getItemCount() { return wins.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView combo, label;
        VH(@NonNull View v) {
            super(v);
            combo = v.findViewById(R.id.tv_win_combo);
            label = v.findViewById(R.id.tv_win_label);
        }
    }
}
