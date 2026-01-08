# TencentV3OCR

## 项目介绍
本项目是基于腾讯云OCR（文字识别）APIV3版本开发的Android端身份证识别应用，支持通过拍照或从相册选择身份证照片，调用腾讯云 IDCardOCR 接口完成身份证信息提取，并展示识别结果（姓名、性别、民族、出生日期、住址、身份证号）。

## 项目结构
    com.czx.tencentv3ocr/
    ├── ApiRequestSender.java       // 网络请求发送
    ├── ApiResponseParser.java      // 响应解析工具
    ├── IdentifyResult.java         // 身份证识别结果实体类
    ├── OcrApiResponse.java         // 接口外层响应实体类
    ├── TencentCloudSigner.java     // 腾讯云V3签名工具
    ├── MainActivity.java           // 主界面逻辑
    ├── AndroidManifest.xml         // 应用配置
    └── res/
        ├── layout/
        │   └── activity_main.xml   // 主界面布局
        ├── xml/
        │   └── file_paths.xml      // FileProvider路径配置
        └── values/
            └── colors.xml          // 颜色配置

## 功能特性
- 📷 拍照 / 相册选择：支持通过相机拍摄身份证照片或从相册选取已有照片
- 🔐 腾讯云 V3 签名：严格按照 TC3-HMAC-SHA256 算法实现请求签名
- 📝 身份证信息提取：自动识别身份证正面的核心字段
- 🎨 简洁 UI：清晰的界面布局，直观展示识别结果
- 📱 权限适配：适配 Android 13 + 存储权限、相机权限
- 🚀 异步请求：网络请求异步处理，不阻塞主线程


## 技术栈
- 开发语言：Java
- 网络框架：OkHttp3
- JSON 解析：Gson
- 加密算法：HMAC-SHA256、SHA256
- 图片处理：Bitmap、Base64编码
- 权限处理：ActivityResultContracts（替代旧版startActivityForResult）
- 文件处理：FileProvider（适配 Android 7.0+文件访问）


## 环境要求
- Android Studio：Arctic Fox 及以上版本
- SDK 版本：minSdkVersion 21（Android 5.0），targetSdkVersion 33
- 构建工具：Gradle 7.0+
- 腾讯云账号：需开通OCR服务并获取 SecretId/SecretKey


## 快速开始
1. 腾讯云配置
1.1 开通服务
登录腾讯云控制台
开通「文字识别 OCR」服务（IDCardOCR 接口）
在「访问管理-API密钥管理」中创建并获取：
- SecretId
- SecretKey
1.2 替MainActivity.java.java`，替换以下常量为你的腾讯云密钥：
        // 替换为自己的密钥
        private static final String SECRET_ID = "请替换为你自己的腾讯云SecretId";
        private static final String SECRET_KEY = "请替换为你自己的腾讯云SecretKey";
2. 项目导入
- 将项目克隆 / 下载到本地
- 用Android Studio打开项目
- 等待Gradle同步完成（确保网络畅通，自动下载依赖）

3. 运行应用
- 连接Android设备或启动模拟器
- 点击「Run 'app'」按钮运行应用
- 授予相机/存储权限后即可使用

## 核心代码说明
1. 签名生成（TencentCloudSigner.java）
实现腾讯云 V3 版本签名算法（TC3-HMAC-SHA256），核心步骤：
- 时间戳处理：生成 UTC 时间戳并格式化为日期
- 构建规范请求（CanonicalRequest）：包含 HTTP 方法、URI、请求头、请求体哈希
- 构建待签名字符串（StringToSign）：拼接算法、时间戳、凭证范围、规范请求哈希
- 计算签名：通过 HMAC-SHA256 分层加密生成签名
- 构建 Authorization 头：整合签名信息用于请求认证

2. 网络请求（ApiRequestSender.java）
- 封装 OkHttp POST 请求，设置腾讯云 OCR 接口必需的请求头：
- Authorization：签名信息
- X-TC-Action：接口名称（IDCardOCR）
- X-TC-Version：接口版本（2018-11-19）
- X-TC-Timestamp：时间戳
- X-TC-Region：地域（ap-guangzhou）

3. 图片处理（MainActivity.java）
- 图片选择：相机 / 相册选择图片，通过 ActivityResultLauncher 处理结果
- Base64 编码：将 Bitmap 转换为 Base64 编码字符串（腾讯云 OCR 接口要求）
- 权限处理：适配 Android 6.0 + 动态权限、Android 13 + 存储权限

4. 响应解析（ApiResponseParser.java + OcrApiResponse.java + IdentifyResult.java）
- 多层 JSON 解析：先解析外层 Response 结构，再提取身份证核心字段
- 实体类映射：通过 Gson 注解（@SerializedName）匹配接口返回字段

5. UI 布局（activity_main.xml）
- 滚动布局：适配小屏幕设备，支持内容滚动
- 结果区域：默认隐藏，识别成功后显示身份证字段
- 响应式设计：按钮状态联动（选择图片后「开始识别」按钮启用）

## 权限说明
|权限|用途|
|--|--|
|CAMERA|拍摄身份证照片|
|READ_MEDIA_IMAGES（Android 13+）|读取相册图片|
|READ_EXTERNAL_STORAGE（Android ≤12）|读取相册图片|
|INTERNET|调用腾讯云 OCR 接口|
|ACCESS_NETWORK_STATE|检测网络状态（可选）|

## 接口参数说明
### 请求参数
| 参数名         | 类型     | 说明                    |
|-------------|--------|-----------------------|
| ImageBase64 | String | 身份证图片 Base64 编码（不含前缀） |
### 响应参数
| 字段名       | 类型     | 说明                |
|-----------|--------|-------------------|
| Name      | String | 姓名                |
| Sex       | String | 性别                |
| Nation    | String | 民族                |
| Birth     | String | 出生日期（格式：YYYYMMDD） |
| Address   | String | 住址                |
| IdNum     | String | 身份证号码             |
| errorCode | Int    | 错误码（0 表示成功）       |
| errorMsg  | String | 错误信息              |

## 常见问题解决
1. 签名错误（Authorization Error）
- 检查 SecretId/SecretKey 是否正确
- 确认时间戳为秒级（System.currentTimeMillis () / 1000）
- 验证签名算法中的 CanonicalHeaders 与 SIGNED_HEADERS 是否一致
- 确保请求头中的 X-TC-* 参数与签名时使用的参数一致

2. 图片加载失败
- 检查 FileProvider 配置是否正确（AndroidManifest.xml + file_paths.xml）
- 确认图片文件路径权限
- 适配不同 Android 版本的图片读取方式

3. 权限被拒绝
- 确保动态申请权限的逻辑正确
- 适配 Android 13 + 的 READ_MEDIA_IMAGES 权限
- 在权限被拒绝时给出友好提示

4. 响应体为空
- 检查网络请求是否成功（response.isSuccessful ()）
- 验证接口返回的 JSON 格式是否正确
- 确认 Gson 解析实体类与返回字段匹配

## 扩展功能建议
- 身份证正反面识别：添加 IDCardType 参数，支持正面 / 反面识别
- 图片压缩：优化大图片的 Base64 编码，减少请求体积
- 缓存机制：缓存识别结果，避免重复请求
- 多语言支持：添加中英文切换
- 错误重试：网络请求失败时自动重试
- 日志优化：添加更详细的日志，方便调试
- 隐私保护：识别完成后清除本地图片缓存

## 许可证
本项目仅供学习和研究使用，如需商用请遵守腾讯云相关服务协议。
