package com.rsyrysy.uploadfile.utils;

import android.app.ProgressDialog;
import android.content.Context;


public class ProgressDialogrsyrysy extends ProgressDialog {
    public ProgressDialogrsyrysy(Context context) {
        super(context);
    }

    public ProgressDialogrsyrysy(Context context, int theme) {
        super(context, theme);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void dismiss() {
        if (this != null && isShowing())
            super.dismiss();
    }

    @Override
    public void show() {
        if (this != null && !isShowing())
            super.show();
    }
}