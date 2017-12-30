package com.dsm.bluetoothsim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.NonNull;
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

public class DeviceActivity extends Activity {

    private static final int REQUEST_CODE = 1;
    EditText ed;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
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
                BTDevice.getInstance().queryBatteryInfo();
                BTDevice.getInstance().querySignal();
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
                if (ret)
                    startActivity(new Intent(DeviceActivity.this, PhoneActivity.class).putExtra("PHONE_NUMBER", ed.getText().toString()));
            }
        });

        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("content://contacts");
                Intent intent = new Intent(Intent.ACTION_PICK, uri);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                //intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

    }


}