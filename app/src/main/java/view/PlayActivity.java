package view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

        Intent intent = new Intent(PlayActivity.this,PlayService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);

        init();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
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
                seekBar.setMax(Currsong.getCurrSaveSong().getInterval());
                songLength.setText(MinuteAndSecond(Currsong.getCurrSaveSong().getInterval()));
                break;
            case Constant.STATUS_PLAYINGRECENTSONG:
                songname.setText(Currsong.getCurrRecentSong().getSongName());
                singername.setText(getSingerNames(Currsong.getCurrRecentSong()));
                seekBar.setMax(Currsong.getCurrRecentSong().getInterval());
                songLength.setText(MinuteAndSecond(Currsong.getCurrRecentSong().getInterval()));
                break;
        }
//        MediaPlayer mp=playBinder.getMp();
//        Log.i("PlayActivity",mp.getDuration()+"");


    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(connection);
//        EventBus.getDefault().unregister(this);
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
        }
        return res;

    }

}
