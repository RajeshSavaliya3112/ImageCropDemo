package com.imagesharedemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btn_take, btn_send;
    ImageView iv_pic;

    String currentPath = "";
    private int CAMERA_REQUEST = 1888;
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);

        findView ();
        attachLauncher ();
    }

    private void attachLauncher() {
        someActivityResultLauncher = registerForActivityResult (
                new ActivityResultContracts.StartActivityForResult (),
                result -> {
                    if (result.getResultCode () == Activity.RESULT_OK) {
                        Intent data = result.getData ();
                    }
                });
    }

    private void findView() {
        btn_take = findViewById (R.id.btn_take);
        btn_send = findViewById (R.id.btn_send);
        iv_pic = findViewById (R.id.iv_pic);

        btn_take.setOnClickListener (this);
        btn_send.setOnClickListener (this);
    }

    private void checkPermission() {
        Dexter.withContext (this)
                .withPermissions (
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener (new MultiplePermissionsListener () {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted ()) {
                            showSettingsDialog ();
                        }

                        if (report.isAnyPermissionPermanentlyDenied ()) {
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest ();
                    }
                })
                .onSameThread ()
                .check ();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder (MainActivity.this);
        builder.setTitle ("Need Permissions");
        builder.setMessage ("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton ("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel ();
            openSettings ();
        });
        builder.setNegativeButton ("Cancel", (dialog, which) -> dialog.cancel ());
        builder.show ();

    }

    private void openSettings() {
        Intent intent = new Intent (Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts ("package", getPackageName (), null);
        intent.setData (uri);
        startActivityForResult (intent, 101);
    }

    private void dispatchTakePicturesIntent() {
        Intent takePicture = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePicture.resolveActivity (getPackageManager ()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile ();
            } catch (IOException e) {
                Log.e ("TAG", "dispatchTakePicturesIntent:--> " + e.getMessage ());
                e.printStackTrace ();
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile (this, "com.imagesharedemo.provider", photoFile);
                takePicture.putExtra (MediaStore.EXTRA_OUTPUT, photoUri);
                someActivityResultLauncher.launch (takePicture);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "imageShare";
        File storageDir = getExternalFilesDir (Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile (imageFileName, ".jpg", storageDir);
        currentPath = image.getAbsolutePath ();

        return image;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId ()) {
            case R.id.btn_take:
                checkPermission ();
                break;

            case R.id.btn_send:
                break;
        }
    }
}