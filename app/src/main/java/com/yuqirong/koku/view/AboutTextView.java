package com.yuqirong.koku.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuqirong.koku.R;

/**
 * Created by Administrator on 2015/11/3.
 */
public class AboutTextView extends LinearLayout {

    private TextView tv_title;
    private TextView tv_content;

    public AboutTextView(Context context) {
        this(context, null);
    }

    public AboutTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.AboutTextView);
        String title = a.getString(R.styleable.AboutTextView_subject);
        String content = a.getString(R.styleable.AboutTextView_content);
        initView(context, title, content);
        a.recycle();
    }

    private void initView(Context context, String title, String content) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_about_item, this);
        tv_title = (TextView) view.findViewById(R.id.tv_title);
        tv_content = (TextView) view.findViewById(R.id.tv_content);
        tv_title.setText(title);
        tv_content.setText(content);
    }

    public void setTitle(String title) {
        tv_title.setText(title);
    }

    public void setContent(String content) {
        tv_content.setText(content);
    }

}
