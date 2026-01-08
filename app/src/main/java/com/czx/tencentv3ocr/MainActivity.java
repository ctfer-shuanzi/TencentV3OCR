package com.czx.tencentv3ocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    // 常量定义
    private static final int PERMISSION_CAMERA = 100;
    private static final int PERMISSION_STORAGE = 101;

    // 替换为自己的密钥
    private static final String SECRET_ID = "请替换为你自己的腾讯云SecretId";
    private static final String SECRET_KEY = "请替换为你自己的腾讯云SecretKey";

    // UI 控件
    private ImageView ivPhoto;
    private Button btnSelect, btnRecognize;
    private LinearLayout llResultZone;
    private TextView tvName, tvSex, tvNation, tvBirthday, tvAddress, tvIdNumber;

    // 图片相关
    private File currentPhotoFile;
    private Bitmap selectedBitmap;

    // ActivityResult 启动器
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        initLauncher();
    }

    // 初始化控件
    private void initView() {
        ivPhoto = findViewById(R.id.photo);
        btnSelect = findViewById(R.id.select);
        btnRecognize = findViewById(R.id.recognize);
        llResultZone = findViewById(R.id.result_zone);
        tvName = findViewById(R.id.name);
        tvSex = findViewById(R.id.sex);
        tvNation = findViewById(R.id.nation);
        tvBirthday = findViewById(R.id.birthday);
        tvAddress = findViewById(R.id.address);
        tvIdNumber = findViewById(R.id.id_number);

        btnRecognize.setEnabled(false);
    }

    // 初始化点击事件
    private void initListener() {
        btnSelect.setOnClickListener(v -> showPhotoDialog());
        btnRecognize.setOnClickListener(v -> startOcr());
    }

    // 初始化ActivityResultLauncher
    private void initLauncher() {
        // 拍照
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && currentPhotoFile != null) {
                Uri uri = FileProvider.getUriForFile(this, "com.czx.tencentv3ocr.fileprovider", currentPhotoFile);
                Bitmap bitmap = null;
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(is);
                    ivPhoto.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
                }
                selectedBitmap = bitmap;
                btnRecognize.setEnabled(selectedBitmap != null);
            } else {
                Toast.makeText(this, "拍照取消或失败", Toast.LENGTH_SHORT).show();
            }
        });

        // 相册
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                ivPhoto.setImageURI(uri);
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    selectedBitmap = BitmapFactory.decodeStream(is);
                    btnRecognize.setEnabled(selectedBitmap != null);
                } catch (IOException e) {
                    Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 显示选择照片对话框
    private void showPhotoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择照片来源")
                .setItems(new String[]{"拍照", "相册"}, (dialog, which) -> {
                    if (which == 0) {
                        checkPermission(Manifest.permission.CAMERA, PERMISSION_CAMERA, this::openCamera);
                    } else {
                        String permission = getStoragePermission();
                        checkPermission(permission, PERMISSION_STORAGE, this::openGallery);
                    }
                }).show();
    }

    // 权限检查
    private void checkPermission(String permission, int requestCode, Runnable onGranted) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            onGranted.run();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    // 打开相机
    @SuppressLint("QueryPermissionsNeeded")
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                currentPhotoFile = createImageFile();
                Uri uri = FileProvider.getUriForFile(this, "com.czx.tencentv3ocr.fileprovider", currentPhotoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                cameraLauncher.launch(intent);
            } catch (IOException e) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "未检测到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    // 打开相册
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // 开始OCR识别
    private void startOcr() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show();
            return;
        }

        // 校验密钥是否替换
        if (SECRET_ID.contains("请替换") || SECRET_KEY.contains("请替换")) {
            Toast.makeText(this, "请先修改 MainActivity 中的 SECRET_ID 和 SECRET_KEY 为你自己的密钥！", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "识别中...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // 图片转Base64
                String base64 = bitmapToBase64(selectedBitmap);

                // 构建请求体
                Map<String, Object> params = new HashMap<>();
                params.put("ImageBase64", base64);
                String payload = new Gson().toJson(params);
                Log.d("czx", "用于签名的 JSON 请求体: " + payload);  // 👈 打印此内容


                // 生成签名
                long timestamp = System.currentTimeMillis() / 1000;
                TencentCloudSigner signer = new TencentCloudSigner(SECRET_ID, SECRET_KEY,timestamp);
                String auth = signer.generateAuthorization(payload);
                long ts = System.currentTimeMillis() / 1000;
                Log.d("czx", "当前系统 UTC 时间戳（秒）: " + ts);
                Log.d("czx", "对应 UTC 时间: " + new Date(ts * 1000L).toGMTString());
                Log.d("czx", "对应本地时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(ts * 1000L)));

                if (auth == null) {
                    runOnUiThread(() -> Toast.makeText(this, "签名失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 发送请求
                String responseJson = ApiRequestSender.sendPostRequest(payload,auth,timestamp);
                Log.d("czx","responseJson:"+responseJson);

                // 解析结果
                if (responseJson != null && !responseJson.isEmpty()) {
                    IdentifyResult result = ApiResponseParser.parseResponse(responseJson);
                    runOnUiThread(() -> showResult(result));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "无返回数据", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.d("czx", Objects.requireNonNull(e.getMessage()));
                runOnUiThread(() -> Toast.makeText(this, "识别失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 显示识别结果
    private void showResult(IdentifyResult result) {
        if (result == null || result.getErrorCode() != 0) {
            String msg = result != null ? result.getErrorMsg() : "未知错误";
            Toast.makeText(this, "识别失败：" + msg, Toast.LENGTH_SHORT).show();
            return;
        }

        llResultZone.setVisibility(View.VISIBLE);
        tvName.setText(result.getName());
        tvSex.setText(result.getSex());
        tvNation.setText(result.getNation());
        tvBirthday.setText(result.getBirth());
        tvAddress.setText(result.getAddress());
        tvIdNumber.setText(result.getId());
    }

    // 工具方法：Bitmap转Base64
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }

    // 工具方法：创建图片文件
    private File createImageFile() throws IOException {
        String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    // 工具方法：获取存储权限（适配Android 13+）
    private String getStoragePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    // 权限请求回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_CAMERA) openCamera();
            else if (requestCode == PERMISSION_STORAGE) openGallery();
        } else {
            Toast.makeText(this, "权限被拒绝，无法完成操作", Toast.LENGTH_SHORT).show();
        }
    }
}