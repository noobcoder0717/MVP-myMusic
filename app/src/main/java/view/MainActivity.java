package view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import Util.Constant;
import Util.Currsong;
import Util.RetrofitFactory;
import bean.ClientApi;
import bean.RecentSong;
import bean.SaveSong;
import butterknife.Bind;
import butterknife.ButterKnife;
import event.PlayingStatusEvent;
import service.PlayService;
import view.fragment.MainFragment;
import view.fragment.RecentPlayFragment;
import view.fragment.SearchFragment;

public class MainActivity extends AppCompatActivity  {
    private MainFragment mainFragment;
    private SearchFragment searchFragment;
    private RecentPlayFragment recentPlayFragment;
    private int FRAGMENTNUMBERS=0;
    private List<Fragment> fragmentList=new ArrayList<>();//管理当前的Fragment

    @Bind(R.id.songname_main)
    TextView songname_main;

    @Bind(R.id.singername_main)
    TextView singername_main;

    @Bind(R.id.albumimage_main)
    ImageView albumimage_main;

    @Bind(R.id.play_pause_main)
    ImageView play_pause;

    @Bind(R.id.statuslayout_main)
    RelativeLayout statusLayout;

    private PlayService.PlayBinder playBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            playBinder = (PlayService.PlayBinder)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity","onCreate");
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        mainFragment=new MainFragment();
        addFragment(mainFragment);//默认显示mainFragment
        showFragment(1);
        Intent intent = new Intent(MainActivity.this,PlayService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);
        ButterKnife.bind(this);


        statusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this, PlayActivity.class);
                if(Currsong.STATUS!=Constant.NOTPLAYING) {
                    startActivity(intent);
                }
            }
        });
    }

    //将fragment添加到fragment回退栈和list中
    public void addFragment(Fragment fragment){
        if(!fragment.isAdded()){
            FRAGMENTNUMBERS++;
            getSupportFragmentManager().beginTransaction().add(R.id.container,fragment,"fragment"+FRAGMENTNUMBERS).addToBackStack(null).commit();
            fragmentList.add(fragment);
        }
    }
    //展示第idx个fragment，非第idx个fragment都hide
    public void showFragment(int idx){
        for(int i=0;i<fragmentList.size();i++){
            if(i+1!=idx)
                getSupportFragmentManager().beginTransaction().hide(fragmentList.get(i)).commit();
        }
        FragmentManager fm=getSupportFragmentManager();
        fm.executePendingTransactions();//添加了这句 findFragmentByTag才不会返回null
        Fragment fragment=fm.findFragmentByTag("fragment"+idx);


        getSupportFragmentManager().beginTransaction().show(fragment).commit();
    }

    public void pop(){
        FRAGMENTNUMBERS--;
        fragmentList.remove(fragmentList.size()-1);
        getSupportFragmentManager().popBackStack();
        getSupportFragmentManager().beginTransaction().commit();
        showFragment(FRAGMENTNUMBERS);
    }

    public void setMainFragment(MainFragment fragment){
        mainFragment=fragment;
    }
    public void setSearchFragment(SearchFragment fragment){
        searchFragment=fragment;
    }
    public void setRecentPlayFragment(RecentPlayFragment fragment){recentPlayFragment=fragment;}
    public MainFragment getMainFragment(){
        return mainFragment;
    }
    public SearchFragment getSearchFragment(){
        return searchFragment;
    }
    public RecentPlayFragment getRecentPlayFragment(){return recentPlayFragment;}
    public int getFRAGMENTNUMBERS(){return FRAGMENTNUMBERS;}


    @Override
    public void onBackPressed(){
        if(getSupportFragmentManager().getBackStackEntryCount()<=1){
            Log.i("MainActivity","onBackPressed,if");
            Log.i("MainActivity",getSupportFragmentManager().getBackStackEntryCount()+"");
            finish();
        }else{
            Log.i("MainActivity","onBackPressed,else");
            getSupportFragmentManager().popBackStack();
            FRAGMENTNUMBERS--;
            fragmentList.remove(FRAGMENTNUMBERS);
            showFragment(FRAGMENTNUMBERS);
            Log.i("MainActivity",getSupportFragmentManager().getBackStackEntryCount()+"");
        }

    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayingStatusEvent(PlayingStatusEvent event){
        refresh();
    }
    @Override
    protected void onResume(){
        super.onResume();
        Log.i("MainActivity","onResume");
        refresh();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(connection);
    }

    public String getSingerNames(SaveSong ss){
        String SingerNames="";
        for(int i=0;i<ss.getSingers().size();i++){
            SingerNames+=ss.getSingers().get(i);
            if(i!=ss.getSingers().size()-1)
                SingerNames+="/";
        }
        return SingerNames;
    }

    public String getSingerNames(RecentSong rs){
        String SingerNames="";
        for(int i=0;i<rs.getSingers().size();i++){
            SingerNames+=rs.getSingers().get(i);
            if(i!=rs.getSingers().size()-1)
                SingerNames+="/";
        }
        return SingerNames;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(){
        switch(Currsong.getSTATUS()){
            case Constant.STATUS_PLAYINGONLINESONG:
                play_pause.setImageResource(R.drawable.pause);
                songname_main.setText(Currsong.getCurrSaveSong().getSongName());
                singername_main.setText(getSingerNames(Currsong.getCurrSaveSong()));
                Glide.with(this).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000"+Currsong.getCurrSaveSong().getAlbummid()+".jpg").into(albumimage_main);
                break;
            case Constant.STATUS_PAUSEONLINESONG:
                play_pause.setImageResource(R.drawable.play);
                songname_main.setText(Currsong.getCurrSaveSong().getSongName());
                singername_main.setText(getSingerNames(Currsong.getCurrSaveSong()));
                Glide.with(this).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000"+Currsong.getCurrSaveSong().getAlbummid()+".jpg").into(albumimage_main);
                break;
            case Constant.STATUS_PLAYINGRECENTSONG:
                play_pause.setImageResource(R.drawable.pause);
                songname_main.setText(Currsong.getCurrRecentSong().getSongName());
                singername_main.setText(getSingerNames(Currsong.getCurrRecentSong()));
                Glide.with(this).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000"+Currsong.getCurrRecentSong().getAlbummid()+".jpg").into(albumimage_main);
                break;
            case Constant.STATUS_PAUSERECENTSONG:
                play_pause.setImageResource(R.drawable.play);
                songname_main.setText(Currsong.getCurrRecentSong().getSongName());
                singername_main.setText(getSingerNames(Currsong.getCurrRecentSong()));
                Glide.with(this).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000"+Currsong.getCurrRecentSong().getAlbummid()+".jpg").into(albumimage_main);
                break;
        }
    }
}
