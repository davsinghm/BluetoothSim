package com.dsm.bluetoothsim.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;

import com.dsm.bluetoothsim.R;

import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CallLogUtility {

    public static void putLog(Context c, ContentResolver resolver, @Nullable String phoneNumber, int type, long timestamp, long duration) {
        ContentValues values = new ContentValues();
        if (phoneNumber != null)
            values.put(CallLog.Calls.NUMBER, phoneNumber);

        values.put(CallLog.Calls.DATE, timestamp);
        values.put(CallLog.Calls.DURATION, duration);
        values.put(CallLog.Calls.TYPE, type);
        values.put(CallLog.Calls.NEW, 1);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            values.put(CallLog.Calls.VIA_NUMBER, c.getText(R.string.app_name).toString());*/

        if (resolver != null)
            resolver.insert(CallLog.Calls.CONTENT_URI, values);
    }

    public static void deleteLog(ContentResolver resolver, String phoneNumber, long timestamp /*TODO*/) {
        try {
            Uri uriCalls = Uri.parse("content://call_log/calls");
            if (resolver != null)
                resolver.delete(uriCalls, CallLog.Calls.NUMBER + "=?", new String[]{phoneNumber});
        } catch (Exception e) {
            e.getMessage();
        }
    }

    private static long getContactCursorID(Context context, @NonNull String number) {
        String column = ContactsContract.PhoneLookup._ID;
        ContentResolver contentResolver = context.getContentResolver();
        long contactId = 0;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = contentResolver.query(uri, new String[]{column}, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext())
                contactId = cursor.getLong(cursor.getColumnIndexOrThrow(column));
            cursor.close();
        }

        return contactId;
    }

    public static String getContactDisplayName(Context context, @Nullable String phoneNumber) {
        if (phoneNumber == null)
            return null;

        ContentResolver contentResolver = context.getContentResolver();
        String displayName = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        Cursor cursor = contentResolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext())
                displayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
            cursor.close();
        }

        return displayName;
    }

    public static Bitmap getContactPhoto(Context context, @Nullable String phoneNumber) {
        if (phoneNumber == null)
            return null;

        long contactId = getContactCursorID(context, phoneNumber);
        Bitmap photo = null;
        if (contactId != 0)
            try {
                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId));

                if (inputStream != null) {
                    photo = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        return photo;
    }

    public static Bitmap getContactDisplayPhoto(Context context, @Nullable String phoneNumber) {
        if (phoneNumber == null)
            return null;

        long contactId = getContactCursorID(context, phoneNumber);
        return BitmapFactory.decodeStream(openDisplayPhoto(context, contactId));
    }

    private static InputStream openDisplayPhoto(Context context, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Uri displayPhotoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.DISPLAY_PHOTO);
        try {
            AssetFileDescriptor fd = context.getContentResolver().openAssetFileDescriptor(displayPhotoUri, "r");
            return fd.createInputStream();
        } catch (IOException e) {
            return null;
        }
    }

}