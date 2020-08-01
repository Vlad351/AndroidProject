package com.example.messengerv01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {

    private GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build();
    private GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    public  ListView listOfChats;
    private FloatingActionButton fb;
    private GoogleSignInAccount account;

    private String currentUserID;
    private FirebaseUser user;
    private String uniqName;
    private BroadcastReceiver mReceiver;
    private IntentFilter intentFilter;
    private FirebaseListAdapter<ChatClass> mAdapter;
    private String currentChatName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chats_screen);
        account = GoogleSignIn.getLastSignedInAccount(this);
        mAuth = FirebaseAuth.getInstance();
        checkIfSignedIn();
        user = mAuth.getCurrentUser();
        currentUserID = user.getUid();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        rootRef = FirebaseDatabase.getInstance().getReference();
        floatingButtonClicked();
        startService();
        displayAllChats();
        chatClicked();
        checkBattery();
    }

    private void floatingButtonClicked() {
        fb = findViewById(R.id.fb);
        fb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestNewChat();
            }
        });
    }




    //startService starts NotificationService
    private void startService(){
        startService(new Intent(this, NotificationService.class));
    }

    //checkIfSignedIn checks if user is signed in and if not sends him to log in activity
    private void checkIfSignedIn(){
        if(mAuth.getCurrentUser()==null||account==null)
            startActivity(new Intent(MainActivity.this, Login.class));
    }

    //checkBattery is listening to a battery state and if 20% and battery isn't charging toast message appears
    private void checkBattery (){
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction()))
                    if(intent.getIntExtra("level", 0) ==20 && plugged != BatteryManager.BATTERY_STATUS_CHARGING )
                        Toast.makeText(context, "Battery level low", Toast.LENGTH_SHORT).show();
            }
        };
    }

    //chatClicked is listening for click on certain item in list of all chats and if clicked sends user to appropriate chat
    private void chatClicked() {
        listOfChats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String currentChatName;
                currentChatName = ((ChatClass) parent.getItemAtPosition(position)).getChatName();
                Intent intent = new Intent(MainActivity.this, Chat.class);
                intent.putExtra("chatName", currentChatName);
                startActivity(intent);
            }
        });
        listOfChats.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
           @Override
           public boolean onItemLongClick(final AdapterView<?> parent, View arg1, final int position, long id) {
               final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
               builder.setTitle("Are you sure you want to quit this chat?");
               builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       dialog.cancel();
                   }
               });
               builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {

                       currentChatName = ((ChatClass) parent.getItemAtPosition(position)).getChatName();
                       rootRef.child("Chats").child(currentChatName).child("Members").child(currentUserID).setValue(null);
                       rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                           @Override
                           public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                               if(!dataSnapshot.child("Chats").child(currentChatName).child("Members").exists()){
                                   rootRef.child("Chats").child(currentChatName).setValue(null);
                               }
                           }
                           @Override
                           public void onCancelled(@NonNull DatabaseError databaseError) {
                           }
                       });
                       DatabaseReference item = mAdapter.getRef(position);
                       item.removeValue();
                   }
               });
               builder.show();
               return true;
           }

    });
    }


    //displayAllChats is getting all chats user has access to and displays them
    private void displayAllChats() {
        listOfChats = findViewById(R.id.chats_list);
        mAdapter = new FirebaseListAdapter<ChatClass>(this, ChatClass.class, R.layout.chat_element_layout, rootRef.child("Users").child(currentUserID).child("Chats")) {
            @Override
            protected void populateView(View v, ChatClass model, int position) {
                TextView chatName =  v.findViewById(R.id.chatName);
                chatName.setText(model.getChatName());
            }
        };
        listOfChats.setAdapter(mAdapter);
    }

    // onStart runs after onCreate and registering broadcastReceiver
    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, intentFilter);
    }

    //onStop runs before app is closed and unregistering broadcast receiver
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    //onCreateOptionsMenu creates menu as three dots in upper right corner
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    //onOptionSelected is listening for events in menu items and acts appropriately
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.log_out:
                signOut();
                startActivity(new Intent(this, Login.class));
                return true;
            case R.id.prof_sett:
                sendUserToSettingsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    // A new group creating dialog function
    private void requestNewChat() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
        builder.setTitle("Enter chat name");

        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint("Enter chat name ");
        builder.setView(groupNameField);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String chatName = groupNameField.getText().toString();
                if(chatName.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please write chat name", Toast.LENGTH_LONG).show();
                }
                if(chatName.contains(".")||chatName.contains("#")||chatName.contains("$")||chatName.contains("[")||chatName.contains("]")){
                    Toast.makeText(MainActivity.this, "Illegal chat name\n Forbidden:  .  [ ] $ # ", Toast.LENGTH_LONG).show();
                }
                else {
                    createNewChat(chatName);
                }
            }
        });

        builder.show();
    }

    //createNewChat gets name of chat user requested to create and creates new chat in firebase database
    private void createNewChat(final String groupName) {
        rootRef.child("Users").child(currentUserID).child("UniqueName").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                uniqName = dataSnapshot.getValue().toString();
                rootRef.child("Chats").child(groupName).child("Members").child(currentUserID).child("UniqueName").setValue(uniqName);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        rootRef.child("Chats").child(groupName).setValue(new ChatClass(groupName))
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(MainActivity.this, groupName + " is created", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        rootRef.child("Users").child(currentUserID).child("Chats").child(groupName).setValue(new ChatClass( groupName));
        rootRef.child("Users").child(currentUserID).child("Chats").child(groupName).child("MessagesCount").setValue(0);
    }

    //signOut is signing out the user from google account
    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                });
    }

    //sendUserToSettingsActivity sends user to settings activity
    private void sendUserToSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }


}
