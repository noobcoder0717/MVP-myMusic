package event;

import java.util.List;

import bean.RecentSong;
import bean.SaveSong;

public class PlayingStatusEvent {

    private SaveSong ss;
    private RecentSong rs;



    public SaveSong getSs() {
        return ss;
    }

    public void setSs(SaveSong ss) {
        this.ss = ss;
    }

    public RecentSong getRecentSong() {
        return rs;
    }

    public void setRecentSong(RecentSong rs) {
        this.rs = rs;
    }

    public PlayingStatusEvent(SaveSong ss) {
        this.ss = ss;
    }
    public PlayingStatusEvent(RecentSong rs){
        this.rs=rs;
    }

    public SaveSong getSaveSong() {
        return ss;
    }

}