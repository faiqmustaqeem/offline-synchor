package com.edgeon.faiq.synchor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.realm.Realm;
import io.realm.RealmResults;

public class BackgroundSynchor extends Service {

    public static boolean isServiceRunning = false;
    Realm realm;
    private DatabaseReference mDatabase;
    RealmResults<TodoItemModel> listItems;

    Handler handler;
    Runnable runnable;
    boolean working;
    String netAddress = null;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("LocalService", "Received start id " + startId + ": " + intent);
        Log.e("service", "service started here");
        isServiceRunning = true;


        mDatabase = FirebaseDatabase.getInstance().getReference();
        realm = Realm.getDefaultInstance();


        mDatabase.child("Tasks").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                realm.beginTransaction();
                TodoItemModelFirebase todoItemModelFirebase = dataSnapshot.getValue(TodoItemModelFirebase.class);
                Log.e("data changed", dataSnapshot.toString());
                TodoItemModel model = new TodoItemModel();
                assert todoItemModelFirebase != null;
                model.setSynced(true);
                model.setTimestamp(todoItemModelFirebase.getTimestamp());
                model.setText(todoItemModelFirebase.getText());

                realm.copyToRealmOrUpdate(model);

                realm.commitTransaction();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        handler = new Handler();
        runnable = () -> {


            if (isStatusChanged()) {

                Log.e("status", "status change...");
                try {
                    netAddress = new NetTask().execute("www.google.com").get();

                    if (netAddress.equals("")) {
                        Log.e("internet", "not working");
                    } else {

                        Log.e("internet", "working");

                        sendUnsynchedDataToFirebase();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }


            } else {

                Log.e("status", "no status change...");

            }
            handler.postDelayed(runnable, 5000);
        };

        handler.postDelayed(runnable, 5000);
        return START_STICKY;
    }

    public void updateData() {

        saveData();
    }

    private void saveData() {

        realm = Realm.getDefaultInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("Tasks").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                realm.beginTransaction();

                for (DataSnapshot childsnapshot : dataSnapshot.getChildren()) {

                    try {
                        TodoItemModelFirebase todoItemModelFirebase = childsnapshot.getValue(TodoItemModelFirebase.class);

                        assert todoItemModelFirebase != null;
                        TodoItemModel model = new TodoItemModel();

                        model.setText(todoItemModelFirebase.getText());
                        model.setTimestamp(todoItemModelFirebase.getTimestamp());
                        model.setSynced(true);
                        realm.copyToRealmOrUpdate(model);


                    } catch (Exception e) {
                        Log.e("exception", e.toString());
                    }

                }

                realm.commitTransaction();


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private boolean isStatusChanged() {
        if (isNetworkAvailable(getApplicationContext())) {
            if (working) {
                working = true;
                return false;
            }
            working = true;
            return true;
        }


        if (working) {
            working = false;
            return true;
        } else {
            working = false;
            return false;
        }

    }

    public void addDataToFirebase(TodoItemModel model) {
        try {
            netAddress = new NetTask().execute("www.google.com").get();
            if (netAddress.equals("")) {
                Log.e("internet", "not working");
            } else {
                Log.e("internet", "working");

                TodoItemModelFirebase todoItemModelFirebase = new TodoItemModelFirebase();
                todoItemModelFirebase.setTimestamp(model.getTimestamp());
                todoItemModelFirebase.setText(model.getText());
                todoItemModelFirebase.setSynced(model.isSynced());
                if (realm == null) {
                    realm = Realm.getDefaultInstance();
                }
                if (mDatabase == null) {
                    mDatabase = FirebaseDatabase.getInstance().getReference();
                }
                mDatabase.child("Tasks").push().setValue(todoItemModelFirebase).addOnSuccessListener(aVoid -> {

                    realm.beginTransaction();
                    model.setSynced(true);
                    realm.copyToRealmOrUpdate(model);
                    realm.commitTransaction();


                }).addOnFailureListener(e -> {
                    Log.e("failure", e.toString());

                });
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

    private void sendUnsynchedDataToFirebase() {

        Log.e("service", "sendUnsynchedDataToFirebase");
        realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        RealmResults<TodoItemModel> list = realm.where(TodoItemModel.class)
                .equalTo("isSynced", false)
                .findAll();


        realm.commitTransaction();
        mDatabase = FirebaseDatabase.getInstance().getReference();


        Log.e("list_size", list.size() + "");

        if (list.size() == 0) {
            updateData();
        }

        for (int i = 0; i < list.size(); i++) {
            TodoItemModel model = list.get(i);
            TodoItemModelFirebase todoItemModelFirebase = new TodoItemModelFirebase();
            assert model != null;
            todoItemModelFirebase.setSynced(model.isSynced());
            todoItemModelFirebase.setText(model.getText());
            todoItemModelFirebase.setTimestamp(model.getTimestamp());
            int finalI = i;


            mDatabase.child("Tasks").push().setValue(todoItemModelFirebase).addOnSuccessListener(aVoid -> {
                realm.beginTransaction();
                model.setSynced(true);
                realm.copyToRealmOrUpdate(model);
                realm.commitTransaction();
                Log.e("data sendUnsynchedData", model.getText());

                if (finalI == list.size() - 1) {
                    updateData();
                }

            }).addOnFailureListener(e -> {
                Log.e("failure", e.toString());
                if (finalI == list.size() - 1) {
                    updateData();
                }
            });
        }


    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static class NetTask extends AsyncTask<String, Integer, String> {


        @Override
        protected String doInBackground(String... params) {
            InetAddress addr = null;
            try {
                addr = InetAddress.getByName(params[0]);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return "";
            }
            return addr.getHostAddress();
        }
    }

}
