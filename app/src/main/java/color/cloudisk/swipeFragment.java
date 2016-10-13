package color.cloudisk;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

/**
 * Created by Dongke on 4/5/2016.
 */
public class swipeFragment extends ListFragment {
    private SwipeRefreshLayout mRefreshLayout;

    @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        final View listFragmentView = super.onCreateView(inflater,container,savedInstanceState);

        mRefreshLayout = new ListFragmentSwipeRefreshLayout(getActivity().getApplicationContext());

        mRefreshLayout.addView(listFragmentView,ViewGroup.LayoutParams.MATCH_PARENT,
                               ViewGroup.LayoutParams.MATCH_PARENT);
        mRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        return mRefreshLayout;
    }

    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener){
        mRefreshLayout.setOnRefreshListener(listener);

    }


    public boolean isRefreshing(){
        return mRefreshLayout.isRefreshing();
       }

    public void setRefreshing(boolean refreshing){
        mRefreshLayout.setRefreshing(refreshing);

    }

    public SwipeRefreshLayout getSwipeRefreshLayout(){

        return  mRefreshLayout;
    }

    private class ListFragmentSwipeRefreshLayout extends  SwipeRefreshLayout{

        public ListFragmentSwipeRefreshLayout(Context context){
            super(context);
        }
        private boolean mMeasured = false;

        @Override
        public void setRefreshing(boolean refreshing) {
           super.setRefreshing(refreshing);
        }

        @Override
        public boolean canChildScrollUp(){

          final ListView listView = getListView();

          if(listView.getVisibility() == View.VISIBLE){
              return ViewCompat.canScrollVertically(listView, -1);

          }else{
              return false;
          }
        }


      @Override
        protected void onDetachedFromWindow(){
          super.onDetachedFromWindow();
            Log.e("","detach from window");
      }

    }





}
