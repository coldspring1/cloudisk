package color.cloudisk;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;

import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import color.cloudisk.MainActivity;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.model.ParentList;
import com.google.api.services.drive.model.ParentReference;
import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import color.cloudisk.FileDirectoryContract.fileTable;


public class listFileFragment extends  swipeFragment {

   MainActivity mainActivity;
    ProgressDialog mProgress;
    private  String curChooseFolder;
    private String chooseFileId;
    private String url;
    private String token;
    ArrayList<String> cachedFolderList = new ArrayList<>();
    ArrayList<String> cachedFileList =new ArrayList<>();
    ArrayList<String> fileNameList ;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private String refreshStatus;
    public static final String GDRIVE_REFRESHING_FRAGMENT = "refreshing send from fragment";
    public static final String GDRIVE_REFRESHED_FRAGMENT="refresh complete sent from fragment";
    public static final String GDRIVE_CHANGEFOLDER_REFRESH="navigate while refreshing";
    private ActivityReceiver mRefresh;
    private IntentFilter refreshedFilter;
    private ArrayList<ArrayList<Set>> previousViewData = new ArrayList<>();
    Boolean isRefreshed = false;


    public static listFileFragment newInstance(String folderId){
        listFileFragment f = new listFileFragment();
        Bundle args = new Bundle();
        args.putString("folderId",folderId);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        curChooseFolder=getArguments().get("folderId").toString();
        if(savedInstanceState != null){
            fileNameList=savedInstanceState.getStringArrayList("fileName");
            cachedFileList=savedInstanceState.getStringArrayList("cachedFileList");
            cachedFolderList=savedInstanceState.getStringArrayList("cachedFolderList");
            customAdapter mAdapter = new customAdapter(fileNameList);
            setListAdapter(mAdapter);
        }
            mainActivity=(MainActivity)getActivity();
            curChooseFolder=getArguments().getString("folderId");
    }


    public String getCurChooseFolder(){
        return curChooseFolder;
    }







    @Override
        public void onActivityCreated(Bundle savedInstanceState){
            super.onActivityCreated(savedInstanceState);
        /*if(mainActivity.isFragmentRefreshing){
            getSwipeRefreshLayout().post(new Runnable() {
                @Override public void run() {
                    getSwipeRefreshLayout().setRefreshing(true);
                }
            });
        }*/
        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
    @Override
         public void onRefresh(){
        mRefresh = new ActivityReceiver();
        IntentFilter refreshingFilter = new IntentFilter(MainActivity.GDRIVE_REFRESHING_ACTIVITY);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefresh,refreshingFilter);
        refreshedFilter = new IntentFilter(GDRIVE_REFRESHED_FRAGMENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefresh,refreshedFilter);
        Intent refreshSignal = new Intent();
        refreshSignal.setAction(GDRIVE_REFRESHING_FRAGMENT);
       LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(refreshSignal);

        try {
            updateFileList();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
        });
        if (fileNameList ==null & savedInstanceState==null){
                new getFileList(curChooseFolder).execute();
        }
       /* if(fragmentRefreshing){
            setRefreshing(true);
            Log.e("fragment lifecycle",isRefreshing()+"");

        }*/
    }



    @Override
    public void onResume(){
        super.onResume();
        Log.e("onresume",curChooseFolder +"");

    }


    private class ActivityReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent){
            if(intent.getAction().equals(MainActivity.GDRIVE_REFRESHING_ACTIVITY)){


            }
            if(intent.getAction().equals(GDRIVE_REFRESHED_FRAGMENT)){
                getSwipeRefreshLayout().setRefreshing(false);
                getSwipeRefreshLayout().clearAnimation();
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefresh);
                Fragment fg = listFileFragment.newInstance(curChooseFolder) ;

                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container, fg, "TAG_" + curChooseFolder);

                   if(!curChooseFolder.equals("root")) ft.addToBackStack(curChooseFolder);

                    getFragmentManager().popBackStackImmediate();
                    ft.commit();
            }
        }
    }





    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if(!cachedFolderList.get(position).equals("0") && cachedFolderList != null){
            ArrayList<Set> viewData = new ArrayList<>();
            SharedPreferences appInfo = getActivity().getSharedPreferences(getString(R.string.appInfo),
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor =appInfo.edit();
            editor.putString("curfolder", curChooseFolder);
            viewData.add(appInfo.getStringSet("fileList", null));
            viewData.add(appInfo.getStringSet("folderList", null));
            viewData.add(appInfo.getStringSet("fileName", null));
            String viewedData = appInfo.getString("viewedData",null);
            
            previousViewData.add(viewData);
           String data = new Gson().toJson(previousViewData);
            editor.putString("viewedData",data);
            editor.apply();
            String folderId = cachedFolderList.get(position);
            curChooseFolder = folderId;
            cachedFileList.clear();
            cachedFolderList.clear();
            fileNameList.clear();
            new getFileList(folderId).execute();
            Log.e("onclick",curChooseFolder);
            //}

        }else{
            String file = cachedFileList.get(position);
            if(isDeviceOnline()) {
                downloadFile(file);
            }else{
                Toast.makeText(getActivity(), "No network connection available.", Toast.LENGTH_SHORT).show();
            }
        }

    }

