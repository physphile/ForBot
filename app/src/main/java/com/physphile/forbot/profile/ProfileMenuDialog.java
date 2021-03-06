package com.physphile.forbot.profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.physphile.forbot.ClassHelper;
import com.physphile.forbot.R;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;
import static com.physphile.forbot.Constants.AUTH_ACTIVITY_CODE;
import static com.physphile.forbot.Constants.FILE_IMAGE_AVATAR;
import static com.physphile.forbot.Constants.FILE_PREFIX;
import static com.physphile.forbot.Constants.LOG_NAME;
import static com.physphile.forbot.Constants.STORAGE_AVATARS_PATH;

public class ProfileMenuDialog extends DialogFragment {
    private View v;
    private FirebaseStorage storage;
    private ImageView Avatar;
    private TextView AccountField;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            user = firebaseAuth.getCurrentUser();
            if (user != null) {
                AccountField.setText(user.getEmail());
                Log.e(LOG_NAME, "setText");
            }
        }
    };
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.Avatar:
                    CropImage.activity()
                            .setCropShape(CropImageView.CropShape.OVAL)
                            .setAspectRatio(1, 1)
                            .start(getContext(), ProfileMenuDialog.this);
                    break;
                case R.id.Profile:
                    Intent intent = new Intent("com.physphile.forbot.profile.ProfileActivity");
                    startActivity(intent);
                    break;
                case R.id.SettingsField:
                    startActivity(new Intent("com.physphile.forbot.profile.AccountSettingsActivity"));
                    break;
                case R.id.AccountExit:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Вы хотите выйти?")
                            .setNegativeButton("Нет, остаться", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).setPositiveButton("Да, выйти", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAuth.signOut();
                            getDialog().dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(positiveBtn.getLayoutParams());
                    lp.setMarginStart(new ClassHelper(getActivity()).dpToPx(15));
                    positiveBtn.setLayoutParams(lp);
                    break;
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        v = inflater.inflate(R.layout.fragment_profile_dialog, container, false);
        getDialog().getWindow().setBackgroundDrawableResource(R.color.transparent);
        AccountField = v.findViewById(R.id.AccountField);
        mAuth = FirebaseAuth.getInstance();
        Avatar = v.findViewById(R.id.Avatar);
        LinearLayout AccountExit = v.findViewById(R.id.AccountExit);
        user = mAuth.getCurrentUser();
        if (mAuth.getCurrentUser() != null) {
            Avatar.setOnClickListener(onClickListener);
        }
        storage = FirebaseStorage.getInstance();
        mAuth.addAuthStateListener(authStateListener);
        v.findViewById(R.id.SettingsField).setOnClickListener(onClickListener);
        if (user != null) {
            try {
                setAvatar();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LinearLayout Profile = v.findViewById(R.id.Profile);
        Profile.setOnClickListener(onClickListener);
        AccountExit.setOnClickListener(onClickListener);
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), result.getUri());
                    } catch (IOException e) {
                        Log.e(LOG_NAME, "cropActivity: изображение не найдено");
                    }
                    Avatar.setImageBitmap(bitmap);
                    uploadImage(result.getUri());
                    new ClassHelper(getActivity()).saveFile(bitmap, FILE_IMAGE_AVATAR);
                }
                break;

            case AUTH_ACTIVITY_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        setAvatarFirebase();
                        Log.e(LOG_NAME, "автар установлен");
                    } catch (IOException e) {
                        Log.e(LOG_NAME, "аватар не установлен");
                    }

                }
                break;
        }
    }

    private void uploadImage(Uri filePath) {
        storage.getReference(STORAGE_AVATARS_PATH + user.getUid())
                .putFile(filePath);
    }

    private void setAvatarFirebase() throws IOException {
        final File localFile = File.createTempFile(FILE_PREFIX, FILE_IMAGE_AVATAR);
        Log.e(LOG_NAME, STORAGE_AVATARS_PATH + user.getUid());
        storage.getReference(STORAGE_AVATARS_PATH + user.getUid())
                .getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Bitmap bmp = BitmapFactory.decodeFile(localFile.getPath());
                        Avatar.setImageBitmap(bmp);
                        new ClassHelper(getActivity()).saveFile(bmp, FILE_IMAGE_AVATAR);
                        Log.e(LOG_NAME, "download image done");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(LOG_NAME, "download image failure");
                    }
                });
    }

    private void setAvatar() throws IOException {
        try {
            Bitmap b = new ClassHelper(getActivity()).readFile(FILE_IMAGE_AVATAR);
            Avatar.setImageBitmap(b);
            Log.e(LOG_NAME, "Аватар установлен");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            setAvatarFirebase();
        }
    }
}