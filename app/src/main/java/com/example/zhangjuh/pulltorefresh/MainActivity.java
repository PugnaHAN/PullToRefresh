package com.example.zhangjuh.pulltorefresh;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.zhangjuh.pulltorefresh.pullable.LoadingLayout;

public class MainActivity extends AppCompatActivity {


    private Button mStartButton;
    private Button mCancelButton;
    private LoadingLayout mLoadingLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mLoadingLayout = (LoadingLayout) findViewById(R.id.loadingLayout);
//        mStartButton = (Button) findViewById(R.id.startBtn);
//        mStartButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mLoadingLayout.repeatAnimation();
//            }
//        });
//        mCancelButton = (Button) findViewById(R.id.cancelBtn);
//        mCancelButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mLoadingLayout.endAnimation();
//            }
//        });
    }
}
