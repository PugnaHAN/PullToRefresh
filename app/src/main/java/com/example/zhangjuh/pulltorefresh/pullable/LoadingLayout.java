package com.example.zhangjuh.pulltorefresh.pullable;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.zhangjuh.pulltorefresh.R;

import java.lang.ref.WeakReference;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public class LoadingLayout extends FrameLayout {
    private static final String TAG = LoadingLayout.class.getSimpleName();

    private static final Interpolator DEFAULT_ANIMATION_INTERPOLATOR =
            new LinearInterpolator();
    private static final int PLAY_ANIMATION = 0;
    private static final int STOP_ANIMATION = 1;
    private static final int PAUSE_ANIMATION  = 2;
    private static final int REPEAT_ANIMATION = 3;

    private TextView mRefreshingNote;
    private ImageView mRefreshingIcon;
    private ProgressBar mProgressBar;
    private Animator mAnimator;

    private FrameLayout mInnerLayout;
    private AnimatorHandler mAnimatorHandler = new AnimatorHandler(this);

    private int mMode;

    public LoadingLayout(Context context){
        super(context);
        initLoadingLayout(context, null);
    }

    public LoadingLayout(Context context, AttributeSet attrs){
        super(context, attrs);
        initLoadingLayout(context, attrs);
    }

    public LoadingLayout(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initLoadingLayout(context, attrs);
    }

    public void setRefreshingIcon(Drawable drawable) {
        mRefreshingIcon.setImageDrawable(drawable);
    }

    public void setRefreshingIcon(int resourceId) {
        Drawable drawable = getContext().getResources().getDrawable(resourceId);
        setRefreshingIcon(drawable);
    }

    public void setRefreshingNote(String note) {
        mRefreshingNote.setText(note);
    }

    public void setAnimator(Animator animator) {
        mAnimator = animator;
    }

    public void startAnimation() {
        setAnimationStatus(PLAY_ANIMATION);
    }

    public void endAnimation(){
        setAnimationStatus(STOP_ANIMATION);
    }

    public void pauseAnimation(){
        setAnimationStatus(PAUSE_ANIMATION);
    }

    public void repeatAnimation(){
        setAnimationStatus(REPEAT_ANIMATION);
    }

    private void setAnimationStatus(int status) {
        Message message = mAnimatorHandler.obtainMessage();
        message.obj = status;
        mAnimatorHandler.sendMessage(message);
    }

    public void show(String views) {
        if(views.contains("all")) {
            views = "text, icon, bar";
        }

        if(views.contains("text")) {
            mRefreshingNote.setVisibility(VISIBLE);
        } else {
            mRefreshingNote.setVisibility(GONE);
        }

        if(views.contains("icon")) {
            mRefreshingIcon.setVisibility(VISIBLE);
        } else {
            mRefreshingIcon.setVisibility(GONE);
        }

        if(views.contains("bar")) {
            mProgressBar.setVisibility(VISIBLE);
        } else {
            mProgressBar.setVisibility(GONE);
        }
    }

    public void show() {
        show("all");
    }

    public void hide(String views){
        if(views.contains("all")) {
            views = "text, icon, bar";
        }

        if(views.contains("text")) {
            mRefreshingNote.setVisibility(GONE);
        } else {
            mRefreshingNote.setVisibility(VISIBLE);
        }

        if(views.contains("icon")) {
            mRefreshingIcon.setVisibility(GONE);
        } else {
            mRefreshingIcon.setVisibility(VISIBLE);
        }

        if(views.contains("bar")) {
            mProgressBar.setVisibility(GONE);
        } else {
            mProgressBar.setVisibility(VISIBLE);
        }
    }

    public void hide() {
        hide("all");
    }

    private void initLoadingLayout(Context context, AttributeSet attrs){
        LayoutInflater.from(context).inflate(R.layout.loadinglayout ,this);
        mInnerLayout = (FrameLayout) findViewById(R.id.loadingLayout);

        TypedArray types = context.obtainStyledAttributes(attrs, R.styleable.LoadingLayout);
        if(types.hasValue(R.styleable.LoadingLayout_pullMode)){
            mMode = types.getInt(R.styleable.LoadingLayout_pullMode, 0);
        }

        mRefreshingIcon = (ImageView) mInnerLayout.findViewById(R.id.loadingIcon);
        mRefreshingNote = (TextView) mInnerLayout.findViewById(R.id.loadingNote);
        mProgressBar = (ProgressBar) mInnerLayout.findViewById(R.id.loadingBar);
        mAnimator = ObjectAnimator.ofFloat(mRefreshingIcon, "rotation", 0f, 360f);
        mAnimator.setDuration(1000);

        switch (mMode) {
            // pullFromStart
            case 0:
                show("icon");
                break;
            // pullFromEnd
            case 1:
                show("text");
                mRefreshingNote.setText(R.string.loading);
                break;
            default:
                hide();
                break;
        }

        types.recycle();
    }

    public final void setHeight(int height){
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = height;
        requestLayout();
    }



    private static class AnimatorHandler extends Handler {
        private WeakReference<LoadingLayout> mLoadingLayoutWeakReference;

        public AnimatorHandler(LoadingLayout loadingLayout){
            mLoadingLayoutWeakReference = new WeakReference<>(loadingLayout);
        }

        @Override
        public void handleMessage(Message msg){
            LoadingLayout loadingLayout = mLoadingLayoutWeakReference.get();
            switch (Integer.decode(msg.obj.toString())){
                case PLAY_ANIMATION:
                    loadingLayout.mAnimator.start();
                    break;
                case STOP_ANIMATION:
                    loadingLayout.mAnimator.end();
                    break;
                case PAUSE_ANIMATION:
                    loadingLayout.mAnimator.pause();
                    break;
                case REPEAT_ANIMATION:
                    // loadingLayout.mRefreshingIcon.setImageResource(R.drawable.noarrow);
                    if(loadingLayout.mAnimator instanceof ValueAnimator){
                        ((ValueAnimator)loadingLayout.mAnimator).setRepeatCount(
                                ValueAnimator.INFINITE);
                        loadingLayout.mAnimator.start();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
