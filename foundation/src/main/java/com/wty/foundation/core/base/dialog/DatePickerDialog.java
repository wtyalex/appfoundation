package com.wty.foundation.core.base.dialog;

import java.util.Calendar;

import com.wty.foundation.common.utils.ScreenUtils;
import com.wty.foundation.common.utils.StringUtils;
import com.wty.foundation.common.utils.TimeSync;
import com.wty.foundation.common.utils.TimeUtils;
import com.wty.foundation.common.utils.ViewUtils;
import com.wty.foundation.core.adapter.DatePickerAdapter;
import com.wty.foundation.core.recycleview.GalleryLayoutManager;
import com.wty.foundation.databinding.UiDialogDatePickerBinding;

import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

/**
 * @author wutianyu
 * @createTime 2023/2/9 11:30
 * @describe 日期时间选择对话框，支持日期和时间两种模式选择
 */
public class DatePickerDialog extends BaseDialog<UiDialogDatePickerBinding> {
    public static final int DATE_MODE = 1;
    public static final int TIME_MODE = 2;
    private static final int ONE_DAY_MINUTES = 24 * 60 - 1;
    private static final String TAG = "DatePickerDialog";
    private final Calendar mCalendar = Calendar.getInstance();
    // 选择类型
    private int mMode;
    private int mMinuteInterval;
    private String mTitle;
    private long mMinTime;
    private long mMaxTime;
    private long mDefaultTime;
    // 最小时间
    private int mMinFirst;
    private int mMinSecond;
    private int mMinThird;
    // 最大时间
    private int mMaxFirst;
    private int mMaxSecond;
    private int mMaxThird;
    // 选中的时间
    private int mSelectedFirst;
    private int mSelectedSecond;
    private int mSelectedThird;
    // 展示选择列表:默认都显示
    private boolean[] mShowItemList = new boolean[] {true, true, true};

    private OnDateSelectedListener mOnDateSelectedListener;

    public DatePickerDialog() {
        mMode = TIME_MODE;
    }

    private DatePickerDialog(Builder builder) {
        mMinTime = builder.minTime;
        mDefaultTime = builder.defaultTime;
        mMaxTime = builder.maxTime;
        mShowItemList = builder.showItemList;
        mMinuteInterval = Math.max(1, Math.min(30, builder.minuteInterval));
        if (builder.mode == DATE_MODE) {
            mMode = DATE_MODE;
        } else {
            mMode = TIME_MODE;
        }
        mTitle = builder.title;
        mOnDateSelectedListener = builder.onDateSelectedListener;
        initData();
    }

    @Override
    protected void initView() {
        ViewGroup.LayoutParams lp = ViewUtils.getLayoutParams(getViewBinding().getRoot());
        if (lp != null) {
            if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                lp.width = ScreenUtils.getScreenSize()[0] * 2 / 5;
            } else {
                lp.width = ScreenUtils.getScreenSize()[0];
            }
        }
        getViewBinding().getRoot().setLayoutParams(lp);
        if (mMode == TIME_MODE) {
            initHalfDay();
            initHour();
            initMinute();
        } else if (mMode == DATE_MODE) {
            initYear();
            initMonth();
            initDay();
        }
        getViewBinding().title.setText(mTitle);
        getViewBinding().cancel.setOnClickListener(getOnClickListener());
        getViewBinding().ok.setOnClickListener(getOnClickListener());

        if (mShowItemList.length == 3) {
            getViewBinding().firstList.setVisibility(mShowItemList[0] ? View.VISIBLE : View.GONE);
            getViewBinding().secondList.setVisibility(mShowItemList[1] ? View.VISIBLE : View.GONE);
            getViewBinding().thirdList.setVisibility(mShowItemList[2] ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == getViewBinding().cancel) {
            dismiss();
        } else if (v == getViewBinding().ok) {
            if (mOnDateSelectedListener != null) {
                mOnDateSelectedListener.onSelected(mSelectedFirst, mSelectedSecond, mSelectedThird);
            }
            dismiss();
        }
    }

    private void initData() {
        if (mMode == DATE_MODE) {
            initDate();
        } else {
            initTime();
        }
    }

