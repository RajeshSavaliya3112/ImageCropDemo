package com.imagesharedemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    ActivityMainBinding mainBinding;

    String currentPath = "";
    ActivityResultLauncher<Intent> someActivityResultLauncher;
    Bitmap drawImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        initListener();
        attachLauncher();
    }

    //<editor-fold desc="Button Listener">
    private void initListener() {
        drawImage = BitmapFactory.decodeResource(getResources(), R.drawable.hacker);

        mainBinding.btnTake.setOnClickListener(this);
        mainBinding.btnSend.setOnClickListener(this);
        mainBinding.btnDraw.setOnClickListener(this);
    }
    //</editor-fold>

    //<editor-fold desc="Camera Result Callback">
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
    //</editor-fold>

    //<editor-fold desc="Check Whether Permission is given by user or not">
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
    //</editor-fold>

    //<editor-fold desc="Show setting dialog if the permission is not given">
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
    //</editor-fold>

    //<editor-fold desc="Method calls when the click on camera button">
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
    //</editor-fold>

    //<editor-fold desc="Create the file with name for store camera clicked image">
    private File createImageFile() throws IOException {
        String imageFileName = "imageShare";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPath = image.getAbsolutePath();

        return image;
    }
    //</editor-fold>

    //<editor-fold desc="Send text and image both to specific whats app number">
    private void sendTextImageMessage() {
        String sendMessage = "This is the demo app for share the image in What's App";

        Uri uri = Uri.parse("android.resource://com.imagesharedemo/drawable/hacker.png");

        /*
           You have to change this number with your sender number (Please add country code of the number and don't add the any sign in the number (+))
         */
        String phone = "919537824372";

        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            Log.e(TAG, "openWhatsApp:--> " + FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(currentPath)));
            Log.e(TAG, "openWhatsApp:--> " + uri);
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(currentPath)));
//            intent.putExtra(Intent.EXTRA_STREAM, uri);
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
    //</editor-fold>

    //<editor-fold desc="Send text to specific whats app number">
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
            } else {
                Toast.makeText(this, "Please install Whats App first.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Please install Whats App first", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    //</editor-fold>

    //<editor-fold desc="On click listener">
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take:
                checkPermission();
                break;

            case R.id.btn_send:
                sendTextImageMessage();
                break;

            case R.id.btn_draw:
                try {
                    if (saveBitmapToFile(createImageFile())) {
                        sendTextImageMessage();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    //</editor-fold>

    //<editor-fold desc="Get image from drawable and store it in storage and send URI in whats app">
    boolean saveBitmapToFile(File dir) {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dir);
            drawImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e("app", e.getMessage());
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }
    //</editor-fold>
}