package com.example.messengerv01;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class SettingsActivity extends AppCompatActivity {
    private Button update;
    private EditText et;
    private TextView tv;
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String currentUserID = mAuth.getCurrentUser().getUid();
    private String name;
    private String currName;
    private String uniqName = null;
    private String random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_settings);
        tv = findViewById(R.id.uniq_id);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        try {
            if (getIntent().getExtras().get("from").toString().equals("Login"))
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        catch (Exception e) {
        }
        checkForNewName();
        et = findViewById(R.id.change_name_et);
        update = findViewById(R.id.apply_changes_button);
        updateClickListener();

    }

    //if button update pressed submit a new name for user
    private void updateClickListener() {
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getId()==update.getId()){
                    name = et.getText().toString();
                    if(name.equals("")) {
                        Toast.makeText(SettingsActivity.this, "Please fill all fields",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Log.d("myLog", "Set new name run");
                        rootRef.child("Users").child(currentUserID).child("name").setValue(name);
                        closeKeyBoard();
                        Toast.makeText(SettingsActivity.this, "Changed", Toast.LENGTH_SHORT).show();
                        try {
                            if (getIntent().getExtras().get("from").toString().equals("Login"))
                                sendUserToMainActivity();
                        }
                        catch (Exception e) {
                        }

                    }
                }
            }
        });
    }

    //checkForNewName sets to editText current user's name (if exists) and calls onNameChanged()
    private void checkForNewName(){
        rootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("Users").child(currentUserID).child("name").exists()) {
                    currName = dataSnapshot.child("Users").child(currentUserID).child("name").getValue().toString();
                    et.setText(currName);
                    onNameChanged(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    //onNameChanged creates new UniqueName for user and sets it to textView
    private void onNameChanged(DataSnapshot dataSnapshot){
        //set user's unique name
        String currName = dataSnapshot.child("Users").child(currentUserID).child("name").getValue().toString();
        if(dataSnapshot.child("Users").child(currentUserID).child("UniqueName").exists()){
            uniqName = dataSnapshot.child("Users").child(currentUserID).child("UniqueName").getValue().toString();
            if(!currName.equals(uniqName.substring(0, uniqName.length() - 5))){
                rootRef.child("Users").child(currentUserID).child("UniqueName").setValue(currName + '@' + randomGen());
            }
        }
        else {
            rootRef.child("Users").child(currentUserID).child("UniqueName").setValue(currName + randomGen());
        }
        tv.setText("    Your unique name is: " + uniqName);
        ArrayList<String> arr = new ArrayList<>();
        for(DataSnapshot postSnapshot : dataSnapshot.child("Users").child(currentUserID).child("Chats").getChildren()){
            arr.add(postSnapshot.getKey());
        }
        for(String chatName : arr){
            rootRef.child("Chats").child(chatName).child("Members").child(currentUserID).child("UniqueName").setValue(uniqName);
        }
    }

    // closeKeyBoard closes virtual keyboard
    private void closeKeyBoard() {
        View view = this.getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    //sendUserToMainActivity sends user to MainActivity
    private void sendUserToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
    }

    //randomGen generates random 4-digits number
    private String randomGen() {
        random = getRandFromPythonServer();
        if(random!=null)
            return random;
        Random random = new Random();
        String randNum = String.format("%04d", random.nextInt(10000));
        Log.d("rand", randNum);
        return randNum;
    }

    private String getRandFromPythonServer() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Socket s = new Socket("localhost", 9090);//connection to server

                    System.out.println("connected");

                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                    out.write("1");
                    out.flush();

                    random = in.readLine();

                    in.close();
                    out.close();
                    s.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return random;
    }
}
