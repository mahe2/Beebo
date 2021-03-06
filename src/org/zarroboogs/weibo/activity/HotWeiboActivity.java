package org.zarroboogs.weibo.activity;

import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.fragment.HotWeiboViewPagerFragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.Toolbar;

public class HotWeiboActivity extends TranslucentStatusBarActivity {

    private Toolbar mToolbar;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	// TODO Auto-generated method stub
    	super.onCreate(savedInstanceState);
    	 setContentView(R.layout.hotweibo_activity_layout);
         mToolbar = (Toolbar) findViewById(R.id.hotWeiboToolbar);
         
         buildContent();
    }

    private void buildContent() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getSupportFragmentManager().findFragmentByTag(HotWeiboViewPagerFragment.class.getName()) == null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.hotWeiboContent, new HotWeiboViewPagerFragment(),HotWeiboViewPagerFragment.class.getName())
                            .commitAllowingStateLoss();
                    getSupportFragmentManager().executePendingTransactions();
                }
            }
        });
    }
}
