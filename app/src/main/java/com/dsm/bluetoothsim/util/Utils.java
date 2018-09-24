package com.dsm.bluetoothsim.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.dsm.bluetoothsim.R;

import androidx.annotation.ColorInt;

/**
 * Created by dsm on 12/27/17.
 */

public class Utils {
    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        if (bitmap == null)
            return null;

        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    @ColorInt
    public static int getRandomMaterial500Color(Context context) {
        Resources res = context.getResources();

        TypedArray colors = res.obtainTypedArray(R.array.material_500);
        int index = (int) (Math.random() * 100 % colors.length());
        int color = colors.getColor(index, 0);
        colors.recycle();
        return color;
    }
}
