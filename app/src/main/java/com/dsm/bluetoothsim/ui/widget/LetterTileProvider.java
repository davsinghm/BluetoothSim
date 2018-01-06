package com.dsm.bluetoothsim.ui.widget;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.text.TextPaint;

import com.dsm.bluetoothsim.R;

public class LetterTileProvider {

    private final TextPaint mPaint = new TextPaint();
    private final Rect mBounds = new Rect();
    private final Canvas mCanvas = new Canvas();
    private final char[] mFirstChar = new char[1];
    private final int mTileLetterFontSize;
    private final int mTileSize;

    public LetterTileProvider(Context context) {
        mPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        mPaint.setColor(Color.WHITE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);

        mTileLetterFontSize = context.getResources().getDimensionPixelSize(R.dimen.tile_letter_font_size);
        mTileSize = context.getResources().getDimensionPixelSize(R.dimen.letter_tile_size);
    }

    public Bitmap getLetterTile(String displayName, @ColorInt int colorInt) {
        if (displayName != null) {
            final Bitmap bitmap = Bitmap.createBitmap(mTileSize, mTileSize, Bitmap.Config.ARGB_8888);
            final char firstChar = displayName.charAt(0);

            final Canvas c = mCanvas;
            c.setBitmap(bitmap);
            c.drawColor(colorInt);

            if (isEnglishLetterOrDigit(firstChar)) {
                mFirstChar[0] = Character.toUpperCase(firstChar);
                mPaint.setTextSize(mTileLetterFontSize);
                mPaint.getTextBounds(mFirstChar, 0, 1, mBounds);
                c.drawText(mFirstChar, 0, 1, mTileSize / 2, mTileSize / 2
                        + (mBounds.bottom - mBounds.top) / 2, mPaint);

                return bitmap;
            }
        }
        return null;
    }

    private boolean isEnglishLetterOrDigit(char c) {
        return 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9';
    }
}