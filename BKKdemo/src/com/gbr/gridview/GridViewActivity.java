package com.gbr.gridview;

import java.util.ArrayList;
import java.util.List;

import com.gbr.bbkdemo.R;
import com.gbr.video.PlayActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GridViewActivity extends Activity {
	private static final String TAG = "MOVIE_ITEM";
	List<Item> items;
	RelativeLayout itmel;
	private GridView gridView;
	private MyAdapter adapter;
    private DisplayMetrics dm;
    private int NUM = 3; // 每行显示个数
    private int hSpacing = 20;// 水平间距
    private int vSpacing = 50;//垂直间距
    int count;
    private TextView edit;
    private ImageView mBack;
    private LinearLayout linearLayout;
    //状态
    public static int STATUS=0;
    public static final int NORMAL=1;
    public static final int DELETE=2;
    private VideoManager videoManager;
    private boolean isEdit;
    private ProgressBar mProgressBar;
    String language;
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE); //没有菜单栏
		setContentView(R.layout.gridview_activity);
		Intent intent = getIntent();
		String picNum = intent.getStringExtra("picNum");
	    language=intent.getStringExtra("language");
		Log.d("aaaa", language+"-------");
		videoManager=new VideoManager(this);
		STATUS=NORMAL;
		initView();
		setBg(picNum);
		getScreenDen();
		setGridView();
		adapter = new MyAdapter(this,dm);
		gridView.setAdapter(adapter);
		asynLoadBitmap();
	
	}
	private void initView(){
		gridView = (GridView) findViewById(R.id.grid);
		mBack = (ImageView) findViewById(R.id.backG);
		edit=(TextView) findViewById(R.id.edit);
	    mProgressBar=(ProgressBar) findViewById(R.id.progressBar1);
		linearLayout = (LinearLayout) findViewById(R.id.bg);
	    gridView.setOnItemClickListener(itemListener);
	    gridView.setOnScrollListener(onScrollListener);
		mBack.setOnClickListener(listener);
		edit.setOnClickListener(listener);
		if (language.equals("English")){
				edit.setText("Edit");
				edit.setTextSize(24);
		}else {
				edit.setText("編輯");
				edit.setTextSize(24);
		};
	}
	private OnClickListener listener=new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.edit:
				if (isEdit) {
					if (language.equals("English")) {
						edit.setText("Cancel");
					}else {
						edit.setText("取消");
					}
					edit.setTextSize(24);
					mBack.setVisibility(View.INVISIBLE);
					STATUS=DELETE;
					isEdit=false;
					adapter.notifyDataSetChanged();
				}else {
					if (language.equals("English")) {
						edit.setText("Edit");
					}else {
						edit.setText("編輯");
					}
					edit.setTextSize(24);
					mBack.setVisibility(View.VISIBLE);
					STATUS=NORMAL;
					isEdit=true;
					adapter.notifyDataSetChanged();
				}
				break;
			case R.id.backG:
				GridViewActivity.this.finish();
				break;
			}
		}
	};
	private OnItemClickListener itemListener=new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			if (STATUS==NORMAL) {
				Intent intent = new Intent(GridViewActivity.this,PlayActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("filePath", items.get(position).getFilePath());
				intent.putExtras(bundle);
				System.out.println(position);
				startActivity(intent);
			}
			if (STATUS==DELETE) {
				Item item=new Item();
			    item=items.get(position);
			    String path=item.getFilePath();
			    items.remove(item);
				//deleteDate(path);
			    videoManager.deleteDate(path);
				adapter.notifyDataSetChanged();
			}
		}
	};
	private OnScrollListener onScrollListener=new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
	
			switch (scrollState) {
			//
			case OnScrollListener.SCROLL_STATE_FLING:
				adapter.setFling(true);
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				adapter.setFling(false);
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				adapter.setFling(false);
				adapter.notifyDataSetChanged();
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub
			
		}
	};
	/**
	 * 设置背景图片
	 * @param picNum
	 */
	private void setBg(String picNum) {
		// TODO Auto-generated method stub
		if(picNum.equals("scene1_blur")){
			linearLayout.setBackgroundResource(R.drawable.scene1_blur);
		}
		if(picNum.equals("scene2_blur")){
			linearLayout.setBackgroundResource(R.drawable.scene2_blur);
		}
		if(picNum.equals("scene3_blur")){
			linearLayout.setBackgroundResource(R.drawable.scene3_blur);
		}
		if(picNum.equals("scene4_blur")){
			linearLayout.setBackgroundResource(R.drawable.scene4_blur);
		}
	}
    private void asynLoadBitmap(){
    	mProgressBar.setVisibility(View.VISIBLE);
    	gridView.setVisibility(View.INVISIBLE);
    	items = new ArrayList<Item>();
    	new Thread(new Runnable() {
			
			@Override
			public void run() {
				//加载图片
				items=videoManager.query();
			
				// TODO 自动生成的方法存根
				runOnUiThread(new Runnable() {
					public void run() {
						mProgressBar.setVisibility(View.INVISIBLE);
						gridView.setVisibility(View.VISIBLE);
						//initView();
				        adapter.addItems(items);
						adapter.notifyDataSetChanged();
					}
				});
			}
			
		}).start();	
    }
   
	
	private void getScreenDen() {
        dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
      
    }
	
	private void setGridView() {
		// TODO Auto-generated method stub
		dm = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics(dm);
	  
        gridView.setColumnWidth(dm.widthPixels / NUM - 10);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setVerticalSpacing(vSpacing);
        gridView.setHorizontalSpacing(hSpacing);
        gridView.setNumColumns(3);
		
	}

	
}