    private void initTime() {
        int minTime = Math.min(Math.max(0, (int)mMinTime), ONE_DAY_MINUTES);
        int maxime = Math.min(Math.max(0, (int)mMaxTime), ONE_DAY_MINUTES);
        if (minTime < maxime) {
            mMinSecond = minTime / 60;
            mMinThird = minTime % 60;
            mMaxSecond = maxime / 60;
            mMaxThird = maxime % 60;
            if (mMinThird % mMinuteInterval != 0) {
                mMinThird += mMinuteInterval - mMinThird % mMinuteInterval;
                if (mMinThird > 59) {
                    mMinThird = 0;
                    mMinSecond++;
                }
            }
            if (mMinSecond >= 24) {
                mMinSecond = 0;
            }
            if (mMaxSecond >= 24) {
                mMaxSecond = 23;
            }
        } else {
            mMinSecond = 0;
            mMinThird = 0;
            mMaxSecond = 23;
            mMaxThird = 59;
        }
        int defaultTime = Math.max(0, (int)mDefaultTime);
        mSelectedSecond = Math.max(mMinSecond, Math.min(defaultTime / 60, mMaxSecond));
        mSelectedThird = defaultTime % 60;
        if (mSelectedSecond == mMinSecond) {
            mSelectedThird = Math.max(mMinThird, mSelectedThird);
        }
        if (mSelectedSecond == mMaxSecond) {
            mSelectedThird = Math.min(mMaxThird, mSelectedThird);
        }
        if (getTimeSpan() == 3) {
            if (mSelectedSecond > 12) {
                mSelectedFirst = 1;
            } else {
                mSelectedFirst = 0;
            }
        }
    }

    private void initDate() {
        mCalendar.setTimeInMillis(mMinTime);
        mMinFirst = mCalendar.get(Calendar.YEAR);
        mMinSecond = mCalendar.get(Calendar.MONTH) + 1;
        mMinThird = mCalendar.get(Calendar.DAY_OF_MONTH);
        mCalendar.setTimeInMillis(mMaxTime);
        mMaxFirst = Math.max(mMinFirst, mCalendar.get(Calendar.YEAR));
        if (mMaxFirst == mMinFirst) {
            mMaxSecond = Math.max(mMinSecond, mCalendar.get(Calendar.MONTH) + 1);
            if (mMaxSecond == mMinSecond) {
                mMaxThird = Math.max(mMinThird, mCalendar.get(Calendar.DAY_OF_MONTH));
            } else {
                mMaxThird = mCalendar.get(Calendar.DAY_OF_MONTH);
            }
        } else {
            mMaxSecond = mCalendar.get(Calendar.MONTH) + 1;
            mMaxThird = mCalendar.get(Calendar.DAY_OF_MONTH);
        }
        mCalendar.setTimeInMillis(mDefaultTime);
        mSelectedFirst = Math.max(mMinFirst, Math.min(mMaxFirst, mCalendar.get(Calendar.YEAR)));
        if (mSelectedFirst == mMinFirst) {
            mSelectedSecond = Math.max(mMinSecond, mCalendar.get(Calendar.MONTH) + 1);
            if (mSelectedFirst == mMaxFirst) {
                mSelectedSecond = Math.min(mMaxSecond, mSelectedSecond);
                mSelectedThird = Math.max(mMinThird, Math.min(mMaxThird, mCalendar.get(Calendar.DAY_OF_MONTH)));
            } else {
                mSelectedThird = Math.max(mMinThird, mCalendar.get(Calendar.DAY_OF_MONTH));
            }
        } else if (mSelectedFirst < mMaxFirst) {
            mSelectedSecond = mCalendar.get(Calendar.MONTH) + 1;
            mSelectedThird = mCalendar.get(Calendar.DAY_OF_MONTH);
        }
    }

    private void initYear() {
        initList(getViewBinding().firstList, new DatePickerAdapter() {
            @Override
            protected String OnText(int position, String data) {
                return String.valueOf(mMinFirst + position);
            }

            @Override
            public int getItemCount() {
                return mMaxFirst - mMinFirst + 1;
            }
        }, (recyclerView, item, position) -> {
            if (position != mSelectedFirst) {
                if (mSelectedFirst != mMinFirst && mSelectedFirst != mMaxFirst) {
                    mSelectedFirst = position + mMinFirst;
                    if (mSelectedFirst == mMinFirst || mSelectedFirst == mMaxFirst) {
                        getViewBinding().secondList.getAdapter().notifyDataSetChanged();
                    }
                } else {
                    mSelectedFirst = position + mMinFirst;
                    getViewBinding().secondList.getAdapter().notifyDataSetChanged();
                }
                getViewBinding().thirdList.getAdapter().notifyDataSetChanged();
                updateTitle();
            }
        }, mSelectedFirst - mMinFirst);
    }

