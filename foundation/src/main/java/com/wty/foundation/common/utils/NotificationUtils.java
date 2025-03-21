package com.wty.foundation.common.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.wty.foundation.common.init.AppContext;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    // 默认通知渠道 ID
    private static final String DEFAULT_CHANNEL_ID = "default_channel_id";
    // 大文本最大长度
    private static final int MAX_BIG_TEXT_LENGTH = 5000;
    // 标题最大长度
    private static final int MAX_TITLE_LENGTH = 100;
    // 内容最大长度
    private static final int MAX_CONTENT_LENGTH = 200;

    // 上下文对象
    private final Context context;
    // 通知管理器
    private final NotificationManager notificationManager;
    // 通知 ID 计数器
    private final AtomicInteger notificationIdCounter = new AtomicInteger(0);

    /**
     * 构造函数，初始化上下文和通知管理器，创建默认通知渠道
     */
    public NotificationUtils() {
        this.context = AppContext.getInstance().getContext();
        this.notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager == null) {
            Log.e(TAG, "Notification service unavailable");
        } else {
            createDefaultChannel();
        }
    }

    /**
     * 创建默认通知渠道
     */
    private void createDefaultChannel() {
        if (notificationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID);
            if (existingChannel == null) {
                // 创建通知渠道
                NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID, truncateString("默认频道", MAX_TITLE_LENGTH), NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("默认通知渠道");
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.setShowBadge(true);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 显示普通通知
     *
     * @param title     通知标题
     * @param message   通知内容
     * @param iconResId 通知小图标资源 ID
     * @param intent    点击通知后的意图
     * @param priority  通知优先级
     * @param color     通知颜色
     * @param soundUri  通知声音 URI
     */
    public void showNotification(@NonNull String title, @NonNull String message, @DrawableRes int iconResId, @Nullable Intent intent, int priority, @ColorInt int color, @Nullable Uri soundUri) {
        if (!isNotificationEnabled()) return;
        if (!validateIconResource(iconResId)) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(truncateString(title, MAX_TITLE_LENGTH)).setContentText(truncateString(message, MAX_CONTENT_LENGTH)).setColor(color).setPriority(validatePriority(priority)).setSound(getValidSound(soundUri)).setAutoCancel(true);

        setContentIntent(builder, intent);
        safelyNotify(builder.build(), notificationIdCounter.incrementAndGet());
    }

    /**
     * 设置通知的点击意图
     *
     * @param builder 通知构建器
     * @param intent  意图
     */
    private void setContentIntent(@NonNull NotificationCompat.Builder builder, @Nullable Intent intent) {
        PendingIntent pendingIntent = createSafePendingIntent(intent);
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }
    }

    /**
     * 安全地发送通知
     *
     * @param notification   通知对象
     * @param notificationId 通知 ID
     */
    private void safelyNotify(@NonNull Notification notification, int notificationId) {
        if (notificationManager == null) {
            Log.w(TAG, "Cannot notify: NotificationManager is null");
            return;
        }

        try {
            notificationManager.notify(notificationId, notification);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when sending notification: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * 检查通知是否可用
     *
     * @return 通知是否可用
     */
    private boolean isNotificationEnabled() {
        if (notificationManager == null) {
            Log.w(TAG, "NotificationManager unavailable");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission");
            return false;
        }
        return true;
    }

    /**
     * 验证图标资源是否有效
     *
     * @param resId 图标资源 ID
     * @return 图标资源是否有效
     */
    private boolean validateIconResource(@DrawableRes int resId) {
        try {
            ContextCompat.getDrawable(context, resId);
            return true;
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Invalid icon resource: " + resId);
            return false;
        }
    }

    /**
     * 创建安全的 PendingIntent
     *
     * @param intent 意图
     * @return 安全的 PendingIntent
     */
    private PendingIntent createSafePendingIntent(@Nullable Intent intent) {
        if (intent == null) return null;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        try {
            return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), // 唯一请求码
                    intent, flags);
        } catch (Exception e) {
            Log.e(TAG, "Create PendingIntent failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取有效的声音 URI
     *
     * @param soundUri 声音 URI
     * @return 有效的声音 URI
     */
    private Uri getValidSound(@Nullable Uri soundUri) {
        return (soundUri != null && isValidSoundUri(soundUri)) ? soundUri : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    /**
     * 验证声音 URI 是否有效
     *
     * @param uri 声音 URI
     * @return 声音 URI 是否有效
     */
    private boolean isValidSoundUri(@NonNull Uri uri) {
        try {
            context.getContentResolver().openInputStream(uri).close();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Invalid sound URI: " + uri);
            return false;
        }
    }

    /**
     * 验证通知优先级是否合法
     *
     * @param priority 通知优先级
     * @return 合法的通知优先级
     */
    private int validatePriority(int priority) {
        return (priority >= NotificationCompat.PRIORITY_MIN && priority <= NotificationCompat.PRIORITY_MAX) ? priority : NotificationCompat.PRIORITY_DEFAULT;
    }

    /**
     * 截断字符串到指定最大长度
     *
     * @param input     输入字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    @NonNull
    private String truncateString(@Nullable String input, int maxLength) {
        if (input == null) return "";
        return (input.length() > maxLength) ? input.substring(0, maxLength) + "…" : input;
    }

    /**
     * 显示大文本通知
     *
     * @param title          通知标题
     * @param bigText        大文本内容
     * @param iconResId      通知小图标资源 ID
     * @param intent         点击通知后的意图
     * @param notificationId 通知 ID
     */
    public void showBigTextNotification(@NonNull String title, @NonNull String bigText, @DrawableRes int iconResId, @Nullable Intent intent, int notificationId) {
        if (!isNotificationEnabled()) return;
        if (!validateIconResource(iconResId)) return;

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle().bigText(truncateString(bigText, MAX_BIG_TEXT_LENGTH));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(truncateString(title, MAX_TITLE_LENGTH)).setStyle(style).setAutoCancel(true);

        setContentIntent(builder, intent);
        safelyNotify(builder.build(), notificationId);
    }

    /**
     * 显示大图片通知
     *
     * @param title          通知标题
     * @param bitmap         大图片
     * @param iconResId      通知小图标资源 ID
     * @param intent         点击通知后的意图
     * @param notificationId 通知 ID
     */
    public void showBigPictureNotification(@NonNull String title, @NonNull Bitmap bitmap, @DrawableRes int iconResId, @Nullable Intent intent, int notificationId) {
        if (!isNotificationEnabled()) return;
        if (!validateIconResource(iconResId)) return;

        Bitmap safeBitmap = compressBitmap(bitmap, 768, 1024);
        if (safeBitmap == null) {
            Log.w(TAG, "Bitmap compression failed");
            return;
        }

        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle().bigPicture(safeBitmap).setSummaryText(truncateString(title, MAX_TITLE_LENGTH));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID).setSmallIcon(iconResId).setContentTitle(truncateString(title, MAX_TITLE_LENGTH)).setStyle(style).setAutoCancel(true);

        setContentIntent(builder, intent);
        safelyNotify(builder.build(), notificationId);
    }

    /**
     * 压缩位图
     *
     * @param src       原始位图
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 压缩后的位图
     */
    @Nullable
    private Bitmap compressBitmap(@NonNull Bitmap src, int maxWidth, int maxHeight) {
        try {
            int width = src.getWidth();
            int height = src.getHeight();

            float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
            if (ratio < 1.0f) {
                width = (int) (width * ratio);
                height = (int) (height * ratio);
            }

            return Bitmap.createScaledBitmap(src, width, height, true);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid bitmap scale parameters");
            return null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Bitmap compression OOM: " + e.getMessage());
            return null;
        }
    }

    /**
     * 取消所有通知
     */
    public void cancelAllNotifications() {
        if (notificationManager != null) {
            try {
                notificationManager.cancelAll();
            } catch (Exception e) {
                Log.e(TAG, "Cancel all notifications failed: " + e.getMessage());
            }
        }
    }

    /**
     * 取消指定 ID 的通知
     *
     * @param notificationId 通知 ID
     */
    public void cancelNotification(int notificationId) {
        if (notificationManager != null) {
            try {
                notificationManager.cancel(notificationId);
            } catch (Exception e) {
                Log.e(TAG, "Cancel notification failed: " + e.getMessage());
            }
        }
    }

    /**
     * 按通知渠道取消通知
     *
     * @param channelId 通知渠道 ID
     */
    public void cancelNotificationsByChannel(@NonNull String channelId) {
        if (notificationManager == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        try {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification n : notifications) {
                if (channelId.equals(n.getNotification().getChannelId())) {
                    notificationManager.cancel(n.getId());
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "No permission to get active notifications");
        } catch (Exception e) {
            Log.e(TAG, "Error canceling notifications by channel: " + e.getMessage());
        }
    }
}