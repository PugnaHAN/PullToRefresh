package com.example.zhangjuh.pulltorefresh.pullable;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.example.zhangjuh.pulltorefresh.R;

/**
 * Created by zhangjuh on 2016/4/11.
 */
public class FooterLoadingLayout extends LoadingLayout{
    private static final String TAG = FooterLoadingLayout.class.getSimpleName();

    public FooterLoadingLayout(Context context) {
        super(context);
        super.setMode(MODE_PULL_FROM_END);
    }

    public FooterLoadingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setMode(MODE_PULL_FROM_END);
    }

    public FooterLoadingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setMode(MODE_PULL_FROM_END);
    }

    public static FooterLoadingLayout create(Context context) {
        return new FooterLoadingLayout(context);
    }

    public static FooterLoadingLayout create(Context context, AttributeSet attrs) {
        return new FooterLoadingLayout(context, attrs);
    }

    public static FooterLoadingLayout create(Context context,
                                                   AttributeSet attrs, int defStyle) {
        return new FooterLoadingLayout(context, attrs, defStyle);
    }

    @Override
    public final void setMode(int mode){
        super.setMode(MODE_PULL_FROM_END);
    }

    @Override
    public final int getMode(){
        return MODE_PULL_FROM_END;
    }

    @Override
    public void reset() {
        show("text");
        mRefreshingIcon.setImageResource(R.drawable.noarrow);
        mAnimator = ObjectAnimator.ofFloat(mRefreshingIcon, "rotation",
                0f, 360f);
        ((ObjectAnimator) mAnimator).setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setDuration(500);
        mRefreshingNote.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show("icon");
                mAnimator.start();
            }
        });
    }
}
