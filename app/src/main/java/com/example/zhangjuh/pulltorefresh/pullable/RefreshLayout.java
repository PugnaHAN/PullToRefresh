package com.example.zhangjuh.pulltorefresh.pullable;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.zhangjuh.pulltorefresh.R;

import java.lang.ref.WeakReference;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public class RefreshLayout extends LinearLayout {
    private static final String TAG = RefreshLayout.class.getSimpleName();

    private boolean DEBUG = true;

    // Header
    private LoadingLayout mHeaderView;
    // Content
    private FrameLayout mContentViewWrapper;
    private View mContentView;
    // Footer
    private LoadingLayout mFooterView;

    // Status
    private int mStatus;

    private boolean mHasHeader = true;
    private boolean mHasFooter = true;

    // Position
    private float mLastY;
    private float mCurrentY;
    private float mPullDist = 0;

    private boolean mIsBeginToPull = false;

    // Status of refreshing and loading
    public static final int IDLE = 0;
    public static final int REFRESHING = 1;
    public static final int LOADING = 2;
    public static final int RELEASE_TO_REFRESH = 3;
    public static final int RELEASE_TO_LOAD = 4;
    public static final int DONE = 5;

    // Mode
    public static final int MODE_DISABLED = 0;
    public static final int MODE_PULL_FROM_START = 1;
    public static final int MODE_PULL_FROM_END = 2;
    public static final int MODE_BOTH = 3;

    // Fraction
    private float mRatio = 2.0f;
    private int mArrowAnimationRepeat = 1;

    private static final int MAX_PULL_DIST = 300;

    /* Three basic constructor */
    public RefreshLayout(Context context){
        super(context);
        initView(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        initView(context, attrs);
    }

    // Override the addview
    @Override
    public void addView(View child, int index,  ViewGroup.LayoutParams params) {
        if (DEBUG) {
            Log.d(TAG, "addView: " + child.getClass().getSimpleName());
        }

        if(mContentView instanceof ViewGroup) {
            ((ViewGroup) mContentView).addView(child, index, params);
        } else {
            throw new UnsupportedOperationException("Content View is not a ViewGroup");
        }
    }

    /**
     * addView is overrided by adding view to content view, so if we want to layout itself,
     * we need to use this method
      */
    protected void addViewInternal(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
    }

    protected void addViewInternal(View child, ViewGroup.LayoutParams params) {
        super.addView(child, -1, params);
    }

    private void initView(Context context, AttributeSet attrs) {
        mHeaderView = new LoadingLayout(context);
        mFooterView = new LoadingLayout(context);
        mContentViewWrapper = new FrameLayout(context);
        mContentView = new PullableTextView(context);

        mContentViewWrapper.addView(mContentView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addViewInternal(mContentViewWrapper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TypedArray types = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
        if(types.hasValue(R.styleable.RefreshLayout_mode)){
            int mode = types.getInt(R.styleable.RefreshLayout_mode, 0);
            switch (mode) {
                case MODE_DISABLED:
                    mHasHeader = mHasFooter = false;
                    break;
                case MODE_PULL_FROM_START:
                    mHasHeader = true;
                    mHasFooter = false;
                    break;
                case MODE_PULL_FROM_END:
                    mHasHeader = false;
                    mHasFooter = true;
                    break;
                case MODE_BOTH:
                    mHasHeader = mHasFooter = true;
                    break;
                default:
                    break;
            }
        }
        types.recycle();
        updateUI(0, 0);
    }

    /**
     *  Use to hide header and footer
     */
    protected final void refreshLoadingLayoutSize(int top, int bottom){
        int maxPullDist = getMaxPullDist();

        int paddingTop = mHasHeader? -(Math.min(maxPullDist, maxPullDist - top)) : 0;
        mHeaderView.setHeight(maxPullDist);

        int paddingBottom = mHasFooter? -(Math.min(maxPullDist, maxPullDist - bottom)) : 0;
        mFooterView.setHeight(maxPullDist);
        if(DEBUG) {
            Log.d(TAG, "paddingTop is: " + paddingTop +
                    ", paddingEnd is: " + paddingBottom +
                    ", maxPullDist is: " + maxPullDist);
        }

        setPadding(0, paddingTop, 0, paddingBottom);
        requestLayout();
    }

    private int getMaxPullDist() {
        int distance;
        if(mHasHeader && mHasFooter) {
            distance = Math.max(mHeaderView.getHeight(), mFooterView.getHeight());
        } else {
            distance = mHasFooter? mFooterView.getHeight() :
                    (mHasHeader? mHeaderView.getHeight() : 0);
        }
        return Math.max(distance, MAX_PULL_DIST);
    }

    protected final void updateUI(int top, int bottom){
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // remove header and footer than add them if necessary
        if(this == mHeaderView.getParent()) {
            removeView(mHeaderView);
        }
        if(mHasHeader){
            addViewInternal(mHeaderView, 0, lp);
        }
        if(this == mFooterView.getParent()) {
            removeView(mFooterView);
        }
        if(mHasFooter){
            addViewInternal(mFooterView, lp);
        }
        refreshLoadingLayoutSize(top, bottom);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        int action = event.getActionMasked();
        int maxPullDist = getMaxPullDist();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                mCurrentY = event.getY();
                mPullDist = 0;
                mIsBeginToPull = true;
                mArrowAnimationRepeat = 1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // Filter multiple pointers
                mIsBeginToPull = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if(mIsBeginToPull) {
                    if(mContentView instanceof Pullable) {
                        if (mPullDist >= 0 || ((Pullable) mContentView).isPullDownEnabled()) {
                            mPullDist = mPullDist + (event.getY() - mLastY) / mRatio;
                            if (mPullDist < 0) {
                                mPullDist = 0;
                            }
                            if (mPullDist > getMeasuredHeight()) {
                                mPullDist = getMeasuredHeight();
                            }
                        }
                    }
                }
                mLastY = event.getY();
                mRatio = (float) (2 + 2 * Math.tan(Math.PI / 2 / getMeasuredHeight() * (Math.abs(mPullDist))));
                if(mPullDist > 0){
                    if(mPullDist >= maxPullDist && mArrowAnimationRepeat == 1) {
                        startAnimationOfPullDown();
                        mArrowAnimationRepeat = 0;
                    }
                    refreshLoadingLayoutSize((int) mPullDist, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsBeginToPull = false;
                mCurrentY = mLastY = event.getY();
                if(mPullDist >= maxPullDist) {
                    refreshLoadingLayoutSize(maxPullDist, 0);
                    startRefreshingAnimation();
                } else {
                    reset();
                }
                break;
            default:
                break;
        }
        if(DEBUG){
            Log.d(TAG, "dispatchEvent: mCurrentY = " + mCurrentY +
                    ", mPullDist = " + mPullDist + ", mLastY = " + mLastY +
                    ", mArrowAnimationRepeat = " + mArrowAnimationRepeat);
        }
        super.dispatchTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        return super.onInterceptTouchEvent(event);
    }

    private void startAnimationOfPullDown(){
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                mHeaderView.getRefreshingIcon(),
                "rotation", 0f, -180f);
        animator.setDuration(200);
        mHeaderView.setAnimator(animator);
        mHeaderView.startAnimation();
    }

    private void startRefreshingAnimation(){
        mHeaderView.setRefreshingIcon(R.drawable.noarrow);

        ObjectAnimator refresh = ObjectAnimator.ofFloat(
                mHeaderView.getRefreshingIcon(),
                "rotation", 0f, 360f);
        refresh.setRepeatCount(ValueAnimator.INFINITE);
        refresh.setDuration(500);

        mHeaderView.setAnimator(refresh);
        mHeaderView.repeatAnimation();

        AnimationHandler animationHandler = new AnimationHandler(this);
        Message message = animationHandler.obtainMessage();
        animationHandler.sendMessageDelayed(message, 3000);
    }
    
    private void resetLoadingLayout() {
        mHeaderView.setMode(LoadingLayout.MODE_PULL_FROM_START);
        mHeaderView.reset();
        mFooterView.setMode(LoadingLayout.MODE_PULL_FROM_END);
        mFooterView.reset();
        refreshLoadingLayoutSize(0, 0);
    }

    private void resetRefreshingView() {
        // TODO
    }

    private void resetValue() {
        mArrowAnimationRepeat = 1;
        mRatio = 2.0f;
        mIsBeginToPull = false;
        mStatus = IDLE;
    }

    public void reset() {
        resetLoadingLayout();
        resetRefreshingView();
        resetValue();
    }
    
    private static class AnimationHandler extends Handler {
        private WeakReference<RefreshLayout> mRefreshLayoutWR;
        
        public AnimationHandler(RefreshLayout refreshLayout){
            mRefreshLayoutWR = new WeakReference<>(refreshLayout);
        }
        
        @Override
        public void handleMessage(Message msg){
            mRefreshLayoutWR.get().reset();
        }
    }
}
