package service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.List;

import Util.Constant;
import Util.Currsong;
import bean.RecentSong;
import bean.SaveSong;
import event.NeedPayEvent;
import event.PlayingStatusEvent;
import model.ISearchResult;

public class PlayService extends Service {
    private int onlineCurrentPosition;
    private int recentCurrentPosition;


    private MediaPlayer mp = new MediaPlayer();


    private PlayBinder playBinder=new PlayBinder();
    public PlayService() {}

    @Override
    public void onCreate(){//第一次创建服务时调用
        super.onCreate();
        System.out.println("playService onCreate");
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){//每次服务启动时调用
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy(){//所有绑定该服务的都unbind了的时候调用
        super.onDestroy();
        System.out.println("playService onDestroy");
        mp.stop();
        mp.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i("PlayService","oncompletion");
                if(Currsong.getSTATUS()==Constant.STATUS_PLAYINGONLINESONG) {
                    saveRecentSong(Currsong.getCurrSaveSong());
                }
                playNextSong();
            }
        });

        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mp.start();
                Log.i("PlayActivity",mp.getDuration()+"");
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.i("PlayService","onError");
                return true;
            }
        });
        return playBinder;
    }

    public class PlayBinder extends Binder {
        public MediaPlayer getMp() {
            return mp;
        }

        //播放在线歌曲
        public void play(SaveSong saveSong){
            if(!saveSong.isNeedPay()){
                try{
                    Currsong.setCurrSaveSong(saveSong);
                    Currsong.setSTATUS(Constant.STATUS_PLAYINGONLINESONG);
                    onlineCurrentPosition=saveSong.getPosition();
                    mp.reset();//reset不能在release之后调用。
                    mp.setDataSource(saveSong.getUrl());
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.prepareAsync();
                    Log.i("PlayService",saveSong.getUrl());

                    EventBus.getDefault().post(new PlayingStatusEvent(saveSong));
                }catch (IOException e){
                    Log.i("PlayService",e.getMessage());
                }
            }else{
                Toast.makeText(getApplicationContext(),"暂未获得该歌曲版权，换首歌吧！",Toast.LENGTH_SHORT).show();
                onlineCurrentPosition++;
                playNextSong();
            }
        }

        //播放最近播放歌曲
        public void play(RecentSong recentSong){
            try{
                Log.i("PlayActivity",mp.toString());
                Currsong.setCurrRecentSong(recentSong);
                Currsong.setSTATUS(Constant.STATUS_PLAYINGRECENTSONG);
                recentCurrentPosition=recentSong.getPosition();
                mp.reset();//reset不能在release之后调用。
                mp.setDataSource(recentSong.getUrl());
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.prepareAsync();
                EventBus.getDefault().post(new PlayingStatusEvent(recentSong));
            }catch (IOException e){
                Log.i("PlayService",e.getMessage());
            }
        }
    }

    public void playNextSong() {
        switch(Currsong.getSTATUS()){
            case Constant.STATUS_PLAYINGONLINESONG:
                List<SaveSong> SaveSongList = LitePal.findAll(SaveSong.class);
                SaveSong ss;
                if (onlineCurrentPosition == SaveSongList.size() - 1) {
                    ss = SaveSongList.get(0);
                    playBinder.play(ss);
                } else {
                    ss = SaveSongList.get(onlineCurrentPosition + 1);
                    playBinder.play(ss);
                }
                break;

            case Constant.STATUS_PLAYINGRECENTSONG:
                List<RecentSong> recentSongList=LitePal.findAll(RecentSong.class);
                RecentSong rs;
                if(recentCurrentPosition==recentSongList.size()-1){
                    rs=recentSongList.get(0);
                    playBinder.play(rs);
                }else{
                    rs=recentSongList.get(recentCurrentPosition+1);
                    playBinder.play(rs);
                }
                break;
        }
    }

    public void saveRecentSong(SaveSong ss){
        Log.i("PlayService","saveRecentSong");
        List<RecentSong> songList=LitePal.findAll(RecentSong.class);
        RecentSong recentSong=new RecentSong();
        recentSong.setAlbummid(ss.getAlbummid());
        recentSong.setAlbumName(ss.getAlbumName());
        recentSong.setNeedPay(ss.isNeedPay());
        recentSong.setSingers(ss.getSingers());
        recentSong.setSongName(ss.getSongName());
        recentSong.setUrl(ss.getUrl());
        recentSong.setSongmId(ss.getSongmId());
        recentSong.setInterval(ss.getInterval());
        if(!AlreadyExist(recentSong)){
            Log.i("PlayService","song don't exist");
            recentSong.setPosition(songList.size());
            recentSong.save();
        }
        Log.i("PlayService",LitePal.findAll(RecentSong.class).size()+"");
//        else{//如果这首歌已存在于最近播放列表中，则将其移至最近播放列表的最顶端
//
//        }
    }

    public Boolean AlreadyExist(RecentSong recentSong){
        List<RecentSong> recentSongList=LitePal.findAll(RecentSong.class);
        if(recentSongList.size()==0)
            return false;
        else{
            for(RecentSong rs:recentSongList) {
                if (recentSong.getUrl().equals(rs.getUrl()))//用url来判断两首歌是否相同
                    return true;
            }
            return false;
        }
    }


}
