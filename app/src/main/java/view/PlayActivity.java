package view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.LitePal;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import Util.Constant;
import Util.Currsong;
import bean.RecentSong;
import bean.SaveSong;
import butterknife.Bind;
import butterknife.ButterKnife;
import event.PlayingStatusEvent;
import service.PlayService;

public class PlayActivity extends AppCompatActivity implements IPlayView{

    private PlayService.PlayBinder playBinder;
    MediaPlayer mp=new MediaPlayer();
    Thread seekbarThread;

    @Bind(R.id.back)
    ImageView back;

    @Bind(R.id.play_pause_playactivity)
    ImageView play_pause;

    @Bind(R.id.last_song)
    ImageView last_song;

    @Bind(R.id.next_song)
    ImageView next_song;

    @Bind(R.id.songname_activityplay)
    TextView songname;

    @Bind(R.id.singername_activityplay)
    TextView singername;

    @Bind(R.id.seekbar)
    SeekBar seekBar;

    @Bind(R.id.current_progress)
    TextView currentProgress;

    @Bind(R.id.songlength)
    TextView songLength;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        Intent intent = new Intent(PlayActivity.this,PlayService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);

        currentProgress.setText(MinuteAndSecond(PlayService.mp.getCurrentPosition()/1000));
        seekBar.setMax(PlayService.mp.getDuration());
        seekBar.setProgress(PlayService.mp.getCurrentPosition());
        init();
        setOnclick();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {//activity与服务成功绑定时调用
            playBinder = (PlayService.PlayBinder)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {//activity与服务的连接断开时调用
        }
    };

