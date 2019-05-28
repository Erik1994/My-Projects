package com.erikantonyan.socialmediaapp;

import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ViewSentPostsActivity extends AppCompatActivity {
    private ListView postsListView;
    private ArrayList<String> usernames;
    private ArrayAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private ImageView sentPostImageView;
    private TextView sentDescription;
    private ArrayList<DataSnapshot> dataSnapshots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_sent_posts);
        iniFirebase();
        initFields();
        addEventListeners();
        addClickListeners();
    }

    private void addEventListeners() {
        FirebaseDatabase.getInstance().getReference().child("my_users")
                .child(firebaseAuth.getCurrentUser().getUid()).child("receved_posts").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                dataSnapshots.add(dataSnapshot);
                String fromWhomUsername = (String) dataSnapshot.child("fromWhom").getValue();
                usernames.add(fromWhomUsername);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                int i = 0;
                for(DataSnapshot snapshot : dataSnapshots) {
                    if(snapshot.getKey().equals(dataSnapshot.getKey())) {
                        dataSnapshots.remove(i);
                        usernames.remove(i);
                    }
                    ++i;
                }
                adapter.notifyDataSetChanged();
                sentPostImageView.setImageResource(R.drawable.placeholder);
                sentDescription.setHint("Description");

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addClickListeners() {
        postsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DataSnapshot myDataSnapshot = dataSnapshots.get(position);
                String dowwnloadLink = (String) myDataSnapshot.child("imageLink").getValue();
                Picasso.get().load(dowwnloadLink).into(sentPostImageView);
                sentDescription.setText((String)myDataSnapshot.child("des").getValue());
            }
        });
       postsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
           @Override
           public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
               showAlertDialog(position);
               return false;
           }
       });

    }

    private void showAlertDialog(final int position) {
        AlertDialog.Builder builder;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("Delete entry")
                .setMessage("Are you sure you wnat to delete this entry?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        FirebaseStorage.getInstance().getReference()
                                .child("my_images").child((String) dataSnapshots.get(position)
                                .child("imageIdentifier").getValue()).delete();

                        FirebaseDatabase.getInstance().getReference().child("my_users")
                                .child(firebaseAuth.getCurrentUser().getUid())
                                .child("receved_posts").child(dataSnapshots.get(position).getKey()).removeValue();



                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void initFields() {
        postsListView = findViewById(R.id.posts_listview);
        usernames = new ArrayList<>();
        adapter = new ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,usernames);
        postsListView.setAdapter(adapter);
        sentDescription = findViewById(R.id.sentdesc);
        sentPostImageView = findViewById(R.id.sentpost_imageview);
        dataSnapshots = new ArrayList<>();


    }

    private void iniFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }
}
