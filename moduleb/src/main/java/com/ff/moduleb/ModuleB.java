package com.ff.moduleb;

import android.util.Log;

import com.ff.baselib.base.ApplicationImpl;
import com.ff.baselib.base.BaseApplication;
import com.ff.baselib.config.Constant;

/**
 * description:
 * author: FF
 * time: 2019/3/21 17:38
 */
public class ModuleB implements ApplicationImpl {
    @Override
    public void onCreate(BaseApplication application) {
        Log.i(Constant.TAG, "BModule");
    }
}
