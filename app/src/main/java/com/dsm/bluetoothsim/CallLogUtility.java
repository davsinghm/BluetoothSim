package com.dsm.bluetoothsim;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;

import java.io.IOException;
import java.io.InputStream;

public class CallLogUtility {

    public static void addNumber(Context c, ContentResolver resolver, String strNum, int type, long timeInMiliSecond, long duration) {
        strNum = strNum.replaceAll("-", "").replaceAll(" ", "");
        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.NUMBER, strNum);
        values.put(CallLog.Calls.DATE, timeInMiliSecond);
        values.put(CallLog.Calls.DURATION, duration);
        values.put(CallLog.Calls.TYPE, type);
        values.put(CallLog.Calls.NEW, 1);

/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            values.put(CallLog.Calls.FEATURES, CallLog.Calls.FEATURES_PULLED_EXTERNALLY);
*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            values.put(CallLog.Calls.VIA_NUMBER, c.getText(R.string.app_name).toString());

        if (null != resolver) {
            resolver.insert(CallLog.Calls.CONTENT_URI, values);
        }
        //getContentResolver().delete(url, where, selectionArgs)
    }

    public static void deleteNumber(ContentResolver resolver, String strNum) {
        try {
            String strUriCalls = "content://call_log/calls";
            Uri UriCalls = Uri.parse(strUriCalls);
            //Cursor c = res.query(UriCalls, null, null, null, null);
            if (null != resolver) {
                resolver.delete(UriCalls, CallLog.Calls.NUMBER + "=?", new String[]{strNum});
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

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

    public static String getContactCursorID(Context context, String number, String column) {
        ContentResolver contentResolver = context.getContentResolver();
        String contactId = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = contentResolver.query(uri, new String[]{column}, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext())
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(column));
            cursor.close();
        }

        return contactId;
    }

    public static String getContactDisplayName(Context context, String phoneNumber) {
        String name = getContactCursorID(context, phoneNumber, ContactsContract.PhoneLookup.DISPLAY_NAME);
        if (name != null)
            return name;
        return phoneNumber;
    }

    public static Bitmap retrieveContactPhoto(Context context, String phoneNumber) {
        String contactId = getContactCursorID(context, phoneNumber, ContactsContract.PhoneLookup._ID);
        Bitmap photo = null;// BitmapFactory.decodeResource(context.getResources(), R.drawable.default_image);
        if (contactId != null)
            try {
                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId)));

                if (inputStream != null) {
                    photo = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        return getCircleBitmap(photo);
    }
}