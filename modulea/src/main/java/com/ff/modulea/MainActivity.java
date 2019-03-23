package com.ff.modulea;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.alibaba.android.arouter.launcher.ARouter;
import com.ff.baselib.base.BaseActivity;
import com.ff.baselib.config.Constant;

import butterknife.OnClick;


public class MainActivity extends BaseActivity {

    private static final int REQUEST_SCAN = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    @Override
    protected void initEvent() {

    }

    @OnClick(R2.id.ll_scan)
    void onClick(View v) {
        if (v.getId() == R.id.ll_scan) {
            getRuntimePermission();
        }
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
                jumpScanPage();
            }
        }
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
                jumpScanPage();
                break;
            }
        }
    }

    // 跳转到扫码页
    private void jumpScanPage() {
        ARouter.getInstance()
                .build("/moduleb/CaptureActivity")
                .navigation(this, REQUEST_SCAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, data.getStringExtra(Constant.BAR_CODE), Toast.LENGTH_LONG).show();
            } else if (resultCode == Constant.RESULT_CAMERA_ERROR) {
                Toast.makeText(this, "相机打开出错，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
