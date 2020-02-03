package view.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.RSRuntimeException;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.security.MessageDigest;
import java.util.List;
import java.util.zip.Inflater;

import adpater.LoveSongAdapter;
import adpater.RecentSongAdapter;
import bean.LoveSong;
import bean.RecentSong;
import butterknife.Bind;
import butterknife.BindInt;
import butterknife.ButterKnife;
import callback.OnItemClickListener;
import event.PlayingStatusEvent;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.internal.FastBlur;
import jp.wasabeef.glide.transformations.internal.RSBlur;
import service.PlayService;
import view.MainActivity;

public class LoveSongFragment extends Fragment {
    List<LoveSong> songList;
    LoveSongAdapter adapter;
    PlayService.PlayBinder playBinder;

    @Bind(R.id.toolbar_lovesong)
    Toolbar toolbar;

    @Bind(R.id.lovesong_recyclerview)
    RecyclerView songRecyclerView;

    @Bind(R.id.lovesong_coordinatorlayout)
    CoordinatorLayout coordinatorLayout;

    @Bind(R.id.lovesong_error_frame)
    FrameLayout errorFrame;

    @Bind(R.id.blur_image)
    ImageView blurImage;

    private ServiceConnection playConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            playBinder=(PlayService.PlayBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Intent playIntent=new Intent(getActivity(),PlayService.class);
        getActivity().bindService(playIntent,playConnection, Context.BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view=inflater.inflate(R.layout.fragment_lovesong,container,false);
        ButterKnife.bind(this,view);
        setHasOptionsMenu(true);

        AppCompatActivity activity=(AppCompatActivity)getActivity();
        activity.setSupportActionBar(toolbar);
        ActionBar actionBar=activity.getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        songList= LitePal.findAll(LoveSong.class);
        if(songList.size()!=0){
            adapter=new LoveSongAdapter(songList);
            adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onClick(int position) {
                    LoveSong loveSong=songList.get(position);
                    Log.i("RecentPlayFragment",loveSong.getUrl());
                    playBinder.play(loveSong);
                }
            });
            LinearLayoutManager llm=new LinearLayoutManager(getActivity());
            llm.setOrientation(RecyclerView.VERTICAL);
            songRecyclerView.setAdapter(adapter);
            songRecyclerView.setLayoutManager(llm);
        }
        else{
            coordinatorLayout.setVisibility(View.GONE);
            errorFrame.setVisibility(View.VISIBLE);
        }

        

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.menu_lovesongfragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                MainActivity activity=(MainActivity)getActivity();
                activity.pop();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshUI(PlayingStatusEvent event){
        List<LoveSong> songList=LitePal.findAll(LoveSong.class);
        adapter=new LoveSongAdapter(songList);
        LinearLayoutManager llm=new LinearLayoutManager(getActivity());
        llm.setOrientation(RecyclerView.VERTICAL);
        songRecyclerView.setAdapter(adapter);
        songRecyclerView.setLayoutManager(llm);
        Log.i("LoveSongFragment",songList.size()+"  "+"refreshUI");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);

    }


}
