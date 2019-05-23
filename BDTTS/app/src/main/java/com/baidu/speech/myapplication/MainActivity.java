package com.baidu.speech.myapplication;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.baidu.speech.myapplication.BaiduST.STActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mBtnSt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }

    private void initView() {
        mBtnSt=findViewById(R.id.btn_st);
    }

    private void initEvent() {
        mBtnSt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_st:
                startActivity(new Intent(this, STActivity.class));
                break;
            default:
                break;
        }
    }
}
