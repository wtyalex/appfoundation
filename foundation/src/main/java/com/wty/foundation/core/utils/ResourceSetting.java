package com.wty.foundation.core.utils;

import com.wty.foundation.common.utils.ScreenUtils;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/**
 * @author wutianyu
 * @createTime 2023/6/7 14:55
 * @describe
 */
public class ResourceSetting {
    private ResourceSetting() {}

    public static void resourceSetting(Resources res) {
        if (res != null) {
            Configuration config = res.getConfiguration();
            if (config != null) {
                config.fontScale = 1f;
                DisplayMetrics dm = res.getDisplayMetrics();
                if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (ScreenUtils.calcScreenSize(dm) > 7.5) {
                        config.densityDpi = dm.widthPixels * 160 / 1280;
                    } else if (config.smallestScreenWidthDp < 375) {
                        config.densityDpi = dm.heightPixels * 160 / 375;
                    }
                } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    if (config.screenWidthDp < 375) {
                        config.densityDpi = dm.widthPixels * 160 / 375;
                    } else if (config.screenWidthDp > 640) {
                        config.densityDpi = dm.widthPixels * 160 / 640;
                    }
                }
            }
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }
}
