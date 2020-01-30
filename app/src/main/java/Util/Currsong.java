package Util;

import bean.RecentSong;
import bean.SaveSong;

public class Currsong {
    public static SaveSong currSaveSong;
    public static RecentSong currRecentSong;
    public static int STATUS=Constant.NOTPLAYING;

    public static int getSTATUS() {
        return STATUS;
    }

    public static void setSTATUS(int STATUS) {
        Currsong.STATUS = STATUS;
    }

    public static SaveSong getCurrSaveSong() {
        return currSaveSong;
    }

    public static void setCurrSaveSong(SaveSong currSaveSong) {
        Currsong.currSaveSong = currSaveSong;
    }

    public static RecentSong getCurrRecentSong() {
        return currRecentSong;
    }

    public static void setCurrRecentSong(RecentSong currRecentSong) {
        Currsong.currRecentSong = currRecentSong;
    }
}
