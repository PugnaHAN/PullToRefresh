package com.example.zhangjuh.pulltorefresh.pullable;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public class PullableImageView extends ImageView implements Pullable {
    private static final String TAG = PullableImageView.class.getSimpleName();

    public PullableImageView(Context context) {
        super(context);
    }

    public PullableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullableImageView(Context context, AttributeSet attrs, int defStyle) {
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
