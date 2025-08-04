package com.wty.foundation.common.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.Log;

import com.wty.foundation.common.init.AppContext;

import java.io.IOException;

/**
 * @author 吴天宇
 * @date 2025/8/4 10:45
 * Description: 音频播放工具类
 */
public class AudioPlayerUtils {
    private static final String TAG = "AudioPlayerUtils";
    private MediaPlayer mediaPlayer;       // 媒体播放器实例
    private AudioManager audioManager;     // 音频管理器，用于处理音频焦点
    private final Context context;         // 上下文对象，用于获取系统服务
    private boolean isPrepared = false;    // 标记媒体播放器是否已准备就绪

    /**
     * 构造方法
     */
    public AudioPlayerUtils() {
        this.context = AppContext.getInstance().getContext();
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 加载本地Raw资源中的音频文件
     * 注意：会先释放之前的播放器资源
     *
     * @param resId Raw资源中的音频文件ID，如R.raw.audio_file
     */
    public void loadAudioResource(int resId) {
        releasePlayer();
        try {
            // 从资源ID创建MediaPlayer，此方法会自动调用prepare()
            mediaPlayer = MediaPlayer.create(context, resId);
            isPrepared = true;
            // 设置音频焦点和属性
            setupAudioFocus();
        } catch (Exception e) {
            Log.e(TAG, "加载音频资源失败", e);
        }
    }

    /**
     * 加载网络音频文件
     * 注意：会先释放之前的播放器资源，采用异步准备方式
     *
     * @param url 音频文件的网络URL地址
     */
    public void loadAudioUrl(String url) {
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        try {
            // 设置音频数据源
            mediaPlayer.setDataSource(url);
            // 异步准备，避免阻塞主线程
            mediaPlayer.prepareAsync();
            // 设置准备完成监听器
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                // 准备完成后设置音频焦点和属性
                setupAudioFocus();
            });
        } catch (IOException e) {
            Log.e(TAG, "加载网络音频失败", e);
        }
    }

    /**
     * 设置音频焦点和音频属性
     * 处理不同Android版本的兼容性，请求音频焦点以确保正确的音频播放行为
     */
    private void setupAudioFocus() {
        if (audioManager == null) return;

        // 设置音频属性（Android 5.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)         // 用途为媒体播放
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)  // 内容类型为音乐
                    .build();
            mediaPlayer.setAudioAttributes(attributes);
        } else {
            // 低版本使用音频流类型
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        // 请求音频焦点
        int result = audioManager.requestAudioFocus(focusChange -> {
                    // 音频焦点变化监听器
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // 永久失去焦点，暂停播放
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // 暂时失去焦点，暂停播放
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // 获得焦点，开始播放
                            play();
                            break;
                    }
                }, AudioManager.STREAM_MUSIC,  // 音频流类型为音乐
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK  // 暂时获得焦点，允许其他音频降低音量播放
        );

        // 如果获得音频焦点，则开始播放
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mediaPlayer.start();
        }
    }

    /**
     * 播放音频
     * 只有在播放器已准备就绪且未在播放状态时才会执行播放操作
     */
    public void play() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    /**
     * 暂停音频播放
     * 只有在播放器正在播放时才会执行暂停操作
     */
    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * 停止音频播放
     * 停止后需要重新准备才能再次播放
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPrepared = false;
        }
    }

    /**
     * 跳转到音频的指定位置
     *
     * @param position 要跳转的位置，单位为毫秒
     */
    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
        }
    }

    /**
     * 获取当前播放位置
     *
     * @return 当前播放位置，单位为毫秒；如果播放器未准备就绪则返回0
     */
    public int getCurrentPosition() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getCurrentPosition() : 0;
    }

    /**
     * 获取音频总时长
     *
     * @return 音频总时长，单位为毫秒；如果播放器未准备就绪则返回0
     */
    public int getDuration() {
        return (mediaPlayer != null && isPrepared) ? mediaPlayer.getDuration() : 0;
    }

    /**
     * 判断音频是否正在播放
     *
     * @return 如果正在播放则返回true，否则返回false
     */
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * 释放播放器资源
     * 调用此方法后，播放器将被销毁，需要重新加载音频才能再次播放
     * 同时释放音频焦点
     */
    public void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
        if (audioManager != null) {
            // 放弃音频焦点
            audioManager.abandonAudioFocus(null);
        }
    }

    /**
     * 设置音频播放音量
     *
     * @param volume 音量值，范围为0.0f（静音）到1.0f（最大音量）
     */
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    /**
     * 设置音频播放完成监听器
     *
     * @param listener 播放完成时的回调监听器
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }
}