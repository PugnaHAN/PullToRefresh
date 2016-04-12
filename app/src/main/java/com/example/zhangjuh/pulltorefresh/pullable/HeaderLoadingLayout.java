package com.example.zhangjuh.pulltorefresh.pullable;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;

import com.example.zhangjuh.pulltorefresh.R;

/**
 * Created by zhangjuh on 2016/4/11.
 */
public class HeaderLoadingLayout extends LoadingLayout {
    private static final String TAG = HeaderLoadingLayout.class.getSimpleName();

    public HeaderLoadingLayout(Context context) {
        super(context);
        super.setMode(MODE_PULL_FROM_START);
    }

    public HeaderLoadingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setMode(MODE_PULL_FROM_START);
    }

    public HeaderLoadingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setMode(MODE_PULL_FROM_START);
    }

    public static HeaderLoadingLayout create(Context context) {
        return new HeaderLoadingLayout(context);
    }

    public static HeaderLoadingLayout create(Context context, AttributeSet attrs) {
        return new HeaderLoadingLayout(context, attrs);
    }

    public static HeaderLoadingLayout create(Context context,
                                             AttributeSet attrs, int defStyle) {
        return new HeaderLoadingLayout(context, attrs, defStyle);
    }

    @Override
    public final void setMode(int mode){
        super.setMode(MODE_PULL_FROM_START);
    }

    @Override
    public final int getMode(){
        return MODE_PULL_FROM_END;
    }

    @Override
    public void reset() {
        mAnimator.end();
        // show and hide
        show("icon");
        mRefreshingIcon.setImageResource(R.drawable.xlistview_arrow);
        mAnimator = ObjectAnimator.ofFloat(getRefreshingIcon(), "rotation",
                0f, -180f);
        mAnimator.setDuration(100);
    }
}
