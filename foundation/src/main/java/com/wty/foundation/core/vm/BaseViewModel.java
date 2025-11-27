package com.wty.foundation.core.vm;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.wty.foundation.core.safe.SafeMutableLiveData;

import android.app.Application;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateHandle;

/**
 * @author wutianyu
 * @createTime 2023/2/21 15:12
 * @describe 基础ViewModel类，提供加载对话框管理和数据仓库访问功能
 */
public abstract class BaseViewModel<Rep extends IRepository> extends AndroidViewModel {
    protected final Rep mRepository;
    private final SafeMutableLiveData<String> mLoadDialogState = new SafeMutableLiveData<>();
    private LinkedHashMap<String, String> mLoadingDialogMsgMap = new LinkedHashMap<>();

    public BaseViewModel(@NonNull Application application, SavedStateHandle savedStateHandle) {
        super(application);
        mRepository = getRepositoryImp();
    }

    /**
     * 监听 LoadDialog状态
     *
     * @param owner LifecycleOwner
     * @param observer null不展示或者关闭，其他展示
     */
    public void observerLoadDialogState(LifecycleOwner owner, Observer<String> observer) {
        mLoadDialogState.observe(owner, observer);
    }

    /**
     * 显示加载框框 后续必须调用{closeLoadingDialog(String)}或{closeAllLoadingDialog()}关闭
     * 
     * @param msg 加载框上显示的提示
     * @return dialog id用来关闭对应加载框的
     */
    protected final String showLoadingDialog(String msg) {
        String id = String.valueOf(SystemClock.elapsedRealtime());
        mLoadingDialogMsgMap.put(id, msg);
        updateLoadingDialogState();
        return id;
    }

    /**
     * 关闭加载框框
     * 
     * @param id 关闭对应id的对话框
     */
    protected final void closeLoadingDialog(String id) {
        mLoadingDialogMsgMap.remove(id);
        updateLoadingDialogState();
    }

    /**
     * 关闭所有加载框框
     *
     */
    protected final void closeAllLoadingDialog() {
        mLoadingDialogMsgMap.clear();
        updateLoadingDialogState();
    }

    private void updateLoadingDialogState() {
        Iterator<Map.Entry<String, String>> iterator = mLoadingDialogMsgMap.entrySet().iterator();
        if (iterator.hasNext()) {
            mLoadDialogState.setValue(iterator.next().getValue());
        } else {
            mLoadDialogState.setValue(null);
        }
    }

    /**
     * 创建Repository实例
     * 
     * @return IRepository实例
     */
    protected abstract Rep getRepositoryImp();
}