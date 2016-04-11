package com.example.zhangjuh.pulltorefresh.pullable;

import android.view.View;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public interface Pullable <T extends View> {
    boolean isPullUpEnabled();
    boolean isPullDownEnabled();
}
