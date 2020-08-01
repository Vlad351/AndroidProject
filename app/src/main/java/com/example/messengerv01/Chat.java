package com.example.messengerv01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;

import com.github.library.bubbleview.BubbleTextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import android.text.format.DateFormat;
import android.widget.Toast;


public class Chat extends AppCompatActivity {
    private FloatingActionButton sendButton;
    private final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();


    private String name;
    private ListView listOfMessages;
    private FirebaseListAdapter<Message> adapter;
    private String currentChatName;
    private String Uid = null;
    private String prevSender = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentChatName = getIntent().getExtras().get("chatName").toString();

        getUsersName();

        sendButton = findViewById(R.id.btn_send);
        sendButtonOnClickListener();
        displayAllMessages();


    }

    private void sendButtonOnClickListener() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.btn_send:
                        EditText textField = findViewById(R.id.messageText2);
                        if(textField.getText().toString().equals(""))
                            return;
                        rootRef.child("Chats").child(currentChatName).child("Messages").push().setValue(
                                new Message(
                                        name,
                                        textField.getText().toString()
                                )
                        );
                        textField.setText("");
                }
            }
        });
    }

    //displayAllMessages takes all messages from firebase database appropriate chat node and displays them
    private void displayAllMessages() {
        listOfMessages = findViewById(R.id.list_messages);
        adapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.message_layout, rootRef.child("Chats").child(currentChatName).child("Messages")) {
            @Override
            protected void populateView(View v, Message model, int position) {
                TextView mess_user, mess_time;
                BubbleTextView mess_text;
                mess_user = v.findViewById(R.id.messageUser);
                mess_time = v.findViewById(R.id.messageTime);
                mess_text = v.findViewById(R.id.messageText);
                if(position>0)
                    if(!adapter.getItem(position-1).getUserName().equals(model.getUserName())) {
                        mess_user.setText(model.getUserName());
                        mess_text.setText(model.getTextMessage());
                        mess_time.setText(DateFormat.format("dd.MM.yyyy HH:mm", model.getMessageTime()));
                    }
                    else{
                        mess_user.setText("");
                        mess_text.setText(model.getTextMessage());
                        mess_time.setText("");
                    }
                else{
                    mess_user.setText(model.getUserName());
                    mess_text.setText(model.getTextMessage());
                    mess_time.setText(DateFormat.format("dd.MM.yyyy HH:mm", model.getMessageTime()));
                }
            }
        };
        listOfMessages.setAdapter(adapter);
    }

    //getUsersName gets current user's name from firebase database
    public void getUsersName() {
        rootRef.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(mAuth.getUid()).exists()){
                    name = dataSnapshot.child(mAuth.getUid()).child("name").getValue().toString();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    //onCreateOptionsMenu creates menu for current activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }


    //onOptionsItemSelected is listening for events on option menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_users:
                addUser();
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    //creates dialog to add new user to current chat
    private void addUser() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setTitle("Enter user's unique name");
        final EditText userNameField = new EditText(this);
        builder.setView(userNameField);
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userName = userNameField.getText().toString();
                if(userName.isEmpty()){
                    Toast.makeText(Chat.this, "Please write user's unique name", Toast.LENGTH_LONG).show();
                }
                else {
                    try {
                        String searchFor = userNameField.getText().toString();
                        order(searchFor);
                    }
                    catch (Exception e) {
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    //order is getting requested user name, searching for it in firebase database and adds him to a chat
    private void order(final String searchFor){
        rootRef.child("Users")
                .orderByChild("UniqueName")
                .equalTo(searchFor)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            Uid = childSnapshot.getKey();
                        }
                        if(Uid==null)
                            Toast.makeText(Chat.this, "User doesn't exist", Toast.LENGTH_LONG).show();
                        else
                            rootRef.child("Chats").child(currentChatName).child("Members").child(Uid).child("UniqueName").setValue(searchFor);
                            if(!dataSnapshot.child("Users").child(Uid).child("Chats").child(currentChatName).exists()){
                                rootRef.child("Users").child(Uid).child("Chats").child(currentChatName).setValue(new ChatClass(currentChatName));
                            }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }



}
