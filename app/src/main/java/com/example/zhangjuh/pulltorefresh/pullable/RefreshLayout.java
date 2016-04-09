package com.example.zhangjuh.pulltorefresh.pullable;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.zhangjuh.pulltorefresh.R;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public class RefreshLayout extends LinearLayout {
    private static final String TAG = RefreshLayout.class.getSimpleName();

    private boolean DEBUG = true;

    // Header
    private View mHeaderView;
    // Content
    private FrameLayout mContentViewWrapper;
    private View mContentView;
    // Footer
    private View mFooterView;

    // Status
    private int mStatus;

    private boolean mHasHeader = true;
    private boolean mHasFooter = true;

    // Position
    private float mLastY;
    private float mCurrentY;
    private float mPullDist = 0;

    private boolean mIsBeginToPull = false;
    private boolean mCanPullDown = false;
    private boolean mCanPullUp = false;


    // Status of refreshing and loading
    public static final int IDLE = 0;
    public static final int REFRESHING = 1;
    public static final int LOADING = 2;
    public static final int RELEASE_TO_REFRESH = 3;
    public static final int RELEASE_TO_LOAD = 4;
    public static final int DONE = 5;

    // Fraction
    private float mRatio = 2.0f;

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
                // Disabled
                case 0:
                    mHasHeader = mHasFooter = false;
                    break;
                // PullFromStart
                case 1:
                    mHasHeader = true;
                    mHasFooter = false;
                    break;
                // PullFromEnd
                case 2:
                    mHasHeader = false;
                    mHasFooter = true;
                    break;
                // Both
                case 3:
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
        final int maxPullDist = MAX_PULL_DIST;

        int paddingTop = mHasHeader? -(Math.min(maxPullDist, MAX_PULL_DIST - top)) : 0;
        if(mHeaderView instanceof LoadingLayout && mHasHeader){
            ((LoadingLayout) mHeaderView).setHeight(maxPullDist);
        }

        int paddingBottom = mHasFooter ? -(Math.min(maxPullDist, MAX_PULL_DIST - bottom)) : 0;
        if(mFooterView instanceof LoadingLayout && mHasFooter){
            ((LoadingLayout) mFooterView).setHeight(maxPullDist);
        }
        if(DEBUG) {
            Log.d(TAG, "paddingTop is: " + paddingTop +
                    ", paddingEnd is: " + paddingBottom +
                    ", maxPullDist is: " + maxPullDist);
        }

        setPadding(0, paddingTop, 0, paddingBottom);
        requestLayout();
    }

    private int getMaxPullDist(){
        return Math.round(getWidth()/2);
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
        if(this == mFooterView) {
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
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                mCurrentY = event.getY();
                mPullDist = 0;
                mIsBeginToPull = true;
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
                if(mPullDist > 0){
                    if(mPullDist < MAX_PULL_DIST) {
                        refreshLoadingLayoutSize((int) (mPullDist), 0);
                    } else {
                        mPullDist = MAX_PULL_DIST;
                        refreshLoadingLayoutSize(MAX_PULL_DIST, 0);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mIsBeginToPull = false;
                mCurrentY = mLastY = event.getY();
                mPullDist = 0;
                refreshLoadingLayoutSize(0 , 0);
                if(mHeaderView instanceof LoadingLayout){
                    ((LoadingLayout) mHeaderView).setRefreshingIcon(R.drawable.noarrow);
                    ((LoadingLayout) mHeaderView).repeatAnimation();
                }
                break;
            default:
                break;
        }
        if(DEBUG){
            Log.d(TAG, "dispatchEvent: mCurrentY = " + mCurrentY +
                    ", mPullDist = " + mPullDist + ", mLastY = " + mLastY);
        }
        super.dispatchTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        return super.onInterceptTouchEvent(event);
    }
}
