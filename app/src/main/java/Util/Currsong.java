package Util;

import bean.LoveSong;
import bean.OnlineSong;
import bean.RecentSong;
import bean.SaveSong;

public class Currsong {
    public static OnlineSong currOnlineSong;
    public static RecentSong currRecentSong;
    public static LoveSong currLoveSong;

    public static LoveSong getCurrLoveSong() {
        return currLoveSong;
    }

    public static void setCurrLoveSong(LoveSong currLoveSong) {
        Currsong.currLoveSong = currLoveSong;
    }

    public static int STATUS=Constant.NOTPLAYING;

    public static OnlineSong getCurrOnlineSong() {
        return currOnlineSong;
    }

    public static void setCurrOnlineSong(OnlineSong currOnlineSong) {
        Currsong.currOnlineSong = currOnlineSong;
    }

    public static RecentSong getCurrRecentSong() {
        return currRecentSong;
    }

    public static void setCurrRecentSong(RecentSong currRecentSong) {
        Currsong.currRecentSong = currRecentSong;
    }

    public static int getSTATUS() {
        return STATUS;
    }

    public static void setSTATUS(int STATUS) {
        Currsong.STATUS = STATUS;
    }
}
