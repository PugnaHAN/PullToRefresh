package com.example.zhangjuh.pulltorefresh.pullable;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public class PullableTextView extends TextView implements Pullable {
    private static final String TAG = PullableRecyclerView.class.getSimpleName();

    public PullableTextView(Context context){
        super(context);
    }

    public PullableTextView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public PullableTextView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    public boolean isPullUpEnabled() {
        return true;
    }

    @Override
    public boolean isPullDownEnabled() {
        return true;
    }
}
