package com.edgeon.faiq.synchor;

import android.app.Service;
import android.content.Intent;
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
import com.novoda.merlin.Merlin;

import io.realm.Realm;
import io.realm.RealmResults;

public class BackgroundSynchor extends Service {

    public static boolean isServiceRunning = false;
    Realm realm;
    private DatabaseReference mDatabase;
    Handler handler;
    Merlin merlin;


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

        isServiceRunning = true;

        init();
        setConnectivityCallback();
        setFirebaseUpdateListener();

        return START_STICKY;
    }

    private void init() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        realm = Realm.getDefaultInstance();
        merlin = new Merlin.Builder().withConnectableCallbacks().withBindableCallbacks().build(this);
        merlin.bind();
    }

    private void setConnectivityCallback() {
        merlin.registerConnectable(() -> {
            /*

            *** for future use , when we need to remove firebase ***
            sendUnsynchedDataToFirebase();


            */
            updateData();
        });
    }

    // when user is connected to internet
    private void setFirebaseUpdateListener() {

        mDatabase.child("Tasks").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                realm.beginTransaction();
                TodoItemModelFirebase todoItemModelFirebase = dataSnapshot.getValue(TodoItemModelFirebase.class);
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
    }

    // when user was not connecte dto internet , update data when user connects
    public void updateData() {

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


    public void addData(TodoItemModel model, long timestamp) {


        if (realm == null) {
            realm = Realm.getDefaultInstance();
        }
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance().getReference();
        }
        addInRealm(model, timestamp);
        addInFirebase(model);

    }

    private void addInRealm(TodoItemModel model, long timestamp) {
        realm.beginTransaction();
        TodoItemModel todoItemModel = realm.createObject(TodoItemModel.class, timestamp);
        todoItemModel.setText(model.getText());
        todoItemModel.setSynced(false);
        realm.commitTransaction();
    }

    private void addInFirebase(TodoItemModel model) {

        TodoItemModelFirebase todoItemModelFirebase = new TodoItemModelFirebase();
        todoItemModelFirebase.setTimestamp(model.getTimestamp());
        todoItemModelFirebase.setText(model.getText());
        todoItemModelFirebase.setSynced(model.isSynced());


        mDatabase.child("Tasks").push().setValue(todoItemModelFirebase).addOnSuccessListener(aVoid -> {

            realm.beginTransaction();
            model.setSynced(true);
            realm.copyToRealmOrUpdate(model);
            realm.commitTransaction();


        }).addOnFailureListener(e -> {

        });


    }


    private void sendUnsynchedDataToFirebase() {


        realm = Realm.getDefaultInstance();
        realm.beginTransaction();

        RealmResults<TodoItemModel> list = realm.where(TodoItemModel.class)
                .equalTo("isSynced", false)
                .findAll();


        realm.commitTransaction();
        mDatabase = FirebaseDatabase.getInstance().getReference();


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

                if (finalI == list.size() - 1) {
                    updateData();
                }

            }).addOnFailureListener(e -> {
                if (finalI == list.size() - 1) {
                    updateData();
                }
            });
        }


    }


}
