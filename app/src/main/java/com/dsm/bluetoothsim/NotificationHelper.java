package com.dsm.bluetoothsim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;

/**
 * Created by dsm on 11/30/17.
 */

public class NotificationHelper {

    private NotificationCompat.Builder foregroundNotiBuilder;
    private Context context;
    private NotificationManager notificationManager;
    public static int SERVICE_NOTIFICATION_ID = (Application.class.getSimpleName() + "Service").hashCode();

    private String colorLime = "#8BC34A";

    public Notification startService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channel_id = "default";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel_id = "channel_service";
            NotificationChannel mChannel = new NotificationChannel(channel_id, context.getString(R.string.channel_service), NotificationManager.IMPORTANCE_MIN);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(mChannel);
        }

        foregroundNotiBuilder = new NotificationCompat.Builder(context, channel_id);
        foregroundNotiBuilder.setOngoing(true);
        foregroundNotiBuilder.setShowWhen(false);
        foregroundNotiBuilder.setContentTitle("Connecting to external card");
        foregroundNotiBuilder.setContentText("Service is running");
        foregroundNotiBuilder.setColor(Color.parseColor(colorLime));
        foregroundNotiBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        foregroundNotiBuilder.setSmallIcon(R.drawable.ic_bluetooth_searching_white);
        return foregroundNotiBuilder.build();
    }

    public void updateService(@DrawableRes int smallIcon, String title, String contentText, String info) {
        foregroundNotiBuilder.setContentTitle(title);
        foregroundNotiBuilder.setContentText(contentText);
        foregroundNotiBuilder.setContentInfo(info);
        foregroundNotiBuilder.setSmallIcon(smallIcon);

        notificationManager.notify(SERVICE_NOTIFICATION_ID, foregroundNotiBuilder.build());
    }

    public void updateService(String title, String contentText, String info) {
        foregroundNotiBuilder.setContentTitle(title);
        foregroundNotiBuilder.setContentText(contentText);
        foregroundNotiBuilder.setContentInfo(info);

        notificationManager.notify(SERVICE_NOTIFICATION_ID, foregroundNotiBuilder.build());
    }
}
