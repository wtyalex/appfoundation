package com.wty.foundation.common.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.wty.foundation.common.init.AppContext;

import java.util.Locale;

/**
 * 多语言工具类，支持动态切换应用语言，并持久化存储语言设置。
 */
public class LanguageUtils {

    private static final String SP_NAME = "LanguageSettings";
    private static final String KEY_LANGUAGE = "key_language";
    private static final String KEY_COUNTRY = "key_country";

    private static volatile LanguageUtils INSTANCE;
    private final SharedPreferences preferences;

    private LanguageUtils(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取 LanguageUtils 实例（单例模式）
     *
     * @return LanguageUtils 实例
     */
    public static LanguageUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (LanguageUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LanguageUtils(AppContext.getInstance().getContext());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 设置应用语言，需要传递Activity上下文以确保UI更新
     *
     * @param context 上下文，应为Activity上下文
     * @param locale  语言区域
     */
    public void setAppLanguage(@NonNull Context context, @NonNull Locale locale) {
        if (context == null || locale == null) {
            return;
        }

        // 保存语言设置
        saveLanguageSetting(locale);

        // 更新应用语言
        updateAppLanguage(context, locale);

        // 如果是Activity上下文，重启Activity以立即刷新UI
        if (context instanceof Activity) {
            refreshActivity((Activity) context);
        }
    }

    /**
     * 获取当前应用的语言设置
     *
     * @return 当前语言区域
     */
    public Locale getAppLanguage() {
        String language = preferences.getString(KEY_LANGUAGE, "");
        String country = preferences.getString(KEY_COUNTRY, "");

        if (!TextUtils.isEmpty(language) && !TextUtils.isEmpty(country)) {
            return new Locale(language, country);
        } else {
            return Locale.getDefault(); // 返回系统默认语言
        }
    }

    /**
     * 重置应用语言为系统默认语言，需要传递Activity上下文以确保UI更新
     *
     * @param context 上下文，应为Activity上下文
     */
    public void resetAppLanguage(@NonNull Context context) {
        // 清除保存的语言设置
        preferences.edit().remove(KEY_LANGUAGE).remove(KEY_COUNTRY).apply();

        // 更新应用语言为系统默认语言
        updateAppLanguage(context, Locale.getDefault());

        // 如果是Activity上下文，重启Activity以立即刷新UI
        if (context instanceof Activity) {
            refreshActivity((Activity) context);
        }
    }

    /**
     * 保存语言设置到 SharedPreferences
     *
     * @param locale 语言区域
     */
    private void saveLanguageSetting(@NonNull Locale locale) {
        preferences.edit().putString(KEY_LANGUAGE, locale.getLanguage()).putString(KEY_COUNTRY, locale.getCountry()).apply();
    }

    /**
     * 更新应用语言
     *
     * @param context 上下文，对于需要立即更新UI的情况，应为Activity上下文
     * @param locale  语言区域
     */
    @SuppressLint("NewApi")
    private void updateAppLanguage(@NonNull Context context, @NonNull Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            configuration.setLocales(localeList);
        } else {
            // 兼容旧版本
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, displayMetrics);
    }

    /**
     * 刷新Activity以确保语言更改生效
     *
     * @param activity 需要刷新的Activity
     *                 注意：对于API < HONEYCOMB的系统，开发者需要自行处理Activity的刷新逻辑，因为finish()和重新启动可能会导致状态丢失或意外行为。
     */
    private void refreshActivity(Activity activity) {
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                activity.recreate();
            } else {
                // 对于API＜HONEYCOMB，开发者需要手动处理刷新
                activity.finish();
                activity.startActivity(activity.getIntent());
            }
        }
    }
}