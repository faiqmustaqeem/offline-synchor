package com.edgeon.faiq.synchor;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.MyViewHolder> {

    List<TodoItemModel> list;
    public DataAdapter(List<TodoItemModel> list)
    {
        this.list=list;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_data , viewGroup , false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        TodoItemModel model=list.get(i);
        myViewHolder.text.setText(model.getText());
        myViewHolder.date.setText(new Date(model.getTimestamp()).toString());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView text;
        TextView date;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            text=(TextView)itemView.findViewById(R.id.item_text);
            date=(TextView)itemView.findViewById(R.id.date);
        }
    }
}
