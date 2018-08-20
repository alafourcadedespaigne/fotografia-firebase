package py.com.android.ale.fotografias;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {


    private static final int RC_GALLERY = 21;
    private static final int RC_CAMERA = 22;

    // Only Android 6.0 or above
    private static final int RP_CAMERA = 121;
    private static final int RP_STORAGE = 122;

    public static final String IMAGE_DIRECTORY = "/MyPhotoApp";
    public static final String MY_PHOTO = "my_photo";

    public static final String PATH_PROFILE = "profile";
    public static final String PATH_PHOTO_URL = "photoUrl";


    @BindView(R.id.imgPhoto)
    AppCompatImageView imgPhoto;
    @BindView(R.id.btnDelete)
    ImageButton btnDelete;
    @BindView(R.id.container)
    ConstraintLayout container;
    private TextView mTextMessage;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    // Storage the path of the camera image
    private String currentPhotoPath;

    // Contain the image url in the gallery
    private Uri photoSelectedUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        configFirebase();
    }

    private void configFirebase() {
        storageReference = FirebaseStorage.getInstance().getReference();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference().child(PATH_PROFILE).child(PATH_PHOTO_URL);
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_gallery:
                    mTextMessage.setText(R.string.main_label_gallery);

                    fromGallery();

                    return true;


                case R.id.navigation_camera:
                    mTextMessage.setText(R.string.main_label_camera);
                    return true;
            }
            return false;
        }
    };


    private void fromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RC_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RC_GALLERY:
                    if (data != null) {
                        photoSelectedUri = data.getData();

                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                                    photoSelectedUri);

                            imgPhoto.setImageBitmap(bitmap);
                            btnDelete.setVisibility(View.GONE);
                            mTextMessage.setText(R.string.main_message_question_upload);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case RC_CAMERA:
                    break;
            }
        }
    }

    @OnClick(R.id.btnUpload)
    public void onUploadPhoto() {

        StorageReference profileReferences = storageReference.child(PATH_PROFILE);

        StorageReference photoReference = profileReferences.child(MY_PHOTO);

        photoReference.putFile(photoSelectedUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Snackbar.make(container, R.string.main_message_upload_success, Snackbar.LENGTH_LONG).show();

                Uri downloadUri = taskSnapshot.getDownloadUrl();

                savePhotoUrl(downloadUri);

                btnDelete.setVisibility(View.VISIBLE);

                mTextMessage.setText(R.string.main_message_done);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(container, R.string.main_message_upload_failure,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void savePhotoUrl(Uri downloadUri) {
        databaseReference.setValue(downloadUri.toString());
    }

    @OnClick(R.id.btnDelete)
    public void onDeletePhoto() {

        storageReference.child(PATH_PROFILE).child(MY_PHOTO).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        databaseReference.removeValue();
                        imgPhoto.setImageBitmap(null);
                        btnDelete.setVisibility(View.GONE);
                        Snackbar.make(container, R.string.main_message_delete_success,
                                Snackbar.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Snackbar.make(container, R.string.main_message_delete_failure,
                        Snackbar.LENGTH_LONG).show();
            }
        });

    }
}
