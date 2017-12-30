package com.dsm.bluetoothsim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import com.dsm.bluetoothsim.ui.PhoneActivity;
import com.dsm.bluetoothsim.ui.widget.LetterTileProvider;
import com.dsm.bluetoothsim.util.CallLogUtility;
import com.dsm.bluetoothsim.util.TextUtils;
import com.dsm.bluetoothsim.util.Utils;

/**
 * Created by dsm on 11/30/17.
 */

public class NotificationHelper {

    public static int SERVICE_NOTIFICATION_ID = (Application.class.getSimpleName() + "Service").hashCode();
    private static int INCOMING_CALL_NOTI_ID = 4;
    private static int ONGOING_CALL_NOTI_ID = 5;
    private static int OUTGOING_CALL_NOTI_ID = 6;
    public static final String REMOTE_INPUT_KEY_SMS_REPLY = "SMS_REPLY";

    String CHANNEL_DEFAULT = "default";
    String CHANNEL_SMS = "channel_sms";
    String CHANNEL_INCOMING_CALLS = "channel_incoming_calls";
    String CHANNEL_ONGOING_CALLS = "channel_ongoing_calls";
    String CHANNEL_MISSED_CALLS = "channel_missed_calls";
    String GROUP_ONGOING_CALL = "group_ongoing_call";
    String GROUP_MISSED_CALLS = "group_missed_calls";
    String GROUP_SMS = "group_sms";

    private Context context;

    private NotificationCompat.Builder foregroundBuilder;
    private NotificationCompat.Action actionConnect;
    private NotificationCompat.Action actionDisconnect;
    private NotificationCompat.Action actionTurnOff;
    private NotificationCompat.Action actionRefresh;
    private NotificationManager notificationManager;
    private LetterTileProvider letterTileProvider;

    private int signalValue;
    private boolean isCharging;
    private int batteryLevel;
    private String opNumber;
    private String opName;
    private int networkStatus;

    @ColorInt
    private int colorLightGreenA700 = Color.parseColor("#64DD17");
    @ColorInt
    private int colorBlue = Color.parseColor("#2196F3");
    @ColorInt
    private int colorIndigo = Color.parseColor("#3F51B5");
    @ColorInt
    private int colorRed = Color.parseColor("#F44336");

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.letterTileProvider = new LetterTileProvider(context);

