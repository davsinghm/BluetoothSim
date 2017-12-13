package com.dsm.bluetoothsim.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.dsm.bluetoothsim.BTDevice;
import com.dsm.bluetoothsim.CallLogUtility;
import com.dsm.bluetoothsim.R;
import com.dsm.bluetoothsim.BluetoothService;

public class PhoneActivity extends AppCompatActivity implements View.OnClickListener {
    BTDevice extendCard;
    String contactDisplayName;
    String contactNumber;
    Context mContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_call);
        mContext = this;

        extendCard = BTDevice.getInstance();
        extendCard.answer();

        contactNumber = getIntent().getStringExtra("contactNumber");
        contactDisplayName = getIntent().getStringExtra("contactDisplayName");

        Button hangupButton = findViewById(R.id.hangup);
        hangupButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.hangup:
                extendCard.hangup();
                break;
        }
    }

    public static void notifyIncomingCall(Context mContext, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() == 0) phoneNumber = "Unknown";

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "default";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel_id = "channel_incoming_calls";
            NotificationChannel mChannel = new NotificationChannel(channel_id, mContext.getString(R.string.channel_incoming_calls), NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableVibration(false);
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent intent = new Intent(mContext, PhoneActivity.class);
        Intent intentAnswer = new Intent(BluetoothService.ACTION_CALL_ANSWER);
        Intent intentDecline = new Intent(BluetoothService.ACTION_CALL_DECLINE);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, "Activity".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piAnswer = PendingIntent.getBroadcast(mContext, "PiAnswer".hashCode(), intentAnswer, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piDecline = PendingIntent.getBroadcast(mContext, "PiDecline".hashCode(), intentDecline, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, channel_id);
        builder.setContentTitle(CallLogUtility.getContactDisplayName(mContext, phoneNumber))
                .setContentText(mContext.getText(R.string.incoming_call))
                .setSmallIcon(R.drawable.ic_call_received_white)
                .setLargeIcon(CallLogUtility.retrieveContactPhoto(mContext, phoneNumber))
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(Color.parseColor("#2196F3")) //blue 500
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_launcher_background, /*TODO change icon*/ mContext.getString(R.string.answer), piAnswer)
                .addAction(R.drawable.ic_call_end_white, mContext.getString(R.string.decline), piDecline);

        notificationManager.notify("incoming_call".hashCode(), builder.build());
    }

    public static void notifyOutgoingCall(Context mContext, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() == 0) phoneNumber = "Unknown";

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "default";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel_id = "channel_outgoing_calls";
            NotificationChannel mChannel = new NotificationChannel(channel_id, mContext.getString(R.string.channel_outgoing_calls), NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableVibration(false);
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent intentHangup = new Intent(BluetoothService.ACTION_CALL_HANG_UP);
        PendingIntent piHangup = PendingIntent.getBroadcast(mContext, "PiHangup".hashCode(), intentHangup, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, channel_id);
        builder.setContentTitle(CallLogUtility.getContactDisplayName(mContext, phoneNumber))
                .setContentText(mContext.getText(R.string.outgoing_call))
                .setSmallIcon(R.drawable.ic_call_made_white)
                .setLargeIcon(CallLogUtility.retrieveContactPhoto(mContext, phoneNumber))
                .setOngoing(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.parseColor("#2196F3")) //blue 500
                .addAction(R.drawable.ic_call_end_white, mContext.getString(R.string.hang_up), piHangup);

        notificationManager.notify("outgoing_call".hashCode(), builder.build());
    }

    public static void notifyMissedCall(Context mContext, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() == 0) phoneNumber = "Unknown";

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "default";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel_id = "channel_missed_calls";
            NotificationChannel mChannel = new NotificationChannel(channel_id, mContext.getString(R.string.channel_missed_calls), NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.parseColor("#F44336")); //red 500
            mChannel.enableVibration(true);
            notificationManager.createNotificationChannel(mChannel);
        }

        /*Intent intent = new Intent(mContext, PhoneActivity.class);
        Intent intentAnswer = new Intent(BluetoothService.PENDING_INTENT_ANSWER);
        Intent intentHangup = new Intent(BluetoothService.PENDING_INTENT_HANG_UP);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, "Activity".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piAnswer = PendingIntent.getBroadcast(mContext, "PiAnswer".hashCode(), intentAnswer, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piHangup = PendingIntent.getBroadcast(mContext, "PiHangup".hashCode(), intentHangup, PendingIntent.FLAG_UPDATE_CURRENT);
*/
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, channel_id);
        builder.setContentTitle(CallLogUtility.getContactDisplayName(mContext, phoneNumber))
                .setContentText(mContext.getText(R.string.missed_call))
                .setSmallIcon(R.drawable.ic_call_missed_white)
                .setLargeIcon(CallLogUtility.retrieveContactPhoto(mContext, phoneNumber))
                .setOngoing(false)
                .setShowWhen(true)
                .setColor(Color.parseColor("#F44336")) //red 500
                .addAction(R.drawable.ic_call_made_white, /*TODO change icon*/ mContext.getString(R.string.call_back), null)
                .addAction(R.drawable.ic_message_white, mContext.getString(R.string.message), null);

        notificationManager.notify(("missed_call" + phoneNumber + System.currentTimeMillis()).hashCode(), builder.build());
    }

    public static void ongoingIncomingCallNotification(Context mContext, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() == 0) phoneNumber = "Unknown";

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "default";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel_id = "channel_incoming_calls";
            NotificationChannel mChannel = new NotificationChannel(channel_id, mContext.getString(R.string.channel_incoming_calls), NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableVibration(false);
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent intent = new Intent(mContext, PhoneActivity.class);
        Intent intentDecline = new Intent(BluetoothService.ACTION_CALL_HANG_UP);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, "Activity".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piHangup = PendingIntent.getBroadcast(mContext, "PiHangup".hashCode(), intentDecline, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, channel_id);
        builder.setContentTitle(CallLogUtility.getContactDisplayName(mContext, phoneNumber))
                .setContentText(mContext.getText(R.string.incoming_call))
                .setSmallIcon(R.drawable.ic_call_received_white)
                .setLargeIcon(CallLogUtility.retrieveContactPhoto(mContext, phoneNumber))
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(Color.parseColor("#2196F3")) //blue 500
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_call_end_white, mContext.getString(R.string.hang_up), piHangup);

        notificationManager.notify("incoming_call".hashCode(), builder.build());
    }

    public static void dismissIncomingCallNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel("incoming_call".hashCode());
    }

    public static void dismissOutgoingCallNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel("outgoing_call".hashCode());
    }
}
