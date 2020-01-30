package view.fragment;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import Util.Constant;
import Util.Currsong;
import adpater.SingerAdapter;
import adpater.SongAdapter;
import bean.ClientApi;
import bean.RecentSong;
import bean.SaveSong;
import bean.SongUrl;
import bean.SongUrlSorted;
import butterknife.Bind;
import butterknife.ButterKnife;
import callback.OnItemClickListener;
import event.LoadFinishedEvent;
import event.PlayingStatusEvent;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import presenter.ISearchResultPresenter;
import presenter.SearchResultPresenter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import service.PlayService;
import view.ISearchResultView;
import view.MainActivity;
import view.PlayActivity;
import view.SearchResultActivity;

import static java.lang.Thread.sleep;

public class SearchResultFragment extends Fragment implements ISearchResultView {
    @Bind(R.id.toolbar_searchresultview)
    Toolbar toolbar;

    @Bind(R.id.singer_recyclerview)
    RecyclerView singerRecyclerView;

    @Bind(R.id.song_recyclerview)
    RecyclerView songRecyclerView;

    @Bind(R.id.albumimage_searchresult)
    ImageView albumimage_searchresult;

    @Bind(R.id.songname_searchresult)
    TextView songname_searchresult;

    @Bind(R.id.singername_searchresult)
    TextView singername_searchresult;

    @Bind(R.id.statuslayout_searchresult)
    RelativeLayout statusLayout;


    SearchResultPresenter searchResultPresenter;//presenter
    PlayService.PlayBinder playBinder;

    String query;//要查询的歌曲/歌手/专辑
    ProgressDialog progressDialog;//显示加载中的进度框

    SingerAdapter singerAdapter;
    SongAdapter songAdapter;

    //singer adapter
    List<String> singers=new ArrayList<>();

    //song adapter
    List<String> songnameList=new ArrayList<>();
    List<List<String>> singerList=new ArrayList<>();//对应每首单曲的歌手
    List<String> albumList=new ArrayList<>();
    List<String> songmidList=new ArrayList<>();
    List<String> albummidList=new ArrayList<>();
    List<SongUrlSorted> songUrlList=new ArrayList<>();
    List<Integer> intervalList=new ArrayList<>();

