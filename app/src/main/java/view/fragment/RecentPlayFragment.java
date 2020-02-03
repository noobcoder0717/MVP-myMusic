package view.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;
import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

import adpater.RecentSongAdapter;
import bean.RecentSong;
import butterknife.Bind;
import butterknife.ButterKnife;
import callback.OnItemClickListener;
import service.PlayService;
import view.MainActivity;

public class RecentPlayFragment extends Fragment {

    RecentSongAdapter adapter;
    List<RecentSong> songList=new ArrayList<>();
    PlayService.PlayBinder playBinder;

    @Bind(R.id.back)
    ImageView back;

    @Bind(R.id.error_frame)
    FrameLayout errorFrame;

    @Bind(R.id.song_relativelayout_recentplay)
    RelativeLayout songRelativelayout;

    @Bind(R.id.song_recyclerview_recentplay)
    RecyclerView songRecyclerView;

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
        Intent playIntent = new Intent(getActivity(),PlayService.class);
        getActivity().bindService(playIntent,playConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view=inflater.inflate(R.layout.fragment_recentplay,container,false);
        ButterKnife.bind(this,view);
        setHasOptionsMenu(true);

        songList= LitePal.findAll(RecentSong.class);
        if(songList.size()!=0){
            adapter=new RecentSongAdapter(songList);
            adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onClick(int position) {
                    RecentSong recentSong=songList.get(position);
                    Log.i("RecentPlayFragment",recentSong.getUrl());
                    playBinder.play(recentSong);
                }
            });
            LinearLayoutManager llm=new LinearLayoutManager(getActivity());
            llm.setOrientation(RecyclerView.VERTICAL);
            songRecyclerView.setAdapter(adapter);
            songRecyclerView.setLayoutManager(llm);
        }
        else{
            songRelativelayout.setVisibility(View.GONE);
            errorFrame.setVisibility(View.VISIBLE);
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity=(MainActivity)getActivity();
                activity.pop();
            }
        });





        return view;
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        getActivity().unbindService(playConnection);
    }
}
