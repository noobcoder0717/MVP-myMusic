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
import bean.LoveSong;
import bean.OnlineSong;
import bean.RecentSong;
import bean.SaveSong;
import event.NeedPayEvent;
import event.PlayingStatusEvent;
import model.ISearchResult;

public class PlayService extends Service {
    private int onlineCurrentPosition;
    private int recentCurrentPosition;
    private int loveCurrentPosition;


    public static MediaPlayer mp = new MediaPlayer();


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
                if(Currsong.getSTATUS()==Constant.STATUS_PLAYINGONLINESONG) {
                    saveRecentSong(Currsong.getCurrOnlineSong());
                }
                if(Currsong.getSTATUS()==Constant.STATUS_PLAYINGLOVESONG){
                    try{
                        LoveSong ls=new LoveSong();
                        ls.setPlaying("no");
                        ls.updateAll("url = ?",Currsong.getCurrLoveSong().getUrl());
                        Log.i("PlayService",Currsong.getCurrLoveSong().getUrl());
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    Log.i("PlayService",Currsong.getCurrLoveSong().getSongName());
                    saveRecentSong(Currsong.getCurrLoveSong());
                }
                playNextSong();
            }
        });

        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if(Currsong.getSTATUS()==Constant.STATUS_PREPAREONLINESONG) {
                    Currsong.setSTATUS(Constant.STATUS_PLAYINGONLINESONG);
                    EventBus.getDefault().post(new PlayingStatusEvent(Currsong.getCurrOnlineSong()));
                }
                if(Currsong.getSTATUS()==Constant.STATUS_PREPARERECENTSONG) {
                    Currsong.setSTATUS(Constant.STATUS_PLAYINGRECENTSONG);
                    EventBus.getDefault().post(new PlayingStatusEvent(Currsong.getCurrRecentSong()));
                }
                if(Currsong.getSTATUS()==Constant.STATUS_PREPARELOVESONG) {
                    Currsong.setSTATUS(Constant.STATUS_PLAYINGLOVESONG);
                    EventBus.getDefault().post(new PlayingStatusEvent(Currsong.getCurrLoveSong()));
                }
                mp.start();
                Log.i("PlayActivity",mp.getDuration()+"");
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.i("PlayService","onError:what "+what+" extra "+extra);
                return true;
            }
        });
        return playBinder;
    }

    public class PlayBinder extends Binder {
        public MediaPlayer getMediaPlayer() {
            Log.i("PlayActivity",mp.getDuration()+"");
            return mp;
        }

        //播放在线歌曲
        public void play(OnlineSong onlineSong){
            LoveSong ls=new LoveSong();
            ls.setPlaying("no");
            ls.updateAll("isPlaying = ?","yes");
            if(!onlineSong.isNeedPay()){
                try{
                    Currsong.setCurrOnlineSong(onlineSong);
                    Currsong.setSTATUS(Constant.STATUS_PREPAREONLINESONG);
                    onlineCurrentPosition=onlineSong.getPosition();
                    mp.reset();//reset不能在release之后调用。
                    mp.setDataSource(onlineSong.getUrl());
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mp.prepareAsync();
                    Log.i("PlayService",onlineSong.getUrl());
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
            LoveSong ls=new LoveSong();
            ls.setPlaying("no");
            ls.updateAll("isPlaying = ?","yes");
            try{
                Log.i("PlayActivity",mp.toString());
                Currsong.setCurrRecentSong(recentSong);
                Currsong.setSTATUS(Constant.STATUS_PREPARERECENTSONG);
                recentCurrentPosition=recentSong.getPosition();
                mp.reset();//reset不能在release之后调用。
                mp.setDataSource(recentSong.getUrl());
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.prepareAsync();
                Log.i("PlayService",recentCurrentPosition+"/"+LitePal.findAll(RecentSong.class).size()+"");
            }catch (IOException e){
                Log.i("PlayService",e.getMessage());
            }
        }

        //播放收藏歌曲
        public void play(LoveSong loveSong){
            try{
                Log.i("PlayActivity",mp.toString());

                //修改歌曲状态
                LoveSong ls=new LoveSong();
                ls.setPlaying("yes");
                ls.updateAll("url = ?",loveSong.getUrl());

                Currsong.setCurrLoveSong(loveSong);
                Currsong.setSTATUS(Constant.STATUS_PREPARELOVESONG);
                loveCurrentPosition=loveSong.getPosition();
                mp.reset();//reset不能在release之后调用。
                mp.setDataSource(loveSong.getUrl());
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.prepareAsync();
                Log.i("PlayService",loveSong.getPosition()+"");
            }catch (IOException e){
                Log.i("PlayService",e.getMessage());
            }
        }
    }

    public void playNextSong() {
        switch(Currsong.getSTATUS()){
            case Constant.STATUS_PLAYINGONLINESONG:
                List<OnlineSong> onlineSongList = LitePal.findAll(OnlineSong.class);
                OnlineSong os;
                if (onlineCurrentPosition == onlineSongList.size() - 1) {
                    os = onlineSongList.get(0);
                    playBinder.play(os);
                } else {
                    os = onlineSongList.get(onlineCurrentPosition + 1);
                    playBinder.play(os);
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

            case Constant.STATUS_PLAYINGLOVESONG:
                List<LoveSong> loveSongList=LitePal.findAll(LoveSong.class);
                LoveSong ls;
                if(loveCurrentPosition==loveSongList.size()-1){
                    ls=loveSongList.get(0);
                    playBinder.play(ls);
                }else{
                    ls=loveSongList.get(loveCurrentPosition+1);
                    playBinder.play(ls);
                }
                Log.i("PlayService","play next love song");
                break;
        }
    }

    public void saveRecentSong(OnlineSong os){
        Log.i("PlayService","saveRecentSong");
        List<RecentSong> songList=LitePal.findAll(RecentSong.class);
        RecentSong recentSong=new RecentSong();
        recentSong.setAlbummid(os.getAlbummid());
        recentSong.setAlbumName(os.getAlbumName());
        recentSong.setNeedPay(os.isNeedPay());
        recentSong.setSingers(os.getSingers());
        recentSong.setSongName(os.getSongName());
        recentSong.setUrl(os.getUrl());
        recentSong.setSongmId(os.getSongmId());
        recentSong.setInterval(os.getInterval());
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
    public void saveRecentSong(LoveSong ls){
        Log.i("PlayService","saveRecentSong");
        List<RecentSong> songList=LitePal.findAll(RecentSong.class);
        RecentSong recentSong=new RecentSong();
        recentSong.setAlbummid(ls.getAlbummid());
        recentSong.setAlbumName(ls.getAlbumName());
        recentSong.setNeedPay(ls.isNeedPay());
        recentSong.setSingers(ls.getSingers());
        recentSong.setSongName(ls.getSongName());
        recentSong.setUrl(ls.getUrl());
        recentSong.setSongmId(ls.getSongmId());
        recentSong.setInterval(ls.getInterval());
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
