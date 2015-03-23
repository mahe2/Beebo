
package org.zarroboogs.weibo.activity;

import org.zarroboogs.weibo.GlobalContext;
import org.zarroboogs.weibo.R;
import org.zarroboogs.weibo.WebViewActivity;
import org.zarroboogs.weibo.bean.AccountBean;
import org.zarroboogs.weibo.db.AccountDatabaseManager;
import org.zarroboogs.weibo.db.table.AccountTable;
import org.zarroboogs.weibo.db.task.AccountDBTask;
import org.zarroboogs.weibo.setting.SettingUtils;
import org.zarroboogs.weibo.support.utils.BundleArgsConstants;
import org.zarroboogs.weibo.support.utils.ThemeUtility;
import org.zarroboogs.weibo.support.utils.Utility;
import org.zarroboogs.weibo.widget.ChangeLogDialog;

import com.umeng.analytics.MobclickAgent;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.AlertDialog.Builder;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AccountActivity extends BaseLoginActivity implements LoaderManager.LoaderCallbacks<List<AccountBean>> {

    private static final String ACTION_OPEN_FROM_APP_INNER = "org.zarroboogs.weibo:accountactivity";

    private static final String ACTION_OPEN_FROM_APP_INNER_REFRESH_TOKEN = "org.zarroboogs.weibo:accountactivity_refresh_token";

    private static final String REFRESH_ACTION_EXTRA = "refresh_account";

    private final int ADD_ACCOUNT_REQUEST_CODE = 0;

    private final int LOADER_ID = 0;

    private ListView listView = null;

    private AccountAdapter listAdapter = null;

    private List<AccountBean> accountList = new ArrayList<AccountBean>();
    private Toolbar mToolBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        MobclickAgent.setDebugMode(false);
        MobclickAgent.openActivityDurationTrack(false);
        MobclickAgent.updateOnlineConfig(this);

        // CookieManager manager = CookieManager.getInstance();
        // manager.removeAllCookie();

        String action = getIntent() != null ? getIntent().getAction() : null;

        if (ACTION_OPEN_FROM_APP_INNER.equals(action)) {
            // empty
        } else if (ACTION_OPEN_FROM_APP_INNER_REFRESH_TOKEN.equals(action)) {
            // empty
        } else {
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.accountactivity_layout);
        mToolBar = (Toolbar) findViewById(R.id.accountToolBar);

        mToolBar.inflateMenu(R.menu.actionbar_menu_accountactivity);
        mToolBar.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem arg0) {
                int itemId = arg0.getItemId();
				if (itemId == R.id.menu_add_account) {
					showAddAccountDialog(false);
				} else {
				}
                return false;
            }
        });

        // getActionBar().setTitle(getString(R.string.app_name));
        listAdapter = new AccountAdapter();
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AccountListItemClickListener());
        listView.setAdapter(listAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AccountMultiChoiceModeListener());
        getLoaderManager().initLoader(LOADER_ID, null, this);

        if (SettingUtils.firstStart()) {
            showChangeLogDialog();
        }

        if (ACTION_OPEN_FROM_APP_INNER_REFRESH_TOKEN.equals(action)) {

            showAddAccountDialog(false);

            AccountBean accountBean = getIntent().getParcelableExtra(REFRESH_ACTION_EXTRA);

            Toast.makeText(this, String.format(getString(R.string.account_token_has_expired), accountBean.getUsernick()),
                    Toast.LENGTH_SHORT).show();

        }

    }

    public static Intent newIntent() {
        Intent intent = new Intent(GlobalContext.getInstance(), AccountActivity.class);
        intent.setAction(ACTION_OPEN_FROM_APP_INNER);
        return intent;
    }

    public static Intent newIntent(AccountBean refreshAccount) {
        Intent intent = new Intent(GlobalContext.getInstance(), AccountActivity.class);
        intent.setAction(ACTION_OPEN_FROM_APP_INNER_REFRESH_TOKEN);
        intent.putExtra(REFRESH_ACTION_EXTRA, refreshAccount);
        return intent;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showChangeLogDialog() {
        ChangeLogDialog changeLogDialog = new ChangeLogDialog(this);
        changeLogDialog.show();
    }

    private void showAddAccountDialog(boolean isHack) {

        if (true) {

            Intent intent = OAuthActivity.oauthIntent(this, isHack);
            startActivityForResult(intent, ADD_ACCOUNT_REQUEST_CODE);
            return;
        }
        
        
        final ArrayList<Class> activityList = new ArrayList<Class>();
        ArrayList<String> itemValueList = new ArrayList<String>();

        activityList.add(OAuthActivity.class);
        itemValueList.add(getString(R.string.oauth_login));

        if (true || SettingUtils.isBlackMagicEnabled()) {
            activityList.add(BlackMagicActivity.class);
            itemValueList.add(getString(R.string.hack_login));
        }

        new AlertDialog.Builder(this).setItems(itemValueList.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(AccountActivity.this, activityList.get(which));
                startActivityForResult(intent, ADD_ACCOUNT_REQUEST_CODE);
            }
        }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_ACCOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
            refresh();
            if (data == null) {
                return;
            }
            String expires_time = data.getExtras().getString("expires_in");
            long expiresDays = TimeUnit.SECONDS.toDays(Long.valueOf(expires_time));

            String content = String.format(getString(R.string.token_expires_in_time), String.valueOf(expiresDays));
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(content).setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

            builder.show();

        }
    }

    private void refresh() {
        getLoaderManager().getLoader(LOADER_ID).forceLoad();
    }

    @Override
    public Loader<List<AccountBean>> onCreateLoader(int id, Bundle args) {
        return new AccountDBLoader(AccountActivity.this, args);
    }

    @Override
    public void onLoadFinished(Loader<List<AccountBean>> loader, List<AccountBean> data) {
        accountList = data;
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<AccountBean>> loader) {
        accountList = new ArrayList<AccountBean>();
        listAdapter.notifyDataSetChanged();
    }

    private void remove() {
        Set<String> set = new HashSet<String>();
        long[] ids = listView.getCheckedItemIds();
        for (long id : ids) {
            set.add(String.valueOf(id));
        }
        accountList = AccountDBTask.removeAndGetNewAccountList(set);
        listAdapter.notifyDataSetChanged();
    }

    private static class AccountDBLoader extends AsyncTaskLoader<List<AccountBean>> {

        public AccountDBLoader(Context context, Bundle args) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();
            forceLoad();
        }

        public List<AccountBean> loadInBackground() {
            return AccountDBTask.getAccountList();
        }
    }

    
    private AlertDialog mDoorAlertDialog;
    private class AccountListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final AccountBean selectAccountBean = accountList.get(i);
            String cookie = selectAccountBean.getCookie();
            
            if (TextUtils.isEmpty(selectAccountBean.getAccess_token_hack())) {
				showAddAccountDialog(true);
				return;
			}
            
            if (TextUtils.isEmpty(selectAccountBean.getPwd())) {
                Builder builder = new Builder(AccountActivity.this);
                mDoorAlertDialog = builder.create();

                mDoorAlertDialog.show();
                mDoorAlertDialog.getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                mDoorAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                mDoorAlertDialog.getWindow().setContentView(R.layout.name_pwd_dialog_layout);
                
                final EditText username = (EditText) mDoorAlertDialog.findViewById(R.id.tellMeUser);
                final EditText pwd = (EditText) mDoorAlertDialog.findViewById(R.id.tellMePwd);
                Button button = (Button) mDoorAlertDialog.findViewById(R.id.upBtn);
                
                button.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						AccountDatabaseManager manager = new AccountDatabaseManager(getApplicationContext());
						manager.updateAccount(AccountTable.ACCOUNT_TABLE, selectAccountBean.getUid(),
								AccountTable.USER_NAME, username.getText().toString().trim());
						manager.updateAccount(AccountTable.ACCOUNT_TABLE, selectAccountBean.getUid(),
								AccountTable.USER_PWD, pwd.getText().toString().trim());
						GlobalContext.getInstance().updateAccountBean();
						refresh();
						mDoorAlertDialog.hide();
					}
				});