        String channel_id = "default";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel_id = "channel_service";
            NotificationChannel mChannel = new NotificationChannel(channel_id, context.getString(R.string.channel_service), NotificationManager.IMPORTANCE_LOW);
            mChannel.setShowBadge(false);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(mChannel);
        }

        foregroundBuilder = new NotificationCompat.Builder(context, channel_id);
        foregroundBuilder.setOngoing(true);
        foregroundBuilder.setShowWhen(false);
        foregroundBuilder.setPriority(NotificationCompat.PRIORITY_LOW);

        createServiceActions();
    }

    private void createServiceActions() {
        PendingIntent piConnect = PendingIntent.getService(context, SERVICE_NOTIFICATION_ID,
                new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_SERVICE_CONNECT),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piDisconnect = PendingIntent.getService(context, SERVICE_NOTIFICATION_ID,
                new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_SERVICE_DISCONNECT),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piTurnOff = PendingIntent.getService(context, SERVICE_NOTIFICATION_ID,
                new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_SERVICE_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piRefresh = PendingIntent.getService(context, SERVICE_NOTIFICATION_ID,
                new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_SERVICE_REFRESH),
                PendingIntent.FLAG_UPDATE_CURRENT);

        actionConnect = new NotificationCompat.Action(R.drawable.ic_bluetooth_searching_white, context.getString(R.string.connect), piConnect);
        actionDisconnect = new NotificationCompat.Action(R.drawable.ic_bluetooth_disabled_white, context.getString(R.string.disconnect), piDisconnect);
        actionTurnOff = new NotificationCompat.Action(R.drawable.ic_close_white, context.getString(R.string.turn_off), piTurnOff);
        actionRefresh = new NotificationCompat.Action(R.drawable.ic_refresh_white, context.getString(R.string.refresh), piRefresh);
    }

    public Notification getServiceNotification() {
        foregroundBuilder.setContentTitle("Service is running in the background");
        foregroundBuilder.setSmallIcon(R.drawable.ic_bluetooth_disabled_white);
        addConnectActions();

        return foregroundBuilder.build();
    }

    public void updateService(@DrawableRes int smallIcon, @ColorInt int color, String title, boolean showWhen) {
        foregroundBuilder.setContentTitle(title);
//        foregroundBuilder.setContentText(contentText);
        foregroundBuilder.setContentInfo(null);
        foregroundBuilder.setSmallIcon(smallIcon);
        foregroundBuilder.setColor(color);
        foregroundBuilder.setShowWhen(showWhen);
        if (showWhen)
            foregroundBuilder.setWhen(System.currentTimeMillis());

        notificationManager.notify(SERVICE_NOTIFICATION_ID, foregroundBuilder.build());
    }

    public void notifyDisconnected(String text) {
        addConnectActions();
        updateService(R.drawable.ic_bluetooth_disabled_white, colorRed, "Disconnected" + (text != null ? ": " + text : null), true);
    }

    public void notifyConnecting() {
        addActions();
        updateService(R.drawable.ic_bluetooth_searching_white, colorBlue, "Connecting\u2026", false);
    }

    public void notifyConnected() {
        addDisconnectActions();
        updateService(R.drawable.ic_bluetooth_connected_white, colorLightGreenA700, "Connected", false);
    }

    private void addActions() {
        foregroundBuilder.mActions.clear();
        foregroundBuilder.addAction(actionTurnOff);
    }

    private void addConnectActions() {
        foregroundBuilder.mActions.clear();
        foregroundBuilder.addAction(actionConnect);
        foregroundBuilder.addAction(actionTurnOff);
    }

    private void addDisconnectActions() {
        foregroundBuilder.mActions.clear();
        foregroundBuilder.addAction(actionDisconnect);
        foregroundBuilder.addAction(actionRefresh);
        foregroundBuilder.addAction(actionTurnOff);
    }

    private void updateServiceStatus() {
        //assert it's connected
        String batteryInfo = "Battery " + batteryLevel + "%" + (isCharging ? " (+)" : "");
        String operator = networkStatus == /*TODO replace*/2 ? "Searching\u2026" : (opName != null ? opName : opNumber);
        String signal = signalValue == 0 ? "No signal" : "Signal " + signalValue;
        String network = String.format("%s (Status %s) | %s", operator == null ? "No network" : operator, networkStatus, signal);
        foregroundBuilder.setContentTitle(network);
        foregroundBuilder.setContentInfo(batteryInfo);

        notificationManager.notify(SERVICE_NOTIFICATION_ID, foregroundBuilder.build());
    }

    public void notifySignal(int value) {
        this.signalValue = value;
        updateServiceStatus();
    }

    public void notifyBattery(int level, boolean isCharging) {
        this.batteryLevel = level;
        this.isCharging = isCharging;
        updateServiceStatus();
    }

    public void notifyNetwork(int status, String opName, String opNumber) {
        this.networkStatus = status;
        this.opName = opName;
        this.opNumber = opNumber;
        if (this.opName == null)
            this.opName = NetworkOperators.getName(opNumber);
        updateServiceStatus();
    }

    private String createSMSChannel() {
        String channelId = CHANNEL_DEFAULT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = CHANNEL_SMS;
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.channel_sms), NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(colorIndigo);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    private String createIncomingCallsChannel() {
        String channelId = CHANNEL_DEFAULT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = CHANNEL_INCOMING_CALLS;
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.channel_incoming_calls), NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    private String createOngoingCallChannel() {
        String channelId = CHANNEL_DEFAULT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = CHANNEL_ONGOING_CALLS;
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.channel_ongoing_calls), NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    private String createMissedCallChannel() {
        String channelId = CHANNEL_DEFAULT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = CHANNEL_MISSED_CALLS;
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.channel_missed_calls), NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(colorRed);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    private void addSMSActions(NotificationCompat.Builder builder, @Nullable String phoneNumber, int requestCode) {
        boolean enableReply = phoneNumber != null && TextUtils.isNumeric(phoneNumber);

        Intent intentMarkRead = new Intent(context, BluetoothService.class)
                .setAction(BluetoothService.ACTION_SMS_MARK_READ);
        PendingIntent piMarkRead = PendingIntent.getService(context, requestCode, intentMarkRead, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteInput remoteInput = new RemoteInput.Builder(REMOTE_INPUT_KEY_SMS_REPLY)
                .setLabel(context.getString(R.string.quick_reply_label)).build();
        PendingIntent replyPendingIntent = null;
        if (enableReply) {
            Intent intentReply = new Intent(context, BluetoothService.class)
                    .setAction(BluetoothService.ACTION_SMS_REPLY)
                    .putExtra("PHONE_NUMBER", phoneNumber);
            replyPendingIntent = PendingIntent.getService(context, requestCode, intentReply, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_reply_white, context.getString(R.string.reply), replyPendingIntent)
                .setAllowGeneratedReplies(true)
                .addRemoteInput(remoteInput).build();

        builder.addAction(R.drawable.ic_mark_read_white, context.getString(R.string.mark_read), piMarkRead);
        builder.addAction(replyAction);
    }

    private void setupDisplayName(NotificationCompat.Builder builder, @Nullable String phoneNumber) {
        String displayName = CallLogUtility.getContactDisplayName(context, phoneNumber);
        Bitmap contactPhoto = CallLogUtility.getContactPhoto(context, phoneNumber);

        if (contactPhoto == null)
            contactPhoto = letterTileProvider.getLetterTile(displayName, Utils.getRandomMaterial500Color(context));

        if (displayName == null)
            displayName = phoneNumber != null ? phoneNumber : "Unknown";

        builder.setContentTitle(displayName);
        builder.setLargeIcon(Utils.getCircleBitmap(contactPhoto));
    }

    public void notifySMS(@Nullable String phoneNumber, @Nullable String content) {
        String channelId = createSMSChannel();

        NotificationCompat.Builder groupBuilder = new NotificationCompat.Builder(context, channelId);
        groupBuilder.setSubText(context.getString(R.string.messages)) //TODO multiple icon
                .setSmallIcon(R.drawable.ic_message_white)
                .setColor(colorIndigo)
                .setGroup(GROUP_SMS)
                .setGroupSummary(true)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(false);

        int notificationId = ((phoneNumber != null ? phoneNumber : "") + content + System.currentTimeMillis()).hashCode();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        setupDisplayName(builder, phoneNumber);
        builder.setContentText(content)
                .setSubText(context.getString(R.string.message))
                .setSmallIcon(R.drawable.ic_message_white)
                .setColor(colorIndigo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setGroup(GROUP_SMS)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(false);
        addSMSActions(builder, phoneNumber, notificationId);

        notificationManager.notify(GROUP_SMS.hashCode(), groupBuilder.build());
        notificationManager.notify(notificationId, builder.build());
    }

    private void addCallActions(NotificationCompat.Builder builder, @Nullable String phoneNumber, boolean isOffHook) {
        Intent intent = new Intent(context, PhoneActivity.class)
                .putExtra("PHONE_NUMBER", phoneNumber);
        PendingIntent piActivity = PendingIntent.getActivity(context, "Activity".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentAnswer = new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_CALL_ANSWER);
        Intent intentHangUp = new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_CALL_HANG_UP);
        PendingIntent piAnswer = PendingIntent.getService(context, "PiAnswer".hashCode(), intentAnswer, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piHangup = PendingIntent.getService(context, "PiHangup".hashCode(), intentHangUp, PendingIntent.FLAG_UPDATE_CURRENT);

        if (isOffHook) {
            builder.setContentIntent(piActivity);

            builder.addAction(R.drawable.ic_call_end_white, context.getString(R.string.hang_up), piHangup);
        } else {
            builder.setFullScreenIntent(piActivity, true);

            builder.addAction(R.drawable.ic_call_white, context.getString(R.string.answer), piAnswer);
            builder.addAction(R.drawable.ic_call_end_white, context.getString(R.string.decline), piHangup);
        }
    }

    private void prepareCallNotification(NotificationCompat.Builder builder, @Nullable String phoneNumber, boolean isOngoing) {
        setupDisplayName(builder, phoneNumber);
        builder.setSubText(context.getString(R.string.phone));
        builder.setCategory(NotificationCompat.CATEGORY_CALL);

        if (isOngoing) {
            builder.setOngoing(true);
            builder.setShowWhen(false);
            builder.setGroup(GROUP_ONGOING_CALL);
            builder.setColor(colorBlue);
        } else {
            builder.setOngoing(false);
            builder.setShowWhen(true);
            builder.setGroup(GROUP_MISSED_CALLS);
            builder.setColor(colorRed);
        }
    }

    public void notifyIncomingCall(@Nullable String phoneNumber) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, createIncomingCallsChannel());
        builder.setContentText(context.getString(R.string.incoming_call));
        builder.setSmallIcon(R.drawable.ic_call_received_white);

        prepareCallNotification(builder, phoneNumber, true);
        addCallActions(builder, phoneNumber, false);

        notificationManager.notify(ONGOING_CALL_NOTI_ID, builder.build());
    }

    public void notifyOngoingCall(@Nullable String phoneNumber) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, createOngoingCallChannel());
        builder.setContentText(context.getString(R.string.ongoing_call));
        builder.setSmallIcon(R.drawable.ic_call_white);

        prepareCallNotification(builder, phoneNumber, true);
        addCallActions(builder, phoneNumber, true);

        notificationManager.notify(ONGOING_CALL_NOTI_ID, builder.build());
    }

    public void notifyMissedCall(@Nullable String phoneNumber) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, createMissedCallChannel());
        builder.setContentText(context.getText(R.string.missed_call));
        builder.setSmallIcon(R.drawable.ic_call_missed_white);

        prepareCallNotification(builder, phoneNumber, false);

        if (phoneNumber != null) {
            Intent intentCall = new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_CALL)
                    .putExtra("PHONE_NUMBER", phoneNumber);
            PendingIntent piCallback = PendingIntent.getService(context, phoneNumber.hashCode(), intentCall, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_call_white, context.getString(R.string.call_back), piCallback);
            builder.addAction(R.drawable.ic_message_white, context.getString(R.string.message), null/*TODO*/);
        }

        notificationManager.notify(("missed_call" + phoneNumber + System.currentTimeMillis()).hashCode(), builder.build());
    }

    public void notifyCallDisconnected() {
        notificationManager.cancel(ONGOING_CALL_NOTI_ID);
    }

    public static void notifyLog(@Nullable String phoneNumber, @Nullable String content) {
        Context context = Application.getAppContext();
        int colorRed = Color.parseColor("#F44336");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "default";
        String groupSms = "group_sms";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = "channel_sms";
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.channel_sms), NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(colorRed);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = ((phoneNumber != null ? phoneNumber : "") + content + System.currentTimeMillis()).hashCode();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
        builder.setContentTitle(phoneNumber)
                .setContentText(content)
                .setSubText(context.getString(R.string.message))
                .setSmallIcon(R.drawable.ic_message_white)
                .setColor(colorRed)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setGroup(groupSms)
                .setShowWhen(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOngoing(false);

        notificationManager.notify(notificationId, builder.build());
    }

    /*public void notifyOutgoingCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() == 0) phoneNumber = "Unknown";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "default";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel_id = "channel_outgoing_calls";
            NotificationChannel channel = new NotificationChannel(channel_id, context.getString(R.string.channel_outgoing_calls), NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this.context, PhoneActivity.class);
        Intent intentHangup = new Intent(context, BluetoothService.class).setAction(BluetoothService.ACTION_CALL_HANG_UP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.context, "Activity".hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent piHangup = PendingIntent.getService(context, "PiHangup".hashCode(), intentHangup, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel_id);
        builder.setContentTitle(CallLogUtility.getContactDisplayName(context, phoneNumber))
                .setContentText(context.getText(R.string.outgoing_call))
                .setSmallIcon(R.drawable.ic_call_made_white)
                .setLargeIcon(CallLogUtility.retrieveCircularContactPhoto(context, phoneNumber))
                .setOngoing(true)
                .setShowWhen(false)
                .setColor(colorBlue)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_call_end_white, context.getString(R.string.hang_up), piHangup);

        notificationManager.notify(ONGOING_CALL_NOTI_ID, builder.build());
    }*/

}
