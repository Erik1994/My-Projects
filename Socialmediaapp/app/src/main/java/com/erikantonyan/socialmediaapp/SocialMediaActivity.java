package com.erikantonyan.socialmediaapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SocialMediaActivity extends AppCompatActivity {
    public static final int PICK_IMAGE_REQUEST = 1;
    private FirebaseAuth mAuth;
    private ImageView postImageView;
    private Button btnCreatePost;
    private EditText editDescription;
    private ListView usersListView;
    private Bitmap bitmap;
    private String imageIdentifier;
    private String imageDownloadLink;

    private ArrayList<String> userNames;
    private ArrayAdapter adapter;
    private ArrayList<String> userIds;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_media);
        initFirebase();
        initFields();
        setClickListeners();
    }


    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }
    private void initFields() {
        postImageView = findViewById(R.id.postImageView);
        btnCreatePost = findViewById(R.id.btnCreatePost);
        editDescription = findViewById(R.id.editDesc);
        usersListView = findViewById(R.id.usersListView);
        userNames = new ArrayList<>();
        adapter = new ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,userNames);
        usersListView.setAdapter(adapter);
        userIds = new ArrayList<>();
    }

    private void setClickListeners() {
        btnCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImageToServer();

            }
        });

        postImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                HashMap<String,String> dataMap = new HashMap<>();
                dataMap.put("fromWhom", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                dataMap.put("imageIdentifier", imageIdentifier);
                dataMap.put("imageLink",imageDownloadLink);
                dataMap.put("des",editDescription.getText().toString());
                FirebaseDatabase.getInstance().getReference().child("my_users").child(userIds.get(position))
                        .child("receved_posts").push().setValue(dataMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(SocialMediaActivity.this, "Data sent", Toast.LENGTH_SHORT).show();

                        }
                    }
                });

            }
        });
    }

    private void selectImage() {
        if(Build.VERSION.SDK_INT < 23) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent,PICK_IMAGE_REQUEST);
        } else if(Build.VERSION.SDK_INT >= 23) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},PICK_IMAGE_REQUEST);//when user does not give permission yet
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//when user already gives permission
                startActivityForResult(intent,PICK_IMAGE_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PICK_IMAGE_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri chosenImageData = data.getData();

            try{
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),chosenImageData);
                postImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void uploadImageToServer() {
        // Get the data from an ImageView as bytes
        if(bitmap != null) {
            postImageView.setDrawingCacheEnabled(true);
            postImageView.buildDrawingCache();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            final byte[] data = baos.toByteArray();
            imageIdentifier = UUID.randomUUID() + ".png"; // UUID.randomUUID().toString(); senc pet chi grel + ov inq@ arden Stringa
            UploadTask uploadTask = FirebaseStorage.getInstance().getReference().child("my_images").child(imageIdentifier).putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Toast.makeText(SocialMediaActivity.this, exception.toString(), Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...
                    Toast.makeText(SocialMediaActivity.this, "Uploading process was sccessfull", Toast.LENGTH_SHORT).show();
                    editDescription.setVisibility(View.VISIBLE);
                    FirebaseDatabase.getInstance().getReference().child("my_users").addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                            userIds.add(dataSnapshot.getKey());
                            String userName = (String) dataSnapshot.child("username").getValue();
                            userNames.add(userName);
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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


                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if(task.isSuccessful()) {
                                imageDownloadLink = task.getResult().toString();

                            }
                        }
                    });


                }
            });
        } else {
            Toast.makeText(SocialMediaActivity.this, "Please tap to existing image to upload your image", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logOut();
                break;
            case R.id.viewPostsItem:
                Intent intent = new Intent(this,ViewSentPostsActivity.class);
                startActivity(intent);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        logOut();
    }

    private void logOut() {
        mAuth.signOut();
        finish();
    }
}
