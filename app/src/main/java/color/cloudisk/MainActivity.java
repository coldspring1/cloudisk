package color.cloudisk;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.IntentService;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends ActionBarActivity  {
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    GoogleAccountCredential m_credential;
    private Context mContext;
    //static final int REQUEST_CODE_RESOLUTION = 1;
   private static final String STORED_ACCOUNT = "accountName";
    public static final String GDRIVE_REFRESHING_ACTIVITY = "refreshing sent from activity";
    public static final String SET_GDRIVE_REFRESHING_STATE ="set fragment to refreshing state";
    ActivityReceiver mRefresh;
    Boolean isFragmentRefreshing = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        mRefresh = new ActivityReceiver();
        IntentFilter refreshingFilter = new IntentFilter(listFileFragment.GDRIVE_REFRESHING_FRAGMENT);
        IntentFilter refreshedFilter = new IntentFilter(listFileFragment.GDRIVE_REFRESHED_FRAGMENT);
        IntentFilter navRefreshFilter=new IntentFilter(listFileFragment.GDRIVE_CHANGEFOLDER_REFRESH);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefresh,refreshingFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefresh,refreshedFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefresh,navRefreshFilter);
        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener(){
            public void onBackStackChanged(){


                if(getFragmentManager().getBackStackEntryCount() >0){
                    for(int i=0;i<getFragmentManager().getBackStackEntryCount();i++){
                        FragmentManager.BackStackEntry fg=getFragmentManager().getBackStackEntryAt(i);
                        Log.e("backstack change","still in backstack is "+ i+ " "+fg);
                    }
                }else{
                    Log.e("backstack change","backentry empty");
                }
            }
        });


        SharedPreferences appInfo = getSharedPreferences(getString(R.string.appInfo),
                Context.MODE_PRIVATE);
        m_credential = GoogleAccountCredential.usingOAuth2(
                mContext, Arrays.asList(DriveScopes.DRIVE))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(appInfo.getString(getString(R.string.accountName), null));
            }


    @Override
    protected void onResume() {
        super.onResume();
        if (isGooglePlayServicesAvailable()) {
            listFileFragment fg =(listFileFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            refreshResults();
            } else {
            Toast.makeText(this, "google service is not here", Toast.LENGTH_SHORT).show();
          }
    }


    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
    }


    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                MainActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void refreshResults() {
        if (m_credential.getSelectedAccountName() == null) {
            chooseAccount();
        }
        else {
                listFileFragment googleFragment = (listFileFragment)getFragmentManager()
                        .findFragmentById(R.id.fragment_container);

                   if(googleFragment == null){
                       Log.e("","activity add root fragment");
                       googleFragment = listFileFragment.newInstance("root");
                       FragmentTransaction ft = getFragmentManager().beginTransaction();
                       ft.replace(R.id.fragment_container,googleFragment,"TAG_root");

                       ft.commit();
                   }
        }
    }


    private void chooseAccount() {
        startActivityForResult(
                m_credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }


    @Override
    public void onBackPressed(){
        listFileFragment fg =(listFileFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
        if (fg.getPreviousViewData().size()>0 ){
            if(fg.isRefreshed){
            fg.backToRefreshedData();
            }
            fg.backToPreviousData();
        } else {
            super.onBackPressed();


        }
    }


    private class ActivityReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent){
            String receiveAction = intent.getAction();
            if(receiveAction.equals(listFileFragment.GDRIVE_REFRESHING_FRAGMENT)){
              isFragmentRefreshing =true;
                //listFileFragment fg =(listFileFragment)getFragmentManager().findFragmentById(R.id.fragment_container);
            }
            if(receiveAction.equals(listFileFragment.GDRIVE_REFRESHED_FRAGMENT)){
                isFragmentRefreshing =false;
                //Log.e("refreshed activity","refreshed received by activity");
            }

        }
    }

    @Override
    protected  void onPause(){
        super.onPause();
        if(mRefresh!= null){
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefresh);

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                  String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                       m_credential.setSelectedAccountName(accountName);
                      SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.appInfo),Context.MODE_PRIVATE).edit();
                        editor.putString(getString(R.string.accountName),accountName);

                        editor.putLong(getString(R.string.changeId),0L);
                        editor.apply();


                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                   chooseAccount();
                }
                break;
        }

    }



}







