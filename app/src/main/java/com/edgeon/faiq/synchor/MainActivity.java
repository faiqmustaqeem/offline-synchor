package com.edgeon.faiq.synchor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {

    EditText editText;
    Button btnAdd;
    RecyclerView recyclerView;
    DataAdapter adapter;
    List<TodoItemModel> list = new ArrayList<>();
    Realm realm;
    BackgroundSynchor synchor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        realm = Realm.getDefaultInstance();   //create a object for read and write database
        synchor = new BackgroundSynchor();

        editText = findViewById(R.id.et_add_item);
        btnAdd = findViewById(R.id.btn_add);
        recyclerView = (RecyclerView) findViewById(R.id.rvData);

        btnAdd.setOnClickListener(v -> {
            realm.beginTransaction();  //open the database


            long timestamp = System.currentTimeMillis();
            TodoItemModel model = realm.createObject(TodoItemModel.class, timestamp);
            model.setText(editText.getText().toString());
            model.setSynced(false);

            synchor.addDataToFirebase(model);


            realm.commitTransaction();

            Toast.makeText(MainActivity.this, "Data added", Toast.LENGTH_SHORT).show();
            editText.setText("");

        });

        RealmResults<TodoItemModel> results = realm.where(TodoItemModel.class).findAllAsync();

        list.addAll(results);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DataAdapter(list);
        recyclerView.setAdapter(adapter);


    }


    @Override
    protected void onStart() {

        super.onStart();
        if (!BackgroundSynchor.isServiceRunning) {
            Log.e("status", "service was not running");
            startService(new Intent(MainActivity.this, BackgroundSynchor.class));
        } else {
            Log.e("status", "service already running");
        }


    }
}
