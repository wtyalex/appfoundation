package com.wty.foundation.common.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.wty.foundation.R;
import com.wty.foundation.common.init.AppContext;

import java.io.ByteArrayOutputStream;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";
    private static final String DEFAULT_CHANNEL_ID = "default_channel_id";
    private Context context;
    private NotificationManager notificationManager;
    private int notificationIdCounter = 0;

    public NotificationUtils() {
        this.context = AppContext.getInstance().getContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            throw new IllegalStateException("Notification service not available");
        }
        createDefaultChannel();
    }

    /**
     * 创建默认的通知渠道
     */
    private void createDefaultChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, "默认频道", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("这是默认的通知频道");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 创建或更新自定义通知渠道
     *
     * @param channelId   渠道ID
     * @param channelName 渠道名称
     * @param description 渠道描述
     * @param importance  渠道重要性
     */
    public synchronized void createCustomChannel(String channelId, String channelName, String description, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
            int validImportance = validateImportance(importance);
            if (existingChannel != null && shouldUpdateChannel(existingChannel, channelName, description, validImportance)) {
                notificationManager.deleteNotificationChannel(channelId); // 删除旧渠道前检查并取消相关通知
                cancelNotificationsByChannelId(channelId);
            }
            NotificationChannel channel = new NotificationChannel(channelId, channelName, validImportance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private boolean shouldUpdateChannel(NotificationChannel existingChannel, String newName, String newDescription, int newImportance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return !(existingChannel.getName().equals(newName) && existingChannel.getDescription().equals(newDescription) && existingChannel.getImportance() == newImportance);
        }
        return false; // 如果当前API级别低于Oreo，则不需要更新渠道
    }

    /**
     * 验证通知渠道的重要性级别
     *
     * @param importance 重要性级别
     * @return 有效的重要性级别
     */
    private int validateImportance(int importance) {
        if (importance < NotificationManager.IMPORTANCE_NONE || importance > NotificationManager.IMPORTANCE_HIGH) {
            Log.w(TAG, "Invalid importance level: " + importance + ", using default.");
            return NotificationManager.IMPORTANCE_DEFAULT;
        }
        return importance;
    }

    /**
     * 显示普通通知
     *
     * @param title     通知标题
     * @param message   通知内容
     * @param iconResId 图标资源ID
     * @param intent    点击通知后的跳转意图
     * @param priority  通知优先级
     * @param color     通知颜色
     * @param soundUri  通知声音
     */
    public synchronized void showNotification(String title, String message, @DrawableRes int iconResId, Intent intent, int priority, Integer color, Uri soundUri) {
        if (!isResourceValid(iconResId)) {
            Log.e(TAG, "Invalid icon resource ID provided for showNotification");
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(intent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(title).setContentText(message).setColor(color != null ? color : Color.RED).setPriority(validatePriority(priority)).setSound(soundUri != null ? soundUri : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).setContentIntent(pendingIntent).setAutoCancel(true);

        try {
            notificationManager.notify(getNextNotificationId(), builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    /**
     * 验证通知优先级
     *
     * @param priority 优先级
     * @return 有效的优先级
     */
    private int validatePriority(int priority) {
        if (priority < NotificationCompat.PRIORITY_MIN || priority > NotificationCompat.PRIORITY_MAX) {
            Log.w(TAG, "Invalid priority level: " + priority + ", using default.");
            return NotificationCompat.PRIORITY_DEFAULT;
        }
        return priority;
    }

    /**
     * 创建PendingIntent
     *
     * @param intent 意图
     * @return PendingIntent
     */
    private PendingIntent createPendingIntent(Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    /**
     * 获取下一个通知ID
     *
     * @return 通知ID
     */
    private synchronized int getNextNotificationId() {
        return ++notificationIdCounter;
    }

    /**
     * 验证资源ID是否有效
     *
     * @param resourceId 资源ID
     * @return 是否有效
     */
    private boolean isResourceValid(@DrawableRes int resourceId) {
        try {
            context.getResources().getDrawable(resourceId, null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Invalid resource ID: " + resourceId, e);
            return false;
        }
    }

    /**
     * 显示大文本通知
     *
     * @param title          通知标题
     * @param bigText        大文本内容
     * @param iconResId      图标资源ID
     * @param intent         点击通知后的跳转意图
     * @param notificationId 通知ID
     */
    public synchronized void showBigTextNotification(String title, String bigText, @DrawableRes int iconResId, Intent intent, int notificationId) {
        // 检查图标资源ID是否有效
        if (!isResourceValid(iconResId)) {
            Log.e(TAG, "无效的图标资源ID，无法显示大文本通知");
            return;
        }

        // 创建点击通知后触发的PendingIntent
        PendingIntent pendingIntent = createPendingIntent(intent);

        // 如果文本非常长，则对其进行分割处理
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle().bigText(splitBigText(bigText));

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(title).setStyle(bigTextStyle).setContentIntent(pendingIntent).setAutoCancel(true);

        try {
            // 发送通知
            notificationManager.notify(notificationId, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "显示大文本通知时出错", e);
        }
    }

    /**
     * 分割大文本
     * <p>
     * 当文本长度超过一定限制时，将其分割为多个部分，避免一次性加载过大的文本内容
     *
     * @param bigText 要分割的大文本
     * @return 分割后的文本
     */
    private String splitBigText(String bigText) {
        final int MAX_CHUNK_SIZE = 3000; // 假设每个分割块的最大大小为3000字符
        if (bigText.length() <= MAX_CHUNK_SIZE) {
            // 如果文本小于或等于最大块大小，则直接返回原文本
            return bigText;
        } else {
            StringBuilder result = new StringBuilder();
            int start = 0;
            while (start < bigText.length()) {
                int end = Math.min(start + MAX_CHUNK_SIZE, bigText.length());
                result.append(bigText.substring(start, end));
                start = end;
                if (start < bigText.length()) {
                    result.append("\n"); // 在每个分割点后加上换行符，以便阅读
                }
            }
            return result.toString();
        }
    }

    /**
     * 显示大图片通知
     *
     * @param title          通知标题
     * @param bigPicture     大图片
     * @param iconResId      图标资源ID
     * @param intent         点击通知后的跳转意图
     * @param notificationId 通知ID
     */
    public synchronized void showBigPictureNotification(String title, Bitmap bigPicture, @DrawableRes int iconResId, Intent intent, int notificationId) {
        // 检查图标资源ID是否有效或大图片是否为空
        if (!isResourceValid(iconResId) || bigPicture == null) {
            Log.e(TAG, "无效的图标资源ID或者未提供大图片，无法显示大图片通知");
            return;
        }

        // 压缩图片以防止内存溢出，并指定压缩质量为70（可根据实际需求调整）
        Bitmap compressedBitmap = compressImage(bigPicture, 70);

        PendingIntent pendingIntent = createPendingIntent(intent);

        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle().bigPicture(compressedBitmap);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(title).setStyle(bigPictureStyle).setContentIntent(pendingIntent).setAutoCancel(true);

        try {
            // 发送通知
            notificationManager.notify(notificationId, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "显示大图片通知时发生错误", e);
        }
    }

    /**
     * 压缩图片以减少内存占用
     *
     * @param image   需要压缩的图片
     * @param quality 图片压缩质量(0-100)，数值越高质量越好但文件越大
     * @return 压缩后的图片
     */
    private Bitmap compressImage(Bitmap image, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // 将Bitmap对象压缩成JPEG格式并写入ByteArrayOutputStream中，quality控制压缩质量
        image.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] byteArray = stream.toByteArray();

        // 使用decodeByteArray方法将字节数组转换回Bitmap对象
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    /**
     * 释放资源，取消所有通知
     */
    public synchronized void release() {
        notificationManager.cancelAll();
    }

    /**
     * 根据通知ID取消通知
     *
     * @param notificationId 通知ID
     */
    public synchronized void cancelNotificationById(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    /**
     * 根据渠道ID取消通知
     *
     * @param channelId 渠道ID
     */
    public synchronized void cancelNotificationsByChannelId(String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification statusBarNotification : activeNotifications) {
                if (statusBarNotification.getNotification().getChannelId().equals(channelId)) {
                    notificationManager.cancel(statusBarNotification.getId());
                }
            }
        }
    }

    /**
     * 显示带操作按钮的通知
     *
     * @param title          通知标题
     * @param message        通知内容
     * @param iconResId      图标资源ID
     * @param intent         点击通知后的跳转意图
     * @param notificationId 通知ID
     * @param priority       通知优先级
     * @param color          通知颜色
     * @param soundUri       通知声音
     */
    public synchronized void showNotificationWithActions(String title, String message, @DrawableRes int iconResId, Intent intent, int notificationId, int priority, Integer color, Uri soundUri) {
        if (!isResourceValid(iconResId)) {
            Log.e(TAG, "Invalid icon resource ID provided for showNotificationWithActions");
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(intent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(title).setContentText(message).setColor(color != null ? color : Color.RED).setPriority(validatePriority(priority)).setSound(soundUri != null ? soundUri : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).setContentIntent(pendingIntent).addAction(R.drawable.ui_icon_pause, "暂停", createActionPendingIntent("pause")).addAction(R.drawable.ui_icon_cancel, "取消", createActionPendingIntent("cancel")).setAutoCancel(true);

        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification with actions", e);
        }
    }

    /**
     * 创建操作按钮的PendingIntent
     *
     * @param action 操作类型
     * @return PendingIntent
     */
    private PendingIntent createActionPendingIntent(String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}