package com.dsm.bluetoothsim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

/**
 * Created by dsm on 11/30/17.
 */

public class NotificationHelper {

    private NotificationCompat.Builder foregroundBuilder;
    private Context context;
    private NotificationManager notificationManager;
    public static int SERVICE_NOTIFICATION_ID = (Application.class.getSimpleName() + "Service").hashCode();

    public static final String KEY_SMS_REPLY = "KEY_SMS_REPLY";

    @ColorInt private int colorLightGreen = Color.parseColor("#8BC34A");
    @ColorInt private int colorLime = Color.parseColor("#8BC34A");
    @ColorInt private int colorBlue = Color.parseColor("#2196F3");
    @ColorInt private int colorIndigo = Color.parseColor("#3F51B5");
    @ColorInt private int colorRed = Color.parseColor("#F44336");

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channel_id = "default";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel_id = "channel_service";
            NotificationChannel mChannel = new NotificationChannel(channel_id, context.getString(R.string.channel_service), NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(mChannel);
        }

        foregroundBuilder = new NotificationCompat.Builder(context, channel_id);
        foregroundBuilder.setOngoing(true);
        foregroundBuilder.setShowWhen(false);
        foregroundBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
    }

    public Notification getServiceNotification() {

        Intent intent = new Intent(context, BluetoothService.class);
        intent.setAction(BluetoothService.ACTION_STOP_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, SERVICE_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        foregroundBuilder.setContentTitle("Service is running in the background")
                .setSmallIcon(R.drawable.ic_bluetooth_disabled_white)
                .addAction(R.drawable.ic_close_white, context.getText(R.string.turn_off), pendingIntent);

        return foregroundBuilder.build();
    }

    public void updateService(@DrawableRes int smallIcon, @ColorInt int color, String title, String contentText, String info, boolean showWhen) {
        foregroundBuilder.setContentTitle(title);
        foregroundBuilder.setContentText(contentText);
        foregroundBuilder.setContentInfo(info);
        foregroundBuilder.setSmallIcon(smallIcon);
        foregroundBuilder.setColor(color);
        foregroundBuilder.setShowWhen(showWhen);
        if (showWhen)
            foregroundBuilder.setWhen(System.currentTimeMillis());

        notificationManager.notify(SERVICE_NOTIFICATION_ID, foregroundBuilder.build());
    }

    public void notifyDisconnected(String text) {
        updateService(R.drawable.ic_bluetooth_disabled_white, colorRed, "Disconnected", text, null, true);
    }

    public void notifyDisconnected() {
        notifyDisconnected(null);
    }

    public void notifyConnecting(String text) {
        updateService(R.drawable.ic_bluetooth_searching_white, colorBlue, "Connecting", text, null, false);
    }

    public void notifyConnecting() {
        notifyConnecting(null);
    }

    public void notifyConnected(String text) {
        updateService(R.drawable.ic_bluetooth_connected_white, colorLightGreen, "Connected", text, null, false);
    }

    public void notifyConnected() {
        notifyConnected(null);
    }

    public void notifySMS(String phoneNumber, String content) {
        if (content == null || content.length() == 0) content = "Empty message";
        if (phoneNumber == null || phoneNumber.length() == 0) phoneNumber = "Unknown";
        //TODO disable reply if phoneNumber is unknown

        String channelId = "default";
        String groupSms = "group_sms";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "channel_sms";
            NotificationChannel mChannel = new NotificationChannel(channelId, context.getString(R.string.channel_sms), NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableLights(true);
            mChannel.setLightColor(colorIndigo);
            mChannel.enableVibration(true);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(context, channelId);
        groupBuilder.setSubText(context.getString(R.string.messages))
                .setSmallIcon(R.drawable.ic_message_white)
                .setColor(colorIndigo)
                .setGroup(groupSms)
                .setGroupSummary(true)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(false);

        int notificationId = (phoneNumber + content + System.currentTimeMillis()).hashCode();

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_SMS_REPLY)
                .setLabel(context.getString(R.string.quick_reply_label)).build();

        Intent replyIntent = new Intent(BluetoothService.ACTION_SMS_REPLY);
        replyIntent.putExtra("PHONE_NUMBER", phoneNumber);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context, notificationId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_reply_white, context.getString(R.string.reply), replyPendingIntent)
                .setAllowGeneratedReplies(true)
                .addRemoteInput(remoteInput).build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setContentTitle(CallLogUtility.getContactDisplayName(context, phoneNumber))
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_message_white)
                .setLargeIcon(CallLogUtility.retrieveContactPhoto(context, phoneNumber))
                .setColor(colorIndigo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setGroup(groupSms)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(false)
                .addAction(replyAction);

        notificationManager.notify(groupSms.hashCode(), groupBuilder.build());
        notificationManager.notify(notificationId, builder.build());
    }
}
