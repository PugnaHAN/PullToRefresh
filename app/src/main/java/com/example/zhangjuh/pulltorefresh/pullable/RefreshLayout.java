package com.example.zhangjuh.pulltorefresh.pullable;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.zhangjuh.pulltorefresh.R;

import java.lang.ref.WeakReference;
import java.util.Locale;

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
    private float mPullDist = 0;

    private boolean mIsBeginToPull = false;
    private boolean mIsRefreshing = false;
    private boolean mIsLoading = false;

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

    // Duration
    public static final int SMOOTH_SCROLL_DURATION_MS = 200;

    // Animation repeat count
    public static final int INIT_ARROW_ANIMATION_COUNT = 1;
    public static final int NO_ARROW_ANIMATION_COUNT = 0;

    // Fraction
    private float mRatio = 2.0f;
    private int mArrowAnimationRepeat = INIT_ARROW_ANIMATION_COUNT;

    private static final int MAX_PULL_DIST = 240;

    private SmoothScrollRunnable mCurrentScrollRunable;
    private Interpolator mScrollAnimationInterpolator;

    /* Three basic constructor */
    public RefreshLayout(Context context){
        super(context);
        setDefaultOrientation();
        initView(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultOrientation();
        initView(context, attrs);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        setDefaultOrientation();
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
        mHeaderView = HeaderLoadingLayout.create(context);
        mFooterView = FooterLoadingLayout.create(context);
        mContentViewWrapper = new FrameLayout(context);
        mContentView = new PullableTextView(context);
        // ((PullableImageView)mContentView).setImageResource(R.drawable.noarrow);

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
        updateUI();

        if(DEBUG) {
            StringBuilder children = new StringBuilder();
            for(int i = 0; i < getChildCount(); i++) {
                String str = String.format(Locale.getDefault(),"child[%d]: ", i)
                        + getChildAt(i).getClass().getSimpleName() + ", ";
                children.append(str);
            }
            Log.d(TAG, new String(children));
            /**
             * 一开始下面两个值为0，因为在初始状态两个view均处在隐藏状态，因此，measured的时候认为其值为0
             */
            Log.d(TAG, "mHeaderViewHeight = " + mHeaderView.getMeasuredHeight()
                    + ", mFootViewHeight = " + mFooterView.getMeasuredHeight());
        }
    }

    // Set the default orientation as vertical
    protected void setDefaultOrientation() {
        setOrientation(VERTICAL);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);

        if(DEBUG) {
            Log.d(TAG, "onSizeChanged: TotalMeasuredHeight = " + getMeasuredHeight() +
                    ", TotalMeasuredWidth = " + getMeasuredWidth() +
                    ", mContentViewWrap = " + mContentViewWrapper.getMeasuredHeight());
        }
    }

    /**
     *  Use to hide header and footer
     */
    protected final void refreshLoadingLayoutSize(){
        int maxPullDist = getMaxPullDist();

        int paddingTop = mHasHeader? -maxPullDist : 0;
        mHeaderView.setHeight(maxPullDist);

        int paddingBottom = mHasFooter? -maxPullDist : 0;
        mFooterView.setHeight(maxPullDist);
        if(DEBUG) {
            Log.d(TAG, "paddingTop is: " + paddingTop +
                    ", paddingEnd is: " + paddingBottom +
                    ", maxPullDist is: " + maxPullDist);
        }

        setPadding(0, paddingTop, 0, paddingBottom);
    }

    /**
     * This method is used to get the maximum pullable distance
     * @return maxPullDistance
     */
    private int getMaxPullDist() {
        int distance;
        if(mHasHeader && mHasFooter) {
            distance = Math.max(mHeaderView.getMeasuredHeight(), mFooterView.getMeasuredHeight());
        } else {
            distance = mHasFooter? mFooterView.getMeasuredHeight() :
                    (mHasHeader? mHeaderView.getMeasuredHeight() : 0);
        }
        /*if(DEBUG) {
            Log.d(TAG, "HeaderView Height: " + mHeaderView.getMeasuredHeight() +
                    ", FooterView Height: " + mFooterView.getMeasuredHeight());
        }*/
        return Math.max(distance, MAX_PULL_DIST);
    }

    protected final void updateUI(){
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
        refreshLoadingLayoutSize();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        // If there is no header and footer, transfer event to super method
        if(!mHasFooter && !mHasHeader) {
            return super.onInterceptTouchEvent(event);
        }

        int action = event.getAction();
        boolean result = false;
        switch (action){
            case MotionEvent.ACTION_DOWN:
                if(isLoading() || isRefreshing()){
                    result = false;
                    break;
                }
                mPullDist = 0;
                mLastY = event.getY();
                mArrowAnimationRepeat = INIT_ARROW_ANIMATION_COUNT;
                if(!isRefreshing() && !isLoading()) {
                    setPullStarted(true);
                    result = true;
                } else {
                    result = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // if the layout is refreshing, keep the mPullDist consistant
                result = !isRefreshing() && !isLoading() && isPullStarted();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                setPullStarted(false);
                // If the pull distance larger than maxPullDistance, start refreshing
                if(mPullDist >= getMaxPullDist()){
                    setRefreshing(true);
                    setLoading(false);
                } else if(mPullDist <= -getMaxPullDist()) {
                    setRefreshing(false);
                    setLoading(true);
                }
            default:
                break;
        }
        if(DEBUG) {
            Log.d(TAG, "onInterceptTouchEvent: mIsRefreshing is " + mIsRefreshing
                    + ", mPullDist = " + mPullDist + ", mLastY = " + mLastY
                    + ", result = " + result);
        }
        return result;
    }

    /**
     * If the event is not interrupted by onInterceptTouchEvent, the event will be handled by
     * this method
     * @param event - touch event to be handled
     * @return True if handled, or return false
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        // if the there are no header and footer, disable pull
        if(!mHasFooter && !mHasHeader) {
            return super.onTouchEvent(event);
        }

        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_MOVE:
                if(!isRefreshing() && isPullStarted()) {
                    mPullDist = getPullDistByY(event.getY());
                    mLastY = event.getY();
                    moveByScroll((int) -mPullDist);
                    // Pull down, if larger than max pull distance
                    if (mPullDist > getMaxPullDist()) {
                        startAnimationOfPullDown();
                    }
                    // Pull up, if less than minus max pull distance
                    if (mPullDist <= -getMaxPullDist()) {
                        mFooterView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mFooterView.show("icon");
                                // mFooterView.startAnimation();
                                endAnimation();
                            }
                        });
                    }
                } else if(isRefreshing()) {
                    setPullStarted(false);
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mPullDist > getMaxPullDist()){
                    startRefreshingAnimation();
                    endAnimation();
                } else if(mPullDist < -getMaxPullDist()) {
                    smoothScrollTo(2*getMaxPullDist());
                } else {
                    reset();
                }
            default:
                break;
        }
        Log.d(TAG, "onTouchEvent: mIsRefreshing is " + mIsRefreshing
                + ", mPullDist = " + mPullDist + ", mLastY = " + mLastY);
        return true;
    }

    private boolean isRefreshing() {
        return mIsRefreshing;
    }

    private void setRefreshing(boolean refreshing){
        mIsRefreshing = refreshing;
    }

    private boolean isLoading() {
        return mIsLoading;
    }

    private void setLoading(boolean loading) {
        mIsLoading = loading;
    }

    private boolean isPullStarted() {
        return mIsBeginToPull;
    }

    private void setPullStarted(boolean started){
        mIsBeginToPull = started;
    }

    private float getPullDistByY(float scrollValue){
        mRatio = (float) (2 + 2 * Math.tan(
                Math.PI / 2 / getMeasuredHeight() * (Math.abs(mPullDist))));
        return mPullDist + (scrollValue - mLastY) / mRatio;
    }

    /**
     * start the animation of rotate the arrow by 180 degrees
     */
    private void startAnimationOfPullDown(){
        mHeaderView.endAnimation();
        if(mArrowAnimationRepeat == INIT_ARROW_ANIMATION_COUNT) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(
                    mHeaderView.getRefreshingIcon(),
                    "rotation", 0f, -180f);
            animator.setDuration(100);
            mHeaderView.setAnimator(animator);
            mHeaderView.startAnimation();
            mArrowAnimationRepeat = NO_ARROW_ANIMATION_COUNT;
        }
    }

    /**
     * start the animation of unstopped rotating circle
     */
    private void startRefreshingAnimation(){
        smoothScrollTo(-getMaxPullDist());
        mHeaderView.endAnimation();
        mHeaderView.setRefreshingIcon(R.drawable.noarrow);
        ObjectAnimator refresh = ObjectAnimator.ofFloat(
                mHeaderView.getRefreshingIcon(),
                "rotation", 0f, 360f);
        refresh.setRepeatCount(ValueAnimator.INFINITE);
        refresh.setDuration(500);

        mHeaderView.setAnimator(refresh);
        mHeaderView.repeatAnimation();
    }

    /**
     * End the animation
     */
    private void endAnimation() {
        AnimationHandler animationHandler = new AnimationHandler(this);
        Message message = animationHandler.obtainMessage();
        animationHandler.sendMessageDelayed(message, 3000);
    }

    /**
     * Some reset methods - used to reset the layout to initial status
     */
    private void resetLoadingLayout() {
        mHeaderView.reset();
        mFooterView.reset();
        refreshLoadingLayoutSize();
        // Move to the top of layout
        moveByScroll(0);
    }

    private void resetRefreshingView() {
        // TODO
    }

    private void resetValue() {
        mArrowAnimationRepeat = INIT_ARROW_ANIMATION_COUNT;
        mRatio = 2.0f;
        mIsBeginToPull = false;
        mIsRefreshing = false;
        mStatus = IDLE;
    }

    public void reset() {
        resetLoadingLayout();
        resetRefreshingView();
        resetValue();
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;
        private OnSmoothScrollFinishedListener mListener;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        public SmoothScrollRunnable(int fromY, int toY, long duration, OnSmoothScrollFinishedListener listener) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mDuration = duration;
            mListener = listener;
            mInterpolator = mScrollAnimationInterpolator;
        }

        @Override
        public void run() {
            /**
             * Only set mStartTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {
                /**
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY)
                        * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentY = mScrollFromY - deltaY;
                moveByScroll(mCurrentY);
            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && mScrollToY != mCurrentY) {
                postOnAnimation(this);
            } else {
                if (null != mListener) {
                    mListener.onSmoothScrollFinished();
                }
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }

    private void moveByScroll(int newScrollValue) {
        int oldScrollValue = getScrollY();
        if(oldScrollValue == newScrollValue) {
            return;
        }
        scrollTo(0, newScrollValue);
    }

    protected final void smoothScrollTo(int newScrollValue, long duration, long delayMillis,
                                      OnSmoothScrollFinishedListener listener) {
        if (null != mCurrentScrollRunable) {
            mCurrentScrollRunable.stop();
        }

        int oldScrollValue = getScrollY();

        if (oldScrollValue != newScrollValue) {
            if (null == mScrollAnimationInterpolator) {
                // Default interpolator is a Decelerate Interpolator
                mScrollAnimationInterpolator = new DecelerateInterpolator();
            }
            mCurrentScrollRunable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration, listener);

            if (delayMillis > 0) {
                postDelayed(mCurrentScrollRunable, delayMillis);
            } else {
                post(mCurrentScrollRunable);
            }
        }
    }

    protected int getPullToRefreshScrollDuration() {
        return SMOOTH_SCROLL_DURATION_MS;
    }

    protected final void smoothScrollTo(int scrollValue) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDuration());
    }

    protected final void smoothScrollTo(int scrollValue, long duration) {
        smoothScrollTo(scrollValue, duration, 0, null);
    }
    
    private static class AnimationHandler extends Handler {
        private WeakReference<RefreshLayout> mRefreshLayoutWR;
        
        public AnimationHandler(RefreshLayout refreshLayout){
            mRefreshLayoutWR = new WeakReference<>(refreshLayout);
        }
        
        @Override
        public void handleMessage(Message msg){
            mRefreshLayoutWR.get().mIsRefreshing = false;
            mRefreshLayoutWR.get().reset();
        }
    }
}