    //记录SaveSong信息（时长）
    List<Integer> songDuration=new ArrayList<>();
    List<SaveSong> saveSongList=new ArrayList<>();


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
        EventBus.getDefault().register(this);
        Intent playIntent = new Intent(getActivity(),PlayService.class);
        getActivity().bindService(playIntent,playConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view=inflater.inflate(R.layout.fragment_searchresult,container,false);
        ButterKnife.bind(this,view);
        searchResultPresenter=new SearchResultPresenter(this);
        Intent intent=getActivity().getIntent();
        query=intent.getStringExtra("query");
        setHasOptionsMenu(true);
        AppCompatActivity activity=(AppCompatActivity)getActivity();
        activity.setSupportActionBar(toolbar);
        ActionBar actionBar=activity.getSupportActionBar();
        if(actionBar!=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        singerAdapter=new SingerAdapter(getContext(),singers);
        songAdapter=new SongAdapter(getContext(),songnameList,singerList,albumList,songmidList,songUrlList);

        statusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getContext(), PlayActivity.class);
                if(Currsong.getSTATUS()!=Constant.NOTPLAYING){
                    startActivity(intent);
                }
            }
        });

        showDialog();
        loadSongData();
        return view;
    }

    public void loadSongData(){
        searchResultPresenter.getResult(query);
    }

    @Override
    public void showResult(){

        singers=searchResultPresenter.loadSingers();
        songnameList=searchResultPresenter.loadSongnameList();
        singerList=searchResultPresenter.loadSingerList();
        songmidList=searchResultPresenter.loadSongmidList();
        albumList=searchResultPresenter.loadAlbumList();
        songUrlList=searchResultPresenter.loadSongUrlList();
        songDuration=searchResultPresenter.loadSongDuration();
        albummidList=searchResultPresenter.loadAlbummidList();
        intervalList=searchResultPresenter.loadIntervalList();

        LitePal.deleteAll(SaveSong.class);

        Collections.sort(songUrlList, new Comparator<SongUrlSorted>() {
            @Override
            public int compare(SongUrlSorted s1, SongUrlSorted s2) {
                if(s1.getIdx()<s2.getIdx())
                    return -1;
                else if(s1.getIdx()>s2.getIdx())
                    return 1;
                else
                    return 0;
            }
        });

        //将搜索得到的歌曲信息保存下来
        for(int i=0;i<songnameList.size();i++){
            SaveSong ss = new SaveSong();
            ss.setSingers(singerList.get(i));
            ss.setDuration(songDuration.get(i));
            ss.setSongmId(songmidList.get(i));
            ss.setSongName(songnameList.get(i));
            ss.setAlbumName(albumList.get(i));
            ss.setUrl(songUrlList.get(i).getUrl());
            ss.setAlbummid(albummidList.get(i));
            ss.setPosition(i);
            ss.setNeedPay(songUrlList.get(i).isNeedPay());
            ss.setInterval(intervalList.get(i));
            ss.save();
        }

        saveSongList = LitePal.findAll(SaveSong.class);
        singerAdapter=new SingerAdapter(getContext(),singers);

        songAdapter=new SongAdapter(getContext(),songnameList,singerList,albumList,songmidList,songUrlList);
        songAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(int position) {//点击播放歌曲
                SaveSong ss = saveSongList.get(position);
                playBinder.play(ss);

                songname_searchresult.setText(ss.getSongName());
                singername_searchresult.setText(getSingerNames(ss));
                Glide.with(getActivity()).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000" + ss.getAlbummid() + ".jpg").into(albumimage_searchresult);
            }
        });

        LinearLayoutManager llm=new LinearLayoutManager(getActivity());
        llm.setOrientation(RecyclerView.HORIZONTAL);

        singerRecyclerView.setLayoutManager(llm);
        singerRecyclerView.setAdapter(singerAdapter);

        songRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        songRecyclerView.setAdapter(songAdapter);

        if(Currsong.getSTATUS()!=Constant.NOTPLAYING){
            switch(Currsong.getSTATUS()){
                case Constant.STATUS_PLAYINGONLINESONG:
                    songname_searchresult.setText(Currsong.getCurrSaveSong().getSongName());
                    singername_searchresult.setText(getSingerNames(Currsong.getCurrSaveSong()));
                    Glide.with(getActivity()).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000" + Currsong.getCurrSaveSong().getAlbummid() + ".jpg").into(albumimage_searchresult);
                    break;
                case Constant.STATUS_PLAYINGRECENTSONG:
                    songname_searchresult.setText(Currsong.getCurrRecentSong().getSongName());
                    singername_searchresult.setText(getSingerNames(Currsong.getCurrRecentSong()));
                    Glide.with(getActivity()).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000" + Currsong.getCurrRecentSong().getAlbummid() + ".jpg").into(albumimage_searchresult);
                    break;

            }
        }

    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void setStatusBar1(PlayingStatusEvent event) {
        Log.i("SearchResultFragment","setStatusBar1");
        if(Currsong.STATUS==Constant.STATUS_PLAYINGONLINESONG){
            songname_searchresult.setText(Currsong.getCurrSaveSong().getSongName());
            singername_searchresult.setText(getSingerNames(Currsong.getCurrSaveSong()));
            Glide.with(this).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000"+Currsong.getCurrSaveSong().getAlbummid()+".jpg").into(albumimage_searchresult);
        }else if(Currsong.STATUS==Constant.STATUS_PLAYINGRECENTSONG){
            songname_searchresult.setText(Currsong.getCurrRecentSong().getSongName());
            singername_searchresult.setText(getSingerNames(Currsong.getCurrRecentSong()));
            Glide.with(this).load("http://y.gtimg.cn/music/photo_new/T002R180x180M000"+Currsong.getCurrRecentSong().getAlbummid()+".jpg").into(albumimage_searchresult);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                SearchResultActivity activity=(SearchResultActivity)getActivity();
                activity.finish();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void showDialog(){
        progressDialog=new ProgressDialog(getContext());
        progressDialog.setTitle("加载中...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    @Override
    public void hideDialog(){
        progressDialog.dismiss();
    }

    @Override
    public void showFailure(){
        hideDialog();
        Toast.makeText(getContext(),"网络状态不可用，请检查网络连接",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        getActivity().unbindService(playConnection);
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
}