    private void initMonth() {
        initList(getViewBinding().secondList, new DatePickerAdapter() {
            @Override
            protected String OnText(int position, String data) {
                if (mSelectedFirst == mMinFirst) {
                    return String.valueOf(mMinSecond + position);
                } else {
                    return String.valueOf(position + 1);
                }
            }

            @Override
            public int getItemCount() {
                if (mSelectedFirst == mMinFirst) {
                    if (mSelectedFirst == mMaxFirst) {
                        return mMaxSecond - mMinSecond + 1;
                    } else {
                        return 13 - mMinSecond;
                    }
                } else if (mSelectedFirst == mMaxFirst) {
                    return mMaxSecond;
                } else {
                    return 12;
                }
            }
        }, (recyclerView, item, position) -> {
            int month;
            if (mSelectedFirst == mMinFirst) {
                month = position + mMinSecond;
            } else {
                month = position + 1;
            }
            if (month != mSelectedSecond) {
                mSelectedSecond = month;
                getViewBinding().thirdList.getAdapter().notifyDataSetChanged();
                updateTitle();
            }
        }, mSelectedFirst == mMinFirst ? mSelectedSecond - mMinSecond : mSelectedSecond - 1);
    }

    private void initDay() {
        initList(getViewBinding().thirdList, new DatePickerAdapter() {
            @Override
            protected String OnText(int position, String data) {
                if (mSelectedFirst == mMinFirst && mSelectedSecond == mMinSecond) {
                    return String.valueOf(position + mMinThird);
                } else {
                    return String.valueOf(position + 1);
                }
            }

            @Override
            public int getItemCount() {
                if (mSelectedFirst == mMinFirst) {
                    if (mSelectedSecond == mMinSecond) {
                        return getDayCountOfMonth(mSelectedFirst, mSelectedSecond) - mMinThird + 1;
                    } else {
                        return getDayCountOfMonth(mSelectedFirst, mSelectedSecond);
                    }
                } else if (mSelectedFirst == mMaxFirst) {
                    if (mSelectedSecond == mMaxSecond) {
                        return mMaxThird;
                    } else {
                        return getDayCountOfMonth(mSelectedFirst, mSelectedSecond);
                    }
                } else {
                    return getDayCountOfMonth(mSelectedFirst, mSelectedSecond);
                }
            }
        }, (recyclerView, item, position) -> {
            if (mSelectedFirst == mMinFirst && mSelectedSecond == mMinSecond) {
                mSelectedThird = position + mMinThird;
            } else {
                mSelectedThird = position + 1;
            }
            updateTitle();
        }, (mSelectedFirst == mMinFirst && mSelectedSecond == mMinSecond) ? mSelectedThird - mMinThird
            : mSelectedThird - 1);
    }

    private void initHalfDay() {
        initList(getViewBinding().firstList, new DatePickerAdapter() {
            @Override
            protected String OnText(int position, String data) {
                if (getTimeSpan() == 3) {
                    if (position == 0) {
                        return "上午";
                    } else {
                        return "下午";
                    }
                } else if (getTimeSpan() == 2) {
                    return "下午";
                } else {
                    return "上午";
                }
            }

            @Override
            public int getItemCount() {
                return getTimeSpan() == 3 ? 2 : 1;
            }
        }, (recyclerView, item, position) -> {
            if (position != mSelectedFirst) {
                mSelectedFirst = position;
                if (position == 0) {
                    if (mSelectedSecond > 12) {
                        getViewBinding().secondList.smoothScrollToPosition(12 - mMinSecond);
                    }
                } else {
                    if (mSelectedSecond <= 12) {
                        getViewBinding().secondList.smoothScrollToPosition(13 - mMinSecond);
                    }
                }
            }
        }, getTimeSpan() == 3 ? (mSelectedSecond <= 12 ? 0 : 1) : 0);
    }

    private void initHour() {
        initList(getViewBinding().secondList, new DatePickerAdapter() {
            @Override
            protected String OnText(int position, String data) {
                return mMinSecond + position + "时";
            }

            @Override
            public int getItemCount() {
                return mMaxSecond - mMinSecond + 1;
            }
        }, (recyclerView, item, position) -> {
            if (position + mMinSecond != mSelectedSecond) {
                mSelectedSecond = position + mMinSecond;
                if (getTimeSpan() == 3) {
                    if (mSelectedSecond > 12) {
                        getViewBinding().firstList.smoothScrollToPosition(1);
                    } else {
                        getViewBinding().firstList.smoothScrollToPosition(0);
                    }
                }
                getViewBinding().thirdList.getAdapter().notifyDataSetChanged();
                updateTitle();
            }
        }, mSelectedSecond - mMinSecond);
    }

