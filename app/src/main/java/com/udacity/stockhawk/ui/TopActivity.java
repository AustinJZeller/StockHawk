package com.udacity.stockhawk.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;


public class TopActivity extends AppCompatActivity {


    AlertDialog alertDialog;
    private ProgressDialog mProgressDialog;

    public void showDialog(DialogInterface.OnClickListener okClickListener, DialogInterface.OnClickListener cancelListener, String okText, String cancelText, String title, String message) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        if (title != null)
            alertDialogBuilder.setTitle(title);
        else
            alertDialogBuilder.setTitle(null);


        // set dialog message
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton(okText, okClickListener);
        if (cancelListener != null) {
            alertDialogBuilder.setNegativeButton(cancelText, cancelListener);
        }

        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog.cancel();
        }

        // create alert dialog
        alertDialog = alertDialogBuilder.create();

        // show it
        if (!isFinishing()) {
            alertDialog.show();
        }
    }

    public void showLoading(String loadingText) {
        if (!isFinishing()) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.setMessage(loadingText);
                return;
            }
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setMessage(loadingText);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }
    }

    public void hideLoading() {

        if (!isFinishing()) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                }
            });
        }
    }
}