ArrayList<ArrayList<Set>> getPreviousViewData(){
    return previousViewData;
}

    void backToPreviousData(){

        int position = previousViewData.size() - 1 ;
      fileNameList = new ArrayList<String>(previousViewData.get(position).get(3));
      cachedFolderList =new ArrayList<String>(previousViewData.get(position).get(2));
      cachedFileList = new ArrayList<String>(previousViewData.get(position).get(1));
        Log.e("back",cachedFolderList+"--"+cachedFileList);
        customAdapter adapter = new customAdapter(fileNameList);
       setListAdapter(adapter);

        previousViewData.remove(position);
    }

    void backToRefreshedData(){
        SharedPreferences appInfo = getActivity().getSharedPreferences(getString(R.string.appInfo),
                Context.MODE_PRIVATE);
        String backFolderId = appInfo.getString("curfolder",null);
        new getFileList(backFolderId).execute();
    }


    private void updateFileList() throws InterruptedException {

        new Thread(new Runnable() {
            @Override
            public void run() {

                FileDirectoryDbHelper openDbHelper = new FileDirectoryDbHelper
                        (getActivity().getApplicationContext());
                SQLiteDatabase readDb = openDbHelper.getReadableDatabase();
                ContentValues newValue = new ContentValues();
                try {
                    HttpTransport transport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                    GoogleAccountCredential credential =mainActivity.m_credential;
                    Drive changeService = new com.google.api.services.drive.Drive.Builder(
                            transport, jsonFactory, credential)
                            .setApplicationName("Google Drive")
                            .build();
                    Log.e("","begin refresh");
                    List<Change> changes = retrieveAllChange(changeService);
                    for(Change change:changes){
                        String changeFileId = change.getFileId();
                        String[] projection ={fileTable.FILE_ID};
                        Cursor queryId = readDb.query(fileTable.TABLE_NAME,projection,
                                fileTable.FILE_ID + "= '"+changeFileId+"'",
                                null, null, null, null);
                        if(queryId.moveToFirst()){
                            readDb.delete(fileTable.TABLE_NAME,fileTable.FILE_ID +"= '"+ changeFileId+"'",null);
                        }
                        File newFile = changeService.files().get(changeFileId).execute();
                        About about = changeService.about().get().execute();
                        Boolean getTrashed =newFile.getLabels().getTrashed();
                        String parentId = newFile.getParents().get(0).getId();
                        String rootFolderId =about.getRootFolderId();
                        if(parentId.equals(rootFolderId))parentId="root";
                        newValue.put(fileTable.FILE_NAME,newFile.getTitle());
                        newValue.put(fileTable.FILE_ID, changeFileId);
                        newValue.put(fileTable.PARENT_DIRECTORY_OF_FILE,parentId);
                        newValue.put(fileTable.FILE_TYPE,newFile.getMimeType());
                        newValue.put(fileTable.FILE_MODIFY_TIME,newFile.getModifiedByMeDate().toString());

                        if(!getTrashed){
                            readDb.insert(fileTable.TABLE_NAME,null,newValue);
                        }
                        queryId.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent refreshDone = new Intent();
                refreshDone.setAction(GDRIVE_REFRESHED_FRAGMENT);
                refreshDone.putExtra("curFolder",curChooseFolder);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(refreshDone);

            }
        }).start();
    }

/*  get all the changes to the drive files */
        private  List<Change> retrieveAllChange(Drive service) throws IOException{
            List<Change> result = new ArrayList<Change>();

            Changes.List request = service.changes().list();
            request.setIncludeSubscribed(false);
            SharedPreferences appInfo = getActivity().getSharedPreferences(getString(R.string.appInfo),
                    Context.MODE_PRIVATE);
            Long startId = appInfo.getLong(getString(R.string.changeId),0L);

            request.setStartChangeId(startId);
            do{
                try{
                    ChangeList changeList = request.execute();
                    result.addAll(changeList.getItems());
                    request.setPageToken(changeList.getNextPageToken());
             }catch (IOException e){
            request.setPageToken(null);
         }
             }while (request.getPageToken() != null && request.getPageToken().length()>0);

            Long changeId = request.execute().getLargestChangeId();

            SharedPreferences.Editor editor = appInfo.edit();
            editor.putLong(getString(R.string.changeId),changeId).commit();
            return result;
        }

    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        Log.e("","onsaveinstance called");
        state.putStringArrayList("fileName", fileNameList);
        state.putStringArrayList("cachedFileList", cachedFileList);
        state.putStringArrayList("cachedFolderList", cachedFolderList);
    }


    // decide which type of file user clicked,and respond accordingly



    private void downloadFile(String fileId){
        chooseFileId = fileId;
        new Thread(new Runnable(){
            public void run() {
               Drive downloadService = getDriveService();
                try {

                    com.google.api.services.drive.model.File file = downloadService.files().get(chooseFileId).execute();
                    String fileToDownload = file.getTitle();
                    token = mainActivity.m_credential.getToken();
                    url= file.getDownloadUrl();
                    if(url != null && url.length()> 0){
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        request.setTitle(fileToDownload);
                        request.addRequestHeader("Authorization", "Bearer " + token);
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileToDownload);
                        DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                        long id = manager.enqueue(request);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }



    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());

    }



    public class FileDirectoryDbHelper extends SQLiteOpenHelper {
        // If  change the database schema, increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "fileDirectory.db";
        private static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + fileTable.TABLE_NAME + " (" +
                        fileTable._ID + " INTEGER PRIMARY KEY," +
                        fileTable.FILE_NAME + " varchar(255),"  +
                        fileTable.FILE_ID+" varchar(255)," +
                        fileTable.PARENT_DIRECTORY_OF_FILE  + " varchar(255)," +
                        fileTable.FILE_TYPE + " varchar(255)," +
                        fileTable.FILE_MODIFY_TIME + " long"+ ")";

        private static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + fileTable.TABLE_NAME;


        public FileDirectoryDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TABLE);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //db.execSQL(SQL_DELETE_TABLE);
            //onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }


    private  static class viewHolder{
        public  TextView rowText;
    }

    private class  customAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private List<String> content;
        private static final int TYPE_FOLDER = 0;
        private static final int TYPE_FILE = 1;


        public  customAdapter(List<String> objects) {
            mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            content = objects;
        }

        @Override
        public  int getCount() {
            if(content!=null){
                return content.size();
            }else{
                return 0;
            }

        }


        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return content.get(position);
        }

        @Override
        public int getItemViewType(int position) {

           if (!cachedFolderList.get(position).equals("0")) {
                return TYPE_FOLDER;
            } else{
                return TYPE_FILE;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }




        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            viewHolder holder;
            if(convertView == null){
                holder = new viewHolder();
                switch(type){
                    case TYPE_FOLDER:
                        convertView = mInflater.inflate(R.layout.folderlayout, null);
                        holder.rowText = (TextView) convertView.findViewById(R.id.folderTextView);
                        break;
                    case TYPE_FILE:
                        convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
                        holder.rowText  = (TextView) convertView.findViewById(android.R.id.text1);
                        break;
                }
                convertView.setTag(holder);
            }else {
                holder = (viewHolder) convertView.getTag();
            }
            holder.rowText.setText(content.get(position));
            return convertView;
        }
    }

/** set up connection with the drive*/
    private Drive getDriveService(){
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleAccountCredential credential =mainActivity.m_credential;
        Drive mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Drive")
                    .build();
        return mService;
    }





    public class getFileList extends AsyncTask<Void, Void, List<String>> {
        private Exception mLastError = null;
        private List<String> folderArray = new ArrayList<String>();
        private String workingFolderName ;
        private SharedPreferences appInfo = getActivity().getSharedPreferences(getString(R.string.appInfo),
                Context.MODE_PRIVATE);
        private FileDirectoryDbHelper mDbHelper = new FileDirectoryDbHelper(getActivity().getApplicationContext());


        public getFileList(String folderId) {
            workingFolderName =folderId;
        }



        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return retrieveFileList(workingFolderName);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private List<String> retrieveFileList(String folderId) throws IOException {
            List<String> result = new ArrayList<>();
            SQLiteDatabase readDatabase = mDbHelper.getReadableDatabase();
            String[] projection = {
                    fileTable.FILE_NAME,fileTable.FILE_ID,fileTable.FILE_TYPE
            };
            Cursor c = readDatabase.query(fileTable.TABLE_NAME,projection,
                    fileTable.PARENT_DIRECTORY_OF_FILE + "= '"+folderId+"'",
                    null, null, null, null);
            c.moveToFirst();

            long changeId = appInfo.getLong(getString(R.string.changeId),0L);
            // when user open the app for the 1st time, the app need to download data from drive
            if(changeId == 0L) {
                if (c.moveToFirst()) {
                    readDatabase.execSQL("DELETE FROM " + fileTable.TABLE_NAME);
                }
                c.close();
                setRetainInstance(true);
                Drive mService = getDriveService();
                initiateGdrive(mService);
                c = readDatabase.query(fileTable.TABLE_NAME, projection,
                        fileTable.PARENT_DIRECTORY_OF_FILE + "= '" + folderId + "'",
                        null, null, null, null);
                c.moveToFirst();
            } else if(!c.moveToFirst()){
                return null;
            }

            do {
                     String fileNameInSql = c.getString(c.getColumnIndex(fileTable.FILE_NAME));
                     String fileTypeInSql = c.getString(c.getColumnIndex(fileTable.FILE_TYPE));
                     String fileIdInSql =c.getString(c.getColumnIndex(fileTable.FILE_ID));
                     if (fileTypeInSql.equals("application/vnd.google-apps.folder")) {
                         cachedFolderList.add(fileIdInSql);
                         cachedFileList.add("0");

                     }else {
                         cachedFolderList.add("0");
                         cachedFileList.add(fileIdInSql);
                     }
                     result.add(fileNameInSql);


                 } while (c.moveToNext());
                 c.close();
            fileNameList = new ArrayList<>();
            fileNameList = (ArrayList<String>) result;
            SharedPreferences.Editor editor = appInfo.edit();
            Set<String> set = new LinkedHashSet<>();

            set.addAll(fileNameList);
            editor.putStringSet("fileName", set);
            set.addAll(cachedFileList);
            editor.putStringSet("fileList", set);
            set.clear();
            set.addAll(cachedFolderList);
            editor.putStringSet("folderList",set);
            String id= String.valueOf(appInfo.getLong(getString(R.string.changeId),0));
            set.add(id);
    editor.putStringSet("changeId",set);
            editor.apply();
            Log.e("async",appInfo.getStringSet("folderList",null)+"?");
            Log.e("async",cachedFolderList+"");
            return result;

        }




        private void initiateGdrive(Drive service) throws IOException {
            SQLiteDatabase writableDatabase = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            folderArray.add("root");

            for(int i=0;i<folderArray.size();i++){
                String workingFolder = folderArray.get(i);
                FileList files = service.files().list().setQ("'"+workingFolder
                        +"' in parents and trashed = false")
                        .execute();

                for (File file : files.getItems()) {
                    String fileName = file.getTitle();
                    String fileId = file.getId();
                    String fileType = file.getMimeType();
                    DateTime modifyTime = file.getModifiedDate();
                    values.put(fileTable.FILE_NAME, fileName);
                    values.put(fileTable.PARENT_DIRECTORY_OF_FILE, workingFolder);
                    values.put(fileTable.FILE_TYPE, fileType);
                    values.put(fileTable.FILE_ID,fileId);
                    values.put(fileTable.FILE_MODIFY_TIME,modifyTime.getValue());
                    writableDatabase.insert(fileTable.TABLE_NAME, null, values);
                    if (fileType.equals("application/vnd.google-apps.folder")) {
                        folderArray.add(fileId);
                        workingFolderName=fileName;
                    }
                }
            }
           Long startChangeId = service.changes().list().execute().getLargestChangeId();
            Log.e("initiatechangeId",startChangeId+"");
                SharedPreferences.Editor editor = appInfo.edit();
                editor.putLong(getString(R.string.changeId),startChangeId);
                editor.apply();
            setRetainInstance(false);
        }


        @Override
        protected void onPostExecute(List<String> output) {
           // if(isRefreshing())setRefreshing(false);
            customAdapter mAdapter = new customAdapter(output);
            setListAdapter(mAdapter);
            mDbHelper.close();
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {

          if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);

                } else {
                    Log.e("error", "The following error occurred:\n" + mLastError.getMessage());

                }
            } else {
                Log.e("error","Request cancelled.");
            }
        }
    }


    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefresh);
    }



}