    private void initMinute() {
        initList(getViewBinding().thirdList, new DatePickerAdapter() {
            @Override
            protected String OnText(int position, String data) {
                if (mSelectedSecond == mMinSecond) {
                    return mMinThird + position * mMinuteInterval + "分";
                } else {
                    return position * mMinuteInterval + "分";
                }
            }

            @Override
            public int getItemCount() {
                if (mSelectedSecond == mMinSecond) {
                    if (mSelectedSecond != mMaxSecond) {
                        return (60 - mMinThird) / mMinuteInterval;
                    } else {
                        return (mMaxThird - mMinThird + 1) / mMinuteInterval;
                    }
                } else if (mSelectedSecond < mMaxSecond) {
                    return 60 / mMinuteInterval;
                } else {
                    return (mMaxThird + 1) / mMinuteInterval;
                }
            }
        }, (recyclerView, item, position) -> {
            if (mSelectedSecond == mMinSecond) {
                mSelectedThird = mMinThird + position * mMinuteInterval;
            } else {
                mSelectedThird = position * mMinuteInterval;
            }
            updateTitle();
        }, mSelectedSecond == mMinSecond ? (mSelectedThird - mMinThird) / mMinuteInterval
            : mSelectedThird / mMinuteInterval);
    }

    protected void updateTitle() {
        if (!StringUtils.isNull(mTitle)) {
            return;
        }
        if (mMode == DATE_MODE) {
            getViewBinding().title.setText(TimeUtils.time2Str(
                TimeUtils.str2Millis(mSelectedFirst + "-" + mSelectedSecond + "-" + mSelectedThird, "yyyy-MM-dd"),
                "yyyy年MM月dd日 E"));
        } else {
            if (mSelectedSecond > 12) {
                getViewBinding().title.setText("下午 " + (mSelectedSecond < 10 ? "0" : "") + mSelectedSecond + "时"
                    + (mSelectedThird < 10 ? "0" : "") + mSelectedThird + "分");
            } else {
                getViewBinding().title.setText("上午 " + (mSelectedSecond < 10 ? "0" : "") + mSelectedSecond + "时"
                    + (mSelectedThird < 10 ? "0" : "") + mSelectedThird + "分");
            }
        }
    }

    private int getTimeSpan() {
        if (mMinSecond > 12) {
            return 2;
        } else if (mMaxSecond <= 12) {
            return 1;
        } else {
            return 3;
        }
    }

    private int getDayCountOfMonth(int year, int month) {
        if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12) {
            return 31;
        } else if (month == 2) {
            if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
                if (year % 3200 == 0 && year % 172800 != 0) {
                    return 28;
                } else {
                    return 29;
                }
            }
            return 28;
        } else {
            return 30;
        }
    }

    private void initList(RecyclerView recyclerView, DatePickerAdapter adapter,
        GalleryLayoutManager.OnItemSelectedListener onItemSelectedListener, int selectedPosition) {
        GalleryLayoutManager layoutManager = new GalleryLayoutManager(GalleryLayoutManager.VERTICAL);
        layoutManager.setCallbackInFling(false);
        layoutManager.setOnItemSelectedListener(onItemSelectedListener);
        layoutManager.setItemTransformer((galleryLayoutManager, view, v) -> {
            float scale = 1 - 0.35F * Math.abs(v);
            float alpha = 1 - 0.6F * Math.abs(v);
            view.setScaleX(scale);
            view.setScaleY(scale);
            if (alpha < 0) {
                alpha = 0;
            }
            view.setAlpha(alpha);
        });
        layoutManager.attach(recyclerView, selectedPosition);
        recyclerView.setAdapter(adapter);
    }

    public interface OnDateSelectedListener {
        void onSelected(int first, int second, int third);
    }

    public static class Builder {
        private int mode;
        private int minuteInterval;
        private long minTime;
        private long maxTime;
        private long defaultTime;
        private String title;
        private OnDateSelectedListener onDateSelectedListener;
        private boolean[] showItemList = new boolean[] {true, true, true};

        public Builder(int mode) {
            this.mode = mode;
            if (mode == DATE_MODE) {
                minTime = 0;
                maxTime = Long.MAX_VALUE;
                defaultTime = TimeSync.getTime();
            } else {
                minTime = 0;
                maxTime = ONE_DAY_MINUTES;
            }
        }

        /**
         * 设置三列分别是否显示
         * 
         * @param showFirst
         * @param showSecond
         * @param showThird
         * @return
         */
        public Builder setItemVisible(boolean showFirst, boolean showSecond, boolean showThird) {
            this.showItemList[0] = showFirst;
            this.showItemList[1] = showSecond;
            this.showItemList[2] = showThird;
            return this;
        }

        public Builder setMin(long minTime) {
            this.minTime = minTime;
            return this;
        }

        public Builder setMax(long maxTime) {
            this.maxTime = maxTime;
            return this;
        }

        public Builder setMinuteInterval(int minuteInterval) {
            this.minuteInterval = minuteInterval;
            return this;
        }

        public Builder setDefaultTime(long defaultTime) {
            this.defaultTime = defaultTime;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setOnDateSelectedListener(OnDateSelectedListener onDateSelectedListener) {
            this.onDateSelectedListener = onDateSelectedListener;
            return this;
        }

        public DatePickerDialog build() {
            return new DatePickerDialog(this);
        }
    }
}
