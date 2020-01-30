package view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mvpmymusic.R;

import org.greenrobot.eventbus.EventBus;

import butterknife.Bind;
import butterknife.ButterKnife;
import presenter.ISearchResultPresenter;
import presenter.SearchResultPresenter;
import service.PlayService;
import view.fragment.SearchResultFragment;

public class SearchResultActivity extends AppCompatActivity {

    private PlayService.PlayBinder playBinder;

    private ServiceConnection playConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {//activity与服务成功绑定时调用
            playBinder = (PlayService.PlayBinder)iBinder;
            System.out.println("SearchResultActivity"+playBinder.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {//activity与服务的连接断开时调用

        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchresult);
        getSupportFragmentManager().beginTransaction().add(R.id.searchresult_container,new SearchResultFragment()).commit();
        //绑定播放服务
        Intent playIntent = new Intent(SearchResultActivity.this,PlayService.class);
        bindService(playIntent,playConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(playConnection);
        Log.i("SearchResultActivity","onDestroy");
    }



}
