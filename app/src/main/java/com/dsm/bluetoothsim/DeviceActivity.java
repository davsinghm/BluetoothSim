package com.dsm.bluetoothsim;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dsm.bluetoothsim.device.BTDevice;
import com.dsm.bluetoothsim.ui.PhoneActivity;
import com.dsm.bluetoothsim.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DeviceActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_CONTACT = 1;
    private static final int REQUEST_CODE_CHANGE_SMS_APP_TO_THIS = 2;
    private static final int REQUEST_CODE_CHANGE_SMS_APP_TO_DEFAULT = 3;

    EditText ed;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CHANGE_SMS_APP_TO_THIS || requestCode == REQUEST_CODE_CHANGE_SMS_APP_TO_DEFAULT) {
            onChangeSmsAppResult(requestCode, resultCode);
        } else if (requestCode == REQUEST_CODE_PICK_CONTACT)
            if (resultCode == RESULT_OK) {
                String phoneNumber = "";
                List<String> numbers = new ArrayList<>();
                List<String> labels = new ArrayList<>();

                Uri result = data.getData();
                String id = result.getLastPathSegment();
                try (Cursor cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[]{id}, null)) {
                    int index = cursor.getColumnIndex(Phone.DATA);
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            phoneNumber = cursor.getString(index).replaceAll("-", "").replaceAll(" ", "");
                            int type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE));
                            String label = type != 0 ? getString(Phone.getTypeLabelResource(type)) : cursor.getString(cursor.getColumnIndex(Phone.LABEL));
                            if (!numbers.contains(phoneNumber)) {
                                numbers.add(phoneNumber);
                                labels.add(label);
                            }
                            cursor.moveToNext();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (numbers.size() > 1) {
                    final String[] items1 = numbers.toArray(new String[numbers.size()]);
                    final String[] items2 = labels.toArray(new String[labels.size()]);
                    ListAdapter adapter = new ArrayAdapter<String>(this, R.layout.two_line_list_item, android.R.id.text1, items1) {
                        @NonNull
                        @Override
                        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                            // User super class to create the View
                            View view = super.getView(position, convertView, parent);
                            TextView text1 = view.findViewById(android.R.id.text1);
                            TextView text2 = view.findViewById(android.R.id.text2);
                            text1.setText(items1[position]);
                            text2.setText(items2[position]);
                            return view;
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Choose a number");
                    builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            ed.setText(items1[item]);
                        }
                    });
                    builder.show();

                } else {
                    ed.setText(phoneNumber);
                }

            }

        /*if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    String number = cursor.getString(numberIndex).replaceAll("-", "").replaceAll(" ", "");
                    ed.setText(number);
                }

                cursor.close();
            }
        }*/
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setElevation(16);

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeviceActivity.this, BluetoothService.class);
                startService(intent);
            }
        });

        findViewById(R.id.button_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeviceActivity.this, BluetoothService.class);
                stopService(intent);
            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*BTDevice.getInstance().queryBatteryInfo();
                BTDevice.getInstance().querySignal();*/
                saveSms();
            }
        });
        ed = findViewById(R.id.phone);

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ret;
                if (TextUtils.clean(ed.getText().toString()) != null) {
                    ret = BTDevice.getInstance().call(ed.getText().toString());
                } else {
                    Toast.makeText(DeviceActivity.this, "Invalid number, calling +919999999999", Toast.LENGTH_SHORT).show();
                    ret = BTDevice.getInstance().call("+919999999999");
                }
                if (!ret)
                    Toast.makeText(DeviceActivity.this, R.string.device_not_connected, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DeviceActivity.this, PhoneActivity.class).putExtra("PHONE_NUMBER", ed.getText().toString()));
            }
        });

        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*byte[] bytes = new byte[]{
                        //OK:
                        //-64, 0, -127, 0, 126, -47, -62, 49, 18, -105, -40, -85, 5, -64
                        -64, 0, -127, 0, 126, -47, -62, 49, 18, -105, -40, -85, 49, -64
                };
                BTDevice.getInstance().notifyData(BleDevice.CHANNEL_ID_BCSP_MUX, bytes);*/

                Uri uri = Uri.parse("content://contacts");
                Intent intent = new Intent(Intent.ACTION_PICK, uri);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                //intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
            }
        });
    }

    private String defaultSmsApp;

    private void onChangeSmsAppResult(int requestCode, int resultCode) {
        if (requestCode == REQUEST_CODE_CHANGE_SMS_APP_TO_THIS) {
            if (resultCode == RESULT_OK)
                checkApp();
            else
                Toast.makeText(this, "Process cancelled by user", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_CODE_CHANGE_SMS_APP_TO_DEFAULT)
            if (resultCode != RESULT_OK)
                changeSmsAppToDefault(); //keep requesting, as we don't want to keep this app as default until features are complete.
    }

    private void changeSmsAppToDefault() {
        if (defaultSmsApp == null || defaultSmsApp.equals(getPackageName()))
            defaultSmsApp = "com.google.android.apps.messaging"; //no need to check if installed, android will open a selection dialog. TODO test with older android

        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsApp);
        startActivityForResult(intent, REQUEST_CODE_CHANGE_SMS_APP_TO_DEFAULT);
    }

    private void changeSmsAppToThis() {
        defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);

        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, REQUEST_CODE_CHANGE_SMS_APP_TO_THIS);
    }

    private void checkApp() {
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName())) {
            changeSmsAppToThis();
        } else {
            writeSmsToStore();
            changeSmsAppToDefault();
        }
    }

    private void saveSms() {
        checkApp();
    }

    //Write the sms
    private void writeSmsToStore() {

        ArrayList<MessageDbHelper.Text> textMessageList = MessageDbHelper.getInstance().getTextMessageList();

        ContentResolver contentResolver = getContentResolver();
        for (MessageDbHelper.Text text : textMessageList) {
            if (text.hasExported())
                continue;

            String address = text.getAddress();
            if (address == null)
                address = "Unknown";

            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, address);
            values.put(Telephony.Sms.DATE, text.getDate());
            values.put(Telephony.Sms.BODY, text.getBody());
            values.put(Telephony.Sms.READ, text.hasRead());

            if (text.getType() == MessageDbHelper.Text.MESSAGE_TYPE_INBOX)
                contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values);
            else if (text.getType() == MessageDbHelper.Text.MESSAGE_TYPE_SENT)
                contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values);

            text.setExported(true);
            MessageDbHelper.getInstance().updateMessage(text);
        }

        Toast.makeText(this, "Text messages has been successfully exported.", Toast.LENGTH_SHORT).show();
    }


}