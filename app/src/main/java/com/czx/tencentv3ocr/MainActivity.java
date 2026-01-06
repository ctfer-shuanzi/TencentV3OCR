package com.czx.tencentv3ocr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ImageView photo;
    Button select, recognize;

    LinearLayout result_zone;
    TextView name, sex, nation, birthday, address, id_number;

    private File currentPhotoFile; // 用于保存拍照时创建的临时文件
    private String currentPhotoPath; // 保存currentPhotoFile的绝对路径
    private Bitmap selectedBitmap;// 保存从currentPhotoPath加载的bitmap

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
        result_zone = findViewById(R.id.result_zone);
        name = findViewById(R.id.name);
        sex = findViewById(R.id.sex);
        nation = findViewById(R.id.nation);
        birthday = findViewById(R.id.birthday);
        address = findViewById(R.id.address);
        id_number = findViewById(R.id.id_number);

        photo.setAdjustViewBounds(true);

        // 注册拍照的 ActivityResultLauncher
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (currentPhotoFile != null) {
                            Uri imageURI = FileProvider.getUriForFile(this,
                                    "com.czx.tencentv3ocr.fileprovider",
                                    currentPhotoFile);// 获取图片的URI
                            photo.setImageURI(imageURI); // 设置图片

                            currentPhotoPath = currentPhotoFile.getAbsolutePath();// 保存临时文件的绝对路径
                            selectedBitmap = BitmapFactory.decodeFile(currentPhotoPath);// 从currentPhotoPath加载bitmap

                            if(selectedBitmap != null){
                                recognize.setEnabled(true);// 启用”识别“按钮
                            }else {
                                Toast.makeText(this, "无法加载选择的图片", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Toast.makeText(this, "拍照的临时文件未正确创建", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(this, "拍照被取消或失败", Toast.LENGTH_SHORT).show();
                    }
                });

        // 注册打开相册的 ActivityResultLauncher
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri imageURI = data.getData();// 获取图片的URI
                            photo.setImageURI(imageURI);// 设置图片

                            // 从 URI 加载 Bitmap
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(imageURI);
                                selectedBitmap = BitmapFactory.decodeStream(inputStream);// 注意：相册图片没有 currentPhotoPath，所以不要依赖它！

                                if (selectedBitmap != null) {
                                    recognize.setEnabled(true);
                                } else {
                                    Toast.makeText(this, "无法加载选择的图片", Toast.LENGTH_SHORT).show();
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "读取相册图片失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        select.setOnClickListener(v -> {
            CharSequence[] options = {"拍照", "从相册选择"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("请选择获取照片的方式");
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    handleCameraSelection();// 如果选择了”拍照“
                } else if (which == 1) {
                    handleGallerySelection();// 如果选择了”从相册选择“
                }
            });
            builder.show();
        });

        recognize.setOnClickListener(v -> {
            if(selectedBitmap == null){
                Toast.makeText(this, "还没有获取到bitmap", Toast.LENGTH_SHORT).show();
                return ;
            }

            Toast.makeText(this, "识别中...", Toast.LENGTH_SHORT).show();

            String imageBase64 = bitmapToBase64(selectedBitmap);

            result_zone.setVisibility(View.VISIBLE);

        });
    }

    // 检查是否已有相机使用权限或相册读取权限
    private boolean checkMyPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleCameraSelection() {
        String requestedPermission = Manifest.permission.CAMERA;
        // 检查相机权限
        if(checkMyPermission(requestedPermission)){
            openCamera();// 有权限，直接打开相机
        }else{
            Toast.makeText(this,"正在请求相机权限",Toast.LENGTH_SHORT).show();// 没权限，去请求权限
            ActivityCompat.requestPermissions(this,new String[]{requestedPermission}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void handleGallerySelection() {
        String requestedPermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        // 检查并请求存储权限
        if(checkMyPermission(requestedPermission)){
            openGallery();// 有权限，直接打开相册
        }else{
            Toast.makeText(this,"没有打开相册的权限",Toast.LENGTH_SHORT).show();// 没权限，去请求权限
            requestStoragePermissionWithExplanation(requestedPermission);
        }
    }

    // 启用相机
    private void openCamera() {
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
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(pickPhotoIntent);
    }

    private void requestStoragePermissionWithExplanation(String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // 用户之前拒绝过，显示解释
            new AlertDialog.Builder(this)
                    .setTitle("存储权限说明")
                    .setMessage("我们需要访问您的照片来存储和识别身份证信息")
                    .setPositiveButton("去授权", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{permission},
                                REQUEST_READ_EXTERNAL_STORAGE);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            // 直接请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // 选择照片的权限验证
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "需要存储权限才能选择照片", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // 拍摄照片的权限验证
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 创建临时文件
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

    // 将Bitmap转为Base64格式
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG,80,outputStream);// 第二个参数是图片质，取值范围在1-100

        if(!success){
            throw new RuntimeException("无法压缩Bitmap");
        }

        byte[] imageBytes = outputStream.toByteArray();
        return Base64Util.encode(imageBytes);
    }
}