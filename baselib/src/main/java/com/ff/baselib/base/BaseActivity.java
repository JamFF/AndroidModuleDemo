package com.ff.baselib.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.LayoutRes;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * description:
 * author: FF
 * time: 2019/3/22 23:39
 */
public abstract class BaseActivity extends Activity {

    private Unbinder butterKnife;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        butterKnife = ButterKnife.bind(this);
        initView();
        initData(savedInstanceState);
        initEvent();
    }

    @LayoutRes
    protected abstract int getLayoutId();

    protected abstract void initView();

    protected abstract void initData(Bundle savedInstanceState);

    protected abstract void initEvent();

    @Override
    protected void onDestroy() {
        if (butterKnife != null) {
            butterKnife.unbind();
        }
        super.onDestroy();
    }
}