//            	Intent intent = new Intent(AccountActivity.this, BlackMagicActivity.class);
//                startActivityForResult(intent, ADD_ACCOUNT_REQUEST_CODE);
                
            	return;
			}

            if (!Utility.isTokenValid(selectAccountBean)) {

                showAddAccountDialog(false);

                return;
            }

            if (false && TextUtils.isEmpty(cookie)) {
                Intent intent = new Intent(AccountActivity.this, WebViewActivity.class);
                intent.putExtra(BundleArgsConstants.ACCOUNT_EXTRA, selectAccountBean);
                startActivity(intent);
            } else {
                Intent intent = MainTimeLineActivity.newIntent(selectAccountBean);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                getCookieStore().clear();

                finish();
            }

        }
    }

    private class AccountMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_menu_accountactivity, menu);
            mode.setTitle(getString(R.string.account_management));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
			if (itemId == R.id.menu_remove_account) {
				remove();
				mode.finish();
				return true;
			}
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class AccountAdapter extends BaseAdapter {

        int checkedBG;

        int defaultBG;

        public AccountAdapter() {
            defaultBG = getResources().getColor(R.color.transparent);
            checkedBG = ThemeUtility.getColor(AccountActivity.this, R.attr.listview_checked_color);

        }

        @Override
        public int getCount() {
            return accountList.size();
        }

        @Override
        public Object getItem(int i) {
            return accountList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return Long.valueOf(accountList.get(i).getUid());
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {

            LayoutInflater layoutInflater = getLayoutInflater();

            View mView = layoutInflater.inflate(R.layout.accountactivity_listview_item_layout, viewGroup, false);
            mView.findViewById(R.id.listview_root).setBackgroundColor(defaultBG);

            if (listView.getCheckedItemPositions().get(i)) {
                mView.findViewById(R.id.listview_root).setBackgroundColor(checkedBG);
            }

            TextView textView = (TextView) mView.findViewById(R.id.account_name);
            if (accountList.get(i).getInfo() != null) {
                textView.setText(accountList.get(i).getInfo().getScreen_name());
            } else {
                textView.setText(accountList.get(i).getUsernick());
            }
            ImageView imageView = (ImageView) mView.findViewById(R.id.imageView_avatar);

            if (!TextUtils.isEmpty(accountList.get(i).getAvatar_url())) {
                getBitmapDownloader().downloadAvatar(imageView, accountList.get(i).getInfo(), false);
            }

            TextView token = (TextView) mView.findViewById(R.id.token_expired);
            if (!Utility.isTokenValid(accountList.get(i))) {
                token.setVisibility(View.VISIBLE);
            } else {
                token.setVisibility(View.GONE);
            }

            return mView;
        }
    }

}
