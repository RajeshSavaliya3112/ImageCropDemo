package com.imagesharedemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.imagesharedemo.databinding.ActivityMainBinding;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    ActivityMainBinding mainBinding;

    String currentPath = "";
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        findView();
        attachLauncher();
    }

    private void attachLauncher() {
        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        File file = new File(currentPath);
                        Uri picUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);

                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);
                            mainBinding.ivPic.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "attachLauncher: " + e.getMessage());
                            e.printStackTrace();
                        }

                        mainBinding.btnSend.setEnabled(true);

                    }
                });
    }

    private void findView() {
        mainBinding.btnTake.setOnClickListener(this);
        mainBinding.btnSend.setOnClickListener(this);
    }

    private void checkPermission() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            dispatchTakePicturesIntent();
                        }

                        if (report.isAnyPermissionPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void dispatchTakePicturesIntent() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePicture.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Log.e(TAG, "dispatchTakePicturesIntent:--> " + e.getMessage());
                e.printStackTrace();
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(this, "com.imagesharedemo.provider", photoFile);
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                someActivityResultLauncher.launch(takePicture);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "imageShare";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPath = image.getAbsolutePath();

        return image;
    }

    private void sendTextImageMessage() {
        String sendMessage = "This is the demo app for share the image in What's App";

        String phone = "919662436892";

        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            Log.e(TAG, "openWhatsApp:--> " + FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(currentPath)));
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(currentPath)));
            intent.putExtra("jid", phone + "@s.whatsapp.net");
            intent.putExtra(Intent.EXTRA_TEXT, sendMessage);
            intent.setAction(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.whatsapp");
            intent.setType("image/*");
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "openWhatsApp: " + e.getMessage());
            e.printStackTrace();
        }
        finish();
    }

    private void sendOnlyTextMessage() {
        try {

            String sendMessage = "This is the demo app for share the image in What's App";
            String phone = "919662436892";

            PackageManager packageManager = getPackageManager();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(currentPath)));

            String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(sendMessage, "UTF-8");
            i.setPackage("com.whatsapp");
            i.setData(Uri.parse(url));
            if (i.resolveActivity(packageManager) != null) {
                startActivity(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take:
                checkPermission();
                break;

            case R.id.btn_send:
                sendTextImageMessage();
                break;
        }
    }
}