    public void init(){
        switch(Currsong.getSTATUS()){
            case Constant.STATUS_PLAYINGONLINESONG:
                songname.setText(Currsong.getCurrSaveSong().getSongName());
                singername.setText(getSingerNames(Currsong.getCurrSaveSong()));
                songLength.setText(MinuteAndSecond(Currsong.getCurrSaveSong().getInterval()));
                play_pause.setImageResource(R.drawable.pause_64);
                break;
            case Constant.STATUS_PLAYINGRECENTSONG:
                songname.setText(Currsong.getCurrRecentSong().getSongName());
                singername.setText(getSingerNames(Currsong.getCurrRecentSong()));
                songLength.setText(MinuteAndSecond(Currsong.getCurrRecentSong().getInterval()));
                play_pause.setImageResource(R.drawable.pause_64);
                break;
        }
        if(Currsong.getSTATUS()!=Constant.NOTPLAYING){
            mp=PlayService.mp;
            seekBar.setMax(mp.getDuration());
            getProgress();
        }
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(connection);
        EventBus.getDefault().unregister(this);
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

    //输入歌曲的总秒数，返回xx：xx的格式（xx分xx秒）
    public String MinuteAndSecond(int interval){
        int minutes=interval/60;
        int seconds=interval%60;
        String res="";
        if(minutes<10){
            res+='0';
            res+=minutes;
        }else{
            res+=minutes;
        }
        res+=':';
        if(seconds<10){
            res+='0';
            res+=seconds;
        }else{
            res+=seconds;
        }
        return res;
    }


    //切换歌曲时更新UI
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshUI(PlayingStatusEvent event) {
        init();
        getProgress();
    }

    //根据播放状态更新进度条
    public void getProgress(){
        seekbarThread=new Thread(new Runnable() {
            @Override
            public void run() {
                while (Currsong.getSTATUS() == Constant.STATUS_PLAYINGONLINESONG || Currsong.getSTATUS() == Constant.STATUS_PLAYINGRECENTSONG) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    handler.sendEmptyMessage(0);
                }
            }
        });
        seekbarThread.start();
    }

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message message){//更新UI，当前播放时间
            currentProgress.setText(MinuteAndSecond(mp.getCurrentPosition()/1000));
            seekBar.setProgress(mp.getCurrentPosition());
        }
    };


    public void setOnclick(){
        //进度条的拖动事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean drag) {
                if(drag){//如果用户拖动了进度条,更新播放的位置
                    mp.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //播放暂停、切歌
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(Currsong.getSTATUS()){
                    case Constant.STATUS_PLAYINGONLINESONG:
                        play_pause.setImageResource(R.drawable.play_64);
                        PlayService.mp.pause();
                        Currsong.setSTATUS(Constant.STATUS_PAUSEONLINESONG);
                        break;
                    case Constant.STATUS_PLAYINGRECENTSONG:
                        play_pause.setImageResource(R.drawable.play_64);
                        PlayService.mp.pause();
                        Currsong.setSTATUS(Constant.STATUS_PAUSERECENTSONG);
                        break;
                    case Constant.STATUS_PAUSEONLINESONG:
                        play_pause.setImageResource(R.drawable.pause_64);
                        PlayService.mp.start();
                        Currsong.setSTATUS(Constant.STATUS_PLAYINGONLINESONG);
                        getProgress();
                        break;
                    case Constant.STATUS_PAUSERECENTSONG:
                        play_pause.setImageResource(R.drawable.pause_64);
                        PlayService.mp.start();
                        Currsong.setSTATUS(Constant.STATUS_PLAYINGRECENTSONG);
                        getProgress();
                        break;
                }
            }
        });

        last_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayService.mp.stop();
                PlayService.mp.reset();
                List<SaveSong> saveSongList=LitePal.findAll(SaveSong.class);
                List<RecentSong> recentSongList=LitePal.findAll(RecentSong.class);
                int position;
                switch (Currsong.getSTATUS()){
                    case Constant.STATUS_PLAYINGONLINESONG:
                        position=Currsong.getCurrSaveSong().getPosition()-1;
                        if(position<0)
                            position= saveSongList.size()-1;
                        playBinder.play(saveSongList.get(position));
                        break;
                    case Constant.STATUS_PAUSEONLINESONG:
                        position=Currsong.getCurrSaveSong().getPosition()-1;
                        if(position<0)
                            position= saveSongList.size()-1;
                        playBinder.play(saveSongList.get(position));
                        break;
                    case Constant.STATUS_PLAYINGRECENTSONG:
                        position=Currsong.getCurrRecentSong().getPosition()-1;
                        if(position<0)
                            position= recentSongList.size()-1;
                        playBinder.play(recentSongList.get(position));
                        break;
                    case Constant.STATUS_PAUSERECENTSONG:
                        position=Currsong.getCurrRecentSong().getPosition()-1;
                        if(position<0)
                            position= recentSongList.size()-1;
                        playBinder.play(recentSongList.get(position));
                        break;
                }

            }
        });

        next_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayService.mp.stop();
                PlayService.mp.reset();
                List<SaveSong> saveSongList=LitePal.findAll(SaveSong.class);
                List<RecentSong> recentSongList=LitePal.findAll(RecentSong.class);
                int position;
                switch (Currsong.getSTATUS()){
                    case Constant.STATUS_PLAYINGONLINESONG:
                        position=Currsong.getCurrSaveSong().getPosition()+1;
                        if(position==saveSongList.size())
                            position= 0;
                        playBinder.play(saveSongList.get(position));
                        break;
                    case Constant.STATUS_PAUSEONLINESONG:
                        position=Currsong.getCurrSaveSong().getPosition()+1;
                        if(position==saveSongList.size())
                            position= 0;
                        playBinder.play(saveSongList.get(position));
                        break;
                    case Constant.STATUS_PLAYINGRECENTSONG:
                        position=Currsong.getCurrRecentSong().getPosition()+1;
                        if(position==recentSongList.size())
                            position= 0;
                        playBinder.play(recentSongList.get(position));
                        break;
                    case Constant.STATUS_PAUSERECENTSONG:
                        position=Currsong.getCurrRecentSong().getPosition()+1;
                        if(position==recentSongList.size())
                            position= 0;
                        playBinder.play(recentSongList.get(position));
                        break;
                }
            }
        });
    }

}
