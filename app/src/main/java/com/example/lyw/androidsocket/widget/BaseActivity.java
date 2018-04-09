/*******************************************************************************
 *
 * Copyright (c) Weaver Info Tech Co. Ltd
 *
 * BaseActivity
 *
 * app.ui.BaseActivity.java
 * TODO: File description or class description.
 *
 * @author: Administrator
 * @since:  2014-9-03
 * @version: 1.0.0
 *
 * @changeLogs:
 *     1.0.0: First created this class.
 *
 ******************************************************************************/
package com.example.lyw.androidsocket.widget;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * @author gao_chun
 * 该类为Activity基类
 */
public class BaseActivity extends AppCompatActivity {

    public static final String TAG = "gao_chun";

    //在基类中初始化Dialog
    public Dialog mLoading;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*if (!ValidateUtils.isNetworkAvailable(this)){
            DialogUtils.showToast(this,R.string.text_network_unavailable);
        }*/
        mLoading = DialogUtils.createLoadingDialog(this);
    }
}
