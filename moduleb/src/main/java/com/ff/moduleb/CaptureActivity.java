package com.ff.moduleb;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.ff.baselib.base.BaseActivity;
import com.ff.baselib.config.Constant;
import com.ff.moduleb.utils.BeepManager;
import com.ff.moduleb.utils.Utils;
import com.ff.qrcode.library.CaptureCallback;
import com.ff.qrcode.library.camera.CameraManager;
import com.ff.qrcode.library.decode.DecodeThread;
import com.ff.qrcode.library.utils.CaptureActivityHandler;
import com.ff.qrcode.library.utils.InactivityTimer;
import com.google.zxing.Result;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;

@Route(path = "/moduleb/CaptureActivity")
public class CaptureActivity extends BaseActivity implements CaptureCallback, SurfaceHolder.Callback {

    @BindView(R2.id.capture_preview)
    SurfaceView scanPreview; // SurfaceView控件
    @BindView(R2.id.capture_container)
    RelativeLayout scanContainer; // 布局容器
    @BindView(R2.id.capture_crop_view)
    RelativeLayout scanCropView; // 布局中的扫描框
    @BindView(R2.id.scan_line)
    ImageView scanLine;// 扫描线
    @BindView(R2.id.tb_light)
    ToggleButton tbLight;// 开/关灯按钮
    @BindView(R2.id.tv_light)
    TextView tvLight;// 开/关灯文字


    private ObjectAnimator objectAnimator; // 属性动画
    private boolean isPause; // 是否暂停

    private InactivityTimer inactivityTimer; // 计时器
    private BeepManager beepManager; // 蜂鸣器
    private CaptureActivityHandler handler;
    private Rect mCropRect; // 矩形
    private CameraManager cameraManager; // 相机管理类
    private boolean isHasSurface; // SurfaceView控件是否存在，surfaceCreated

    private ProgressDialog dialog;// 扫描相册加载框

    private boolean isPermission;// 是否有权限

