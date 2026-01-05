package com.czx.tencentv3ocr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ImageView photo;
    Button select, recognize;
    ProgressBar progressBar;
    LinearLayout result_zone;
    TextView name, sex, nation, birthday, address, id_number;

    private String currentPhotoPath = null; // 拍照后图片路径
    private File currentPhotoFile; // 用于保存拍照时创建的文件

    // 定义用于拍照、打开相册的结果回调
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        photo = findViewById(R.id.photo);
        select = findViewById(R.id.select);
        recognize = findViewById(R.id.recognize);
        progressBar = findViewById(R.id.progressBar);
        result_zone = findViewById(R.id.result_zone);
        name = findViewById(R.id.name);
        sex = findViewById(R.id.sex);
        nation = findViewById(R.id.nation);
        birthday = findViewById(R.id.birthday);
        address = findViewById(R.id.address);
        id_number = findViewById(R.id.id_number);

        // 注册拍照的 ActivityResultLauncher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (currentPhotoFile != null) {
                            Uri photoURI = FileProvider.getUriForFile(this,
                                    "com.czx.tencentv3ocr.fileprovider",
                                    currentPhotoFile);
                            photo.setImageURI(photoURI); // 设置图片
                            recognize.setEnabled(true);
                        } else {
                            Toast.makeText(this, "拍照文件未正确创建", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(this, "拍照被取消或失败", Toast.LENGTH_SHORT).show();
                    }
                });

        // 注册打开相册的 ActivityResultLauncher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri selectedImage = data.getData();
                            photo.setImageURI(selectedImage);
                            recognize.setEnabled(true);
                        }
                    }
                });

        select.setOnClickListener(v -> {
            CharSequence[] options = {"拍照", "从相册选择"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("请选择获取照片的方式");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 检查并请求相机权限
                    openCamera();
                } else if (which == 1) {
                    // 检查并请求存储权限
                    openGallery();
                }
            });
            builder.show();
        });

        recognize.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(this, "识别中...", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            result_zone.setVisibility(View.VISIBLE);
        });
    }

    private void openCamera() {
        // 首先检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            // 已经有权限，打开相机
            startCamera();
        }
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                currentPhotoFile = photoFile;
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.czx.tencentv3ocr.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "没有找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        // 检查权限（适配 Android 13+）
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            // 已经有权限，打开相册
            pickImageFromGallery();
        }
    }

    private void pickImageFromGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhotoIntent);
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // 选择照片的权限验证
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery();
            } else {
                Toast.makeText(this, "需要存储权限才能选择照片", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // 拍摄照片的权限验证
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}