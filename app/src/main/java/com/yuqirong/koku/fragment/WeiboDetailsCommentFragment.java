package com.yuqirong.koku.fragment;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.yuqirong.koku.R;
import com.yuqirong.koku.adapter.LoadMoreAdapter;
import com.yuqirong.koku.adapter.WeiboCommentAdapter;
import com.yuqirong.koku.constant.AppConstant;
import com.yuqirong.koku.entity.Comment;
import com.yuqirong.koku.entity.Pic_urls;
import com.yuqirong.koku.entity.Status;
import com.yuqirong.koku.util.CommonUtil;
import com.yuqirong.koku.util.DateUtils;
import com.yuqirong.koku.util.JsonUtils;
import com.yuqirong.koku.util.LogUtils;
import com.yuqirong.koku.util.SharePrefUtil;
import com.yuqirong.koku.util.StringUtils;
import com.yuqirong.koku.view.AutoLoadRecyclerView;
import com.yuqirong.koku.view.DividerItemDecoration;
import com.yuqirong.koku.view.FixedSwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 微博详情页面评论
 * Created by Anyway on 2015/10/11.
 */
public class WeiboDetailsCommentFragment extends BaseFragment {

    public LinearLayout ll_item;
    public TextView tv_screen_name;
    public ImageView iv_avatar;
    public TextView tv_time;
    public TextView tv_device;
    public ImageView iv_verified;
    public ImageView iv_overflow;
    public TextView tv_repost_count;
    public TextView tv_comment_count;
    public TextView tv_text;
    public List<ImageView> iv_arrays = new ArrayList<>();
    public List<ImageView> iv_retweeted_arrays = new ArrayList<>();
    public TextView tv_retweeted_name_text;
    public View view_retweeted;
    public TextView tv_retweeted_repost_count;
    public TextView tv_retweeted_comment_count;
    public RelativeLayout rl_pics;
    public RelativeLayout rl_retweeted_pics;

    private static final int[] IMAGEVIEW_IDS = new int[]{R.id.iv_01, R.id.iv_02, R.id.iv_03, R.id.iv_04, R.id.iv_05, R.id.iv_06, R.id.iv_07, R.id.iv_08, R.id.iv_09};
    public static final String AT = "@";

    private TextView tv_favorite;
    private RadioButton rb_repost;
    private RadioButton rb_comment;
    private FrameLayout mFrameLayout;
    private RelativeLayout mRelativeLayout;

    private AutoLoadRecyclerView mAutoLoadRecyclerView;
    private WeiboCommentAdapter adapter;

    private long next_cursor;
    private boolean load; //是否正在加载
    private boolean refresh; //是否正在刷新
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private Status status;

    @Override
    public void initData(Bundle savedInstanceState) {
        if(savedInstanceState == null){
            status = (Status) getArguments().getSerializable("Status");
        }else{
            // TODO: 2015/10/12
        }
        if(status != null){
            getStatusesCountData();
            initWeiboData(status);
            getDataFromServer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(status.comments_count == 0){
            adapter.setIsLoadingMore(true);
            adapter.setLoadFinish();
        }
    }

    private void getStatusesCountData(){
        if (!TextUtils.isEmpty(status.idstr)) {
            String fresh_count_url = AppConstant.STATUSES_COUNT_URL + "?access_token=" + SharePrefUtil.getString(context, "access_token", "") + "&ids=" + status.idstr;
            LogUtils.i("批量获取指定微博的转发数评论数url ：" + fresh_count_url);
            getData(fresh_count_url, countListener, errorListener);
        }
    }

    Response.Listener<String> countListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String result) {
            try {
                JSONArray jsonArray = new JSONArray(result);
                JSONObject object = (JSONObject) jsonArray.get(0);
                status.comments_count = object.getInt("comments");
                status.reposts_count = object.getInt("reposts");
                status.attitudes_count = object.getInt("attitudes");
                String comments = getResources().getString(R.string.rb_comment) + CommonUtil.getNumString(status.comments_count);
                rb_comment.setText(comments);
                String reposts = getResources().getString(R.string.rb_repost) + CommonUtil.getNumString(status.reposts_count);
                rb_repost.setText(reposts);
                tv_favorite.setText(CommonUtil.getNumString(status.attitudes_count));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private void getDataFromServer() {
        if (!TextUtils.isEmpty(status.idstr)) {
            if(refresh){
                next_cursor=0;
                adapter.initFooterViewHolder();
                adapter.setIsLoadingMore(false);
            }
            String url = AppConstant.COMMENTS_SHOW_URL + "?count=20&id=" + status.idstr +
                    "&access_token=" + SharePrefUtil.getString(context, "access_token", "") + "&max_id=" + next_cursor;
            LogUtils.i("评论url ：" + url);
            getJsonData(url, listener, errorListener);
        }
    }

    Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject object) {
            processData(object);
        }
    };

    private void processData(JSONObject object) {
        try {
            if(refresh){
                adapter.getList().clear();
                adapter.getList().add(new Comment());
                adapter.getList().add(new Comment());
                mSwipeRefreshLayout.setRefreshing(false);
            }
            String str = object.getString("comments");
            next_cursor = object.getLong("next_cursor");
            adapter.getList().addAll(adapter.getList().size() - 1, JsonUtils.getListFromJson(str, Comment.class));
            if (load) {
                adapter.completeLoadMore(true);
                load = false;
            }
            if (next_cursor == 0 && !refresh) {
                adapter.setIsLoadingMore(true);
                adapter.setLoadFinish();
            }
            refresh = false;
            adapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {

        }
    };

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.from(context).inflate(R.layout.fragment_weibo_comments,null);
        View headerView = inflater.from(context).inflate(R.layout.fragment_weibo_comments_header,null);
        initHeaderView(headerView);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.mSwipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeResources(FixedSwipeRefreshLayout.SWIPE_REFRESH_LAYOUT_COLOR);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getStatusesCountData();
                refresh = true;
                getDataFromServer();
            }
        });
        mAutoLoadRecyclerView = (AutoLoadRecyclerView) view.findViewById(R.id.mAutoLoadRecyclerView);
        mAutoLoadRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
        adapter = new WeiboCommentAdapter(context);
        if (headerView != null) {
            ViewParent parent = headerView.getParent();
            if (parent != null) {
                ViewGroup group = (ViewGroup) parent;
                group.removeView(headerView);
            }
            adapter.addHeaderView(true, headerView);
        }
        adapter.getList().add(new Comment());
        adapter.getList().add(new Comment());
        adapter.setOnLoadingMoreListener(new LoadMoreAdapter.OnLoadingMoreListener() {
            @Override
            public void onLoadingMore() {
                    load = true;
                    getDataFromServer();
            }
        });
        mAutoLoadRecyclerView.setAdapter(adapter);
        return view;
    }

    private void initHeaderView(View view) {
        ll_item = (LinearLayout) view.findViewById(R.id.ll_item);
        ll_item.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        mRelativeLayout = (RelativeLayout) view.findViewById(R.id.mRelativeLayout);
        iv_avatar = (ImageView) view.findViewById(R.id.iv_avatar);
        tv_screen_name = (TextView) view.findViewById(R.id.tv_screen_name);
        iv_overflow = (ImageView) view.findViewById(R.id.iv_overflow);
        tv_time = (TextView) view.findViewById(R.id.tv_time);
        tv_device = (TextView) view.findViewById(R.id.tv_device);
        iv_verified = (ImageView) view.findViewById(R.id.iv_verified);
        tv_repost_count = (TextView) view.findViewById(R.id.tv_repost_count);
        tv_comment_count = (TextView) view.findViewById(R.id.tv_comment_count);
        tv_text = (TextView) view.findViewById(R.id.tv_text);
        rl_pics = (RelativeLayout) view.findViewById(R.id.rl_pics);
        rb_comment = (RadioButton) view.findViewById(R.id.rb_comment);
        rb_repost = (RadioButton) view.findViewById(R.id.rb_repost);
        tv_favorite = (TextView) view.findViewById(R.id.tv_favorite);
        mFrameLayout = (FrameLayout) view.findViewById(R.id.mFrameLayout);

        for (int i = 0; i < IMAGEVIEW_IDS.length; i++) {
            ImageView iv = (ImageView) view.findViewById(IMAGEVIEW_IDS[i]);
            iv_arrays.add(iv);
        }
    }

    private void initWeiboData(Status status) {
        tv_screen_name.setText(status.user.name);
        bitmapUtils.display(iv_avatar, status.user.profile_image_url);
        tv_time.setText(DateUtils.getWeiboDate(status.created_at));
        tv_device.setText(Html.fromHtml(status.source));
        //设置认证图标
        switch (status.user.verified_type) {
            case 0:
                iv_verified.setImageResource(R.drawable.avatar_vip);
                break;
            case -1:
                iv_verified.setImageResource(android.R.color.transparent);
                break;
            case 220:
                iv_verified.setImageResource(R.drawable.avatar_grassroot);
                break;
            default:
                iv_verified.setImageResource(R.drawable.avatar_enterprise_vip);
                break;
        }
        //隐藏微博 转发数和评论数
        tv_repost_count.setVisibility(View.GONE);
        tv_comment_count.setVisibility(View.GONE);
        iv_overflow.setVisibility(View.GONE);

        //设置微博内容
        SpannableString weiBoContent = StringUtils.getWeiBoContent(context, status.text, tv_text);
        tv_text.setText(weiBoContent);

        if (status.pic_urls != null && status.pic_urls.size() > 0) {
            initImageView(rl_pics, iv_arrays, status.pic_urls);
        }
        //设置被转发的内容
        if (status.retweeted_status != null) {
            processRetweeted();
            ll_item.addView(view_retweeted);
        }
        String comments = getResources().getString(R.string.rb_comment) + CommonUtil.getNumString(status.comments_count);
        rb_comment.setText(comments);
        String reposts = getResources().getString(R.string.rb_repost) + CommonUtil.getNumString(status.reposts_count);
        rb_repost.setText(reposts);
        tv_favorite.setText(CommonUtil.getNumString(status.attitudes_count));
        rb_comment.setOnCheckedChangeListener(onCheckedChangeListener);
        rb_repost.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    // 处理被转发的View
    private void processRetweeted() {
        view_retweeted = LayoutInflater.from(context).inflate(R.layout.weibo_retweeted_item, null);
        initRetweetedView();
        SpannableString weiBoContent = StringUtils.getWeiBoContent(context, AT + status.retweeted_status.user.name + context.getResources().getString(R.string.colon) + status.retweeted_status.text, tv_retweeted_name_text);
        tv_retweeted_name_text.setText(weiBoContent);

        if (status.retweeted_status.pic_urls != null && status.retweeted_status.pic_urls.size() > 0) {
            initImageView(rl_retweeted_pics, iv_retweeted_arrays, status.retweeted_status.pic_urls);
        }
    }

    private void initRetweetedView() {
        tv_retweeted_name_text = (TextView) view_retweeted.findViewById(R.id.tv_retweeted_name_text);
        tv_retweeted_repost_count = (TextView) view_retweeted.findViewById(R.id.tv_retweeted_repost_count);
        tv_retweeted_comment_count = (TextView) view_retweeted.findViewById(R.id.tv_retweeted_comment_count);
        rl_retweeted_pics = (RelativeLayout) view_retweeted.findViewById(R.id.rl_pics);
        for (int i = 0; i < IMAGEVIEW_IDS.length; i++) {
            ImageView iv = (ImageView) view_retweeted.findViewById(IMAGEVIEW_IDS[i]);
            iv_retweeted_arrays.add(iv);
        }
        //设置被转发微博 转发数和评论数
        tv_retweeted_repost_count.setText(CommonUtil.getNumString(status.retweeted_status.reposts_count));
        tv_retweeted_comment_count.setText(CommonUtil.getNumString(status.retweeted_status.comments_count));
    }

    private void initImageView(RelativeLayout rl, List<ImageView> iv_arrays, List<Pic_urls> pic_urls) {
        rl.setVisibility(View.VISIBLE);
        for (int i = 0; i < iv_arrays.size(); i++) {
            if (i < pic_urls.size()) {
                iv_arrays.get(i).setVisibility(View.VISIBLE);
                bitmapUtils.display(iv_arrays.get(i), pic_urls.get(i).thumbnail_pic);
            } else {
                iv_arrays.get(i).setVisibility(View.GONE);
            }
        }
    }

    CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // TODO: 2015/10/11 新浪微博API没有提供查看转发微博列表的接口
//            if (buttonView.getId() == R.id.rb_comment && isChecked) {
//                LogUtils.i("comment button : " + isChecked);
//                if (commentFragment == null) {
//                    commentFragment = new WeiboDetailsCommentFragment(mRelativeLayout);
//                    Bundle bundle = new Bundle();
//                    bundle.putString("idstr", status.idstr);
//                    commentFragment.setArguments(bundle);
//                }
//                fragmentManager.beginTransaction().replace(R.id.mFrameLayout, commentFragment, "commentFragment").commit();
//            } else if (buttonView.getId() == R.id.rb_repost && isChecked) {
//                LogUtils.i("repost button : " + isChecked);
//                if (repostFragment == null) {
//                    repostFragment = new WeiboDetailsRepostFragment();
//                }
//                fragmentManager.beginTransaction().replace(R.id.mFrameLayout, repostFragment, "repostFragment").commit();
//            }
        }
    };


}