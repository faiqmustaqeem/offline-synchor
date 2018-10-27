package com.edgeon.faiq.synchor;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

import io.realm.Realm;

public class SplashScreenActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Realm realm;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        sharedPreferences=getSharedPreferences("app",MODE_PRIVATE);
       boolean isFirsttime=sharedPreferences.getBoolean("isFirstTime",true);
        if(isFirsttime){

            saveData();

        }
        else {
            Intent intent = new Intent(this , MainActivity.class);
            startActivity(intent);
            SplashScreenActivity.this.finish();
        }
    }

    private void saveData(){

        ProgressDialog dialog=new ProgressDialog(this);
        dialog.setTitle("Synchronizing");
        dialog.setMessage("wait...");
        dialog.show();
        realm=Realm.getDefaultInstance();
        realm.beginTransaction();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("Tasks").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Log.e("datasnapshot" , dataSnapshot.toString());
                for(DataSnapshot childsnapshot : dataSnapshot.getChildren()){

                    try{
                        TodoItemModelFirebase todoItemModelFirebase= childsnapshot.getValue(TodoItemModelFirebase.class);

                        assert todoItemModelFirebase != null;
                        TodoItemModel model=realm.createObject(TodoItemModel.class,todoItemModelFirebase.getTimestamp());
                        model.setText(todoItemModelFirebase.getText());
                        model.setSynced(true);
                    }catch (Exception e)
                    {
                        Log.e("exception" , e.toString());
                    }
                }
                editor=sharedPreferences.edit();
                editor.putBoolean("isFirstTime" , false);
                editor.apply();
                realm.commitTransaction();
                dialog.dismiss();
                Intent intent = new Intent(SplashScreenActivity.this , MainActivity.class);
                startActivity(intent);
                SplashScreenActivity.this.finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }
}
