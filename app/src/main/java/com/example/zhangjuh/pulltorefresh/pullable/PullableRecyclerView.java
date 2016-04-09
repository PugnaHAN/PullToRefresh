package com.example.zhangjuh.pulltorefresh.pullable;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.example.zhangjuh.pulltorefresh.R;

/**
 * Created by zhangjuh on 2016/4/7.
 */
public class PullableRecyclerView extends RecyclerView implements  Pullable{
    private static final String TAG = PullableRecyclerView.class.getSimpleName();

    public PullableRecyclerView(Context context){
        super(context);
    }

    public PullableRecyclerView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public PullableRecyclerView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }


    @Override
    public boolean isPullUpEnabled(){
        int lastItem = getLastVisiblePosition();
        int total = getLayoutManager().getItemCount();

        return lastItem == total - 1;
    }

    @Override
    public boolean isPullDownEnabled(){
        // 是否在最上面
        return getScrollY() == 0;
    }

    /**
     *  获取最后一条展示的信息
     *
     *  @return position
     */
    private int getLastVisiblePosition(){
        LayoutManager layoutManager = getLayoutManager();
        int position;
        if(layoutManager instanceof LinearLayoutManager){
            position = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if(layoutManager instanceof StaggeredGridLayoutManager){
            int lastPositions[] = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(
                    new int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()]);
            // 获取最大的位置
            position = lastPositions[0];
            for(int pos : lastPositions){
                if(position < pos){
                    position = pos;
                }
            }
        } else {
            position = getLayoutManager().getItemCount() - 1;
        }
        return position;
    }
}
