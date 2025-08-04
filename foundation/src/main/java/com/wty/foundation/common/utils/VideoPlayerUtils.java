package com.wty.foundation.common.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import com.wty.foundation.common.init.AppContext;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author 吴天宇
 * @date 2025/8/4 10:45
 * Description: 视频播放器工具类
 */
public class VideoPlayerUtils {
    private static final String TAG = "VideoPlayerUtils";
    private MediaPlayer mediaPlayer;       // 媒体播放器实例
    private final Context context;         // 上下文对象，使用应用上下文避免内存泄漏
    private Surface currentSurface;        // 当前关联的显示表面
    private boolean isPrepared = false;    // 播放器是否准备就绪
    private int savedPosition = 0;         // 保存的播放位置，用于恢复播放

    /**
     * 构造方法
     */
    public VideoPlayerUtils() {
        // 使用应用上下文，避免持有Activity引用导致内存泄漏
        this.context = AppContext.getInstance().getContext();
    }

    /**
     * 初始化播放器
     * 释放已有的播放器实例并创建新实例，设置各种监听事件
     */
    public void initializePlayer() {
        // 先释放已有的播放器
        releasePlayer();
        // 创建新的MediaPlayer实例
        mediaPlayer = new MediaPlayer();
        try {
            // 设置视频缩放模式为裁剪适应（保持比例并填满屏幕）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            }

            // 准备完成监听器
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                // 如果有保存的播放位置，恢复到该位置
                if (savedPosition > 0) {
                    mediaPlayer.seekTo(savedPosition);
                    savedPosition = 0;
                }
                // 如果已有关联的Surface，设置给播放器
                if (currentSurface != null) {
                    mediaPlayer.setSurface(currentSurface);
                }
                // 开始播放
                mediaPlayer.start();
            });

            // 错误监听器
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "播放错误: " + what + " | " + extra);
                return true; // 已处理错误，不向上传递
            });

            // 播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                if (mediaPlayer != null) {
                    // 播放完成后回到起始位置并暂停
                    mediaPlayer.seekTo(0);
                    mediaPlayer.pause();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "播放器初始化失败", e);
        }
    }

    /**
     * 设置播放数据源
     *
     * @param uri 视频资源的Uri，可以是本地文件或网络地址
     */
    public void setDataSource(Uri uri) {
        // 检查播放器实例和Uri是否有效
        if (mediaPlayer == null || uri == null) return;

        try {
            // 重置播放器状态
            mediaPlayer.reset();
            // 根据系统版本设置数据源
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mediaPlayer.setDataSource(context, uri, new HashMap<>());
            } else {
                mediaPlayer.setDataSource(context, uri);
            }
            // 异步准备播放（不会阻塞主线程）
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "设置数据源失败", e);
        }
    }

    /**
     * 设置视频显示的Surface
     *
     * @param surface 用于显示视频的Surface对象
     */
    public void setSurface(Surface surface) {
        this.currentSurface = surface;
        // 如果播放器已准备就绪，立即设置Surface
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.setSurface(surface);
        }
    }

    /**
     * 开始或恢复播放
     */
    public void play() {
        // 检查播放器状态，只有准备就绪且未在播放时才执行
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        // 只有在播放状态时才暂停
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * 停止播放
     * 停止后需要重新准备才能再次播放
     */
    public void stop() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.stop();
            isPrepared = false; // 停止后状态变为未准备
        }
    }

    /**
     * 跳转到指定位置播放
     *
     * @param position 要跳转的位置，单位：毫秒
     */
    public void seekTo(int position) {
        if (mediaPlayer != null) {
            if (isPrepared) {
                // 已准备就绪，直接跳转
                mediaPlayer.seekTo(position);
            } else {
                // 未准备就绪，先保存位置，准备完成后跳转
                savedPosition = position;
            }
        }
    }

    /**
     * 获取当前播放位置
     *
     * @return 当前播放位置，单位：毫秒；未准备就绪时返回0
     */
    public int getCurrentPosition() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getCurrentPosition() : 0;
    }

    /**
     * 获取视频总时长
     *
     * @return 视频总时长，单位：毫秒；未准备就绪时返回0
     */
    public int getDuration() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getDuration() : 0;
    }

    /**
     * 判断是否正在播放
     *
     * @return 是否正在播放
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * 释放播放器资源
     * 当不再需要播放器时调用，避免资源泄露
     */
    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
        currentSurface = null;
    }

    /**
     * 设置播放音量
     *
     * @param volume 音量值，范围0.0f（静音）到1.0f（最大音量）
     */
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            // 左右声道同时设置相同音量
            mediaPlayer.setVolume(volume, volume);
        }
    }

    /**
     * 设置是否循环播放
     *
     * @param looping true表示循环播放，false表示不循环
     */
    public void setLooping(boolean looping) {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(looping);
        }
    }

    /**
     * 设置是否静音
     *
     * @param mute true表示静音，false表示取消静音
     */
    public void setMute(boolean mute) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(mute ? 0f : 1f, mute ? 0f : 1f);
        }
    }
}