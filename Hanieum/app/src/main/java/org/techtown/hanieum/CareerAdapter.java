package org.techtown.hanieum;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CareerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ArrayList<Career> items = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View view = inflater.inflate(R.layout.career_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        Career item = items.get(position);
        ((ViewHolder) viewHolder).setItem(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(Career item) {
        items.add(item);
    }

    public void setItems(ArrayList<Career> items) {
        this.items = items;
    }

    public Career getItem(int position) {
        return items.get(position);
    }

    public void setItem(int position, Career item) {
        items.set(position, item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView delete;

        public ViewHolder(View view) {
            super(view);
            delete = view.findViewById(R.id.delete);

            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    items.remove(getLayoutPosition());
                    notifyDataSetChanged();
                }
            });
        }

        public void setItem(Career item) {
        }
    }

}
