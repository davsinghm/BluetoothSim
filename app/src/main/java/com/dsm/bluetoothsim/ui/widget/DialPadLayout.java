package com.dsm.bluetoothsim.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

import com.dsm.bluetoothsim.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.NonNull;

/**
 * Created by dsm on 12/24/17.
 */

public class DialPadLayout extends BottomSheetBehavior.BottomSheetCallback implements View.OnClickListener, View.OnLongClickListener {

    private Context context;
    private BottomSheetBehavior bottomSheetBehavior;
    private OnInputListener onInputListener;
    private EditText editText;

    public interface OnInputListener {
        void onInput(View buttonView, char c);
    }

    public DialPadLayout(Activity activity, OnInputListener onInputListener) {
        this.context = activity;
        this.bottomSheetBehavior = BottomSheetBehavior.from(activity.findViewById(R.id.sliding_pane_layout));
        this.bottomSheetBehavior.setBottomSheetCallback(this);
        this.onInputListener = onInputListener;
        this.editText = activity.findViewById(R.id.et_input_phone);

        int[] ids = {R.id.b1, R.id.b2, R.id.b3, R.id.b4, R.id.b5, R.id.b6, R.id.b7, R.id.b8, R.id.b9, R.id.b0, R.id.ba, R.id.bh};

        for (int id : ids) {
            activity.findViewById(id).setOnClickListener(this);
            activity.findViewById(id).setOnLongClickListener(this);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (onInputListener != null) {
            switch (v.getId()) {
                case R.id.b0:
                    onInputListener.onInput(v, '+');
                    editText.append("+");
                    return true;
            }
        }
        return false;
    }

    public void onClick(View v) {
        if (onInputListener != null) {
            switch (v.getId()) {
                case R.id.b1:
                    onInputListener.onInput(v, '1');
                    editText.append("1");
                    break;
                case R.id.b2:
                    onInputListener.onInput(v, '2');
                    editText.append("2");
                    break;
                case R.id.b3:
                    onInputListener.onInput(v, '3');
                    editText.append("3");
                    break;
                case R.id.b4:
                    onInputListener.onInput(v, '4');
                    editText.append("4");
                    break;
                case R.id.b5:
                    onInputListener.onInput(v, '5');
                    editText.append("5");
                    break;
                case R.id.b6:
                    onInputListener.onInput(v, '6');
                    editText.append("6");
                    break;
                case R.id.b7:
                    onInputListener.onInput(v, '7');
                    editText.append("7");
                    break;
                case R.id.b8:
                    onInputListener.onInput(v, '8');
                    editText.append("8");
                    break;
                case R.id.b9:
                    onInputListener.onInput(v, '9');
                    editText.append("9");
                    break;
                case R.id.b0:
                    onInputListener.onInput(v, '0');
                    editText.append("0");
                    break;
                case R.id.ba:
                    onInputListener.onInput(v, '*');
                    editText.append("*");
                    break;
                case R.id.bh:
                    onInputListener.onInput(v, '#');
                    editText.append("#");
                    break;
            }
        }
    }

    public void openPage() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {

    }
}