    @Override
    protected int getLayoutId() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return R.layout.activity_capture;
    }

    @Override
    protected void initView() {
        // 扫描线性动画(属性动画可暂停)
        float curTranslationY = scanLine.getTranslationY();
        objectAnimator = ObjectAnimator.ofFloat(scanLine, "translationY",
                curTranslationY, Utils.dp2px(this, 170));
        // 动画持续的时间
        objectAnimator.setDuration(4000);
        // 线性动画 Interpolator 匀速
        objectAnimator.setInterpolator(new LinearInterpolator());
        // 动画重复次数
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        // 动画如何重复，从下到上，还是重新开始从上到下
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        getRuntimePermission();
    }

    @Override
    protected void initEvent() {

        // 闪光灯控制
        tbLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvLight.setText("关灯");
                if (!isPermission) {
                    Toast.makeText(CaptureActivity.this, "权限拒绝", Toast.LENGTH_SHORT).show();
                } else {
                    Utils.openFlashlight(cameraManager);
                }
            } else {
                tvLight.setText("开灯");
                if (!isPermission) {
                    Toast.makeText(CaptureActivity.this, "权限拒绝", Toast.LENGTH_SHORT).show();
                } else {
                    Utils.closeFlashlight();
                }
            }
        });
    }

    // 获得运行时权限
    private void getRuntimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] perms = {Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 200);
            } else {
                isPermission = true;
            }
        }
    }

    @OnClick(R2.id.ll_album)
    void onClick(View v) {
        if (v.getId() == R.id.ll_album) {// 打开相册
            // 打开相册，做权限判断
            if (!isPermission) {
                Toast.makeText(CaptureActivity.this, "权限拒绝", Toast.LENGTH_SHORT).show();
            } else {
                Utils.openAlbum(CaptureActivity.this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause() {
        if (isPermission) {
            pauseScan();
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200: {
                for (int i : grantResults) {
                    if (i == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "权限拒绝", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                isPermission = true;
                break;
            }
        }
    }

    // 开始扫描
    private void startScan() {
        if (isPermission) {
            inactivityTimer = new InactivityTimer(this);
            beepManager = new BeepManager(this);

            if (isPause) {
                // 如果是暂停，扫描动画应该要暂停
                objectAnimator.resume();
                isPause = false;
            } else {
                // 开始扫描动画
                objectAnimator.start();
            }
        }
        // 初始化相机管理
        cameraManager = new CameraManager(this);
        handler = null; // 重置handler
        if (isHasSurface) {
            initCamera(scanPreview.getHolder());
        } else {
            // 等待surfaceCreated来初始化相机
            scanPreview.getHolder().addCallback(this);
        }
        // 开启计时器
        if (inactivityTimer != null) {
            inactivityTimer.onResume();
        }
    }

    // 暂停扫描
    private void pauseScan() {
        if (handler != null) {
            // handler退出同步并置空
            handler.quitSynchronously();
            handler = null;
        }
        // 计时器的暂停
        if (inactivityTimer != null) {
            inactivityTimer.onPause();
        }
        // 关闭蜂鸣器
        if (beepManager != null) {
            beepManager.close();
        }
        // 关闭相机管理器驱动
        cameraManager.closeDriver();
        if (!isHasSurface) {
            // remove等待
            scanPreview.getHolder().removeCallback(this);
        }
        if (isPermission) {
            // 动画暂停
            objectAnimator.pause();
        }
        isPause = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(Constant.TAG, "surfaceCreated: SurfaceHolder is null");
            return;
        }

        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    // 初始化相机
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("SurfaceHolder is null");
        }
        if (cameraManager.isOpen()) {
            Log.e(Constant.TAG, "surfaceCreated: camera is open");
            return;
        }

        if (!isPermission) {
            Log.e(Constant.TAG, "no permission");
            return;
        }

        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, DecodeThread.ALL_MODE);
            }
            initCrop();
        } catch (IOException ioe) {
            Log.w(Constant.TAG, ioe);
            Utils.displayFrameworkBugMessageAndExit(this);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.lang.RuntimeException: Fail to connect to camera service
            Log.w(Constant.TAG, "Unexpected error initializing camera", e);
            Utils.displayFrameworkBugMessageAndExit(this);
        }
    }

    // 初始化截取的矩形区域
    private void initCrop() {
        // 获取相机的宽高
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        // 获取布局中扫描框的位置信息
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - Utils.getStatusBarHeight(this);

        // 获取截取的宽高
        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        // 获取布局容器的宽高
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        // 计算最终截取的矩形的左上角顶点x坐标
        int x = cropLeft * cameraWidth / containerWidth;
        // 计算最终截取的矩形的左上角顶点y坐标
        int y = cropTop * cameraHeight / containerHeight;

        // 计算最终截取的矩形的宽度
        int width = cropWidth * cameraWidth / containerWidth;
        // 计算最终截取的矩形的高度
        int height = cropHeight * cameraHeight / containerHeight;

        // 生成最终的截取的矩形
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void handleDecode(Result result, Bundle bundle) {
        // 扫码成功之后回调的方法
        if (inactivityTimer != null) {
            inactivityTimer.onActivity();
        }
        // 播放蜂鸣声
        beepManager.playBeepSoundAndVibrate();

        // 将扫码的结果返回到MainActivity
        Intent intent = new Intent();
        intent.putExtra(Constant.BAR_CODE, result.getText());
        Utils.setResultAndFinish(CaptureActivity.this, RESULT_OK, intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // 相册返回
        if (requestCode == Utils.SELECT_PIC_KITKAT // 4.4及以上图库
                && resultCode == Activity.RESULT_OK) {
            new Thread(() -> {
                showProgressDialog();
                Uri uri = data.getData();
                String path = Utils.getPath(CaptureActivity.this, uri);
                Result result = Utils.scanningImage(path);
                Intent intent = new Intent();
                if (result == null) {
                    intent.putExtra(Constant.BAR_CODE, "未发现二维码/条形码");
                } else {
                    // 数据返回
                    intent.putExtra(Constant.BAR_CODE, Utils.recode(result.getText()));
                }
                Utils.setResultAndFinish(CaptureActivity.this, RESULT_OK, intent);
                dismissProgressDialog();
            }).start();
        }
    }

    private void showProgressDialog() {
        runOnUiThread(() -> {
            if (dialog == null) {
                dialog = new ProgressDialog(CaptureActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
            dialog.setMessage("扫描中");    //设置内容
            dialog.setCancelable(false);//点击屏幕和按返回键都不能取消加载框
            dialog.show();
        });
    }

    private void dismissProgressDialog() {
        runOnUiThread(() -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (objectAnimator != null) {
            objectAnimator.end();
        }
        if (inactivityTimer != null) {
            inactivityTimer.shutdown();
        }
        super.onDestroy();
    }
}