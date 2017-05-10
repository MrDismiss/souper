package com.example.yt.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.yt.coolweather.WeatherActivity;
import com.example.yt.coolweather.gson.Weather;
import com.example.yt.coolweather.util.HttpUtil;
import com.example.yt.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by yt on 17-5-10.
 */

public class AutoUpdateService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updatebackgroundPic();

        //使用AlarmManager 来完成定时任务

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int time = 8 * 60 * 60 * 1000; //8小时
        long triggerAtTime = SystemClock.elapsedRealtime() + time;
        Log.d("gkz",SystemClock.elapsedRealtime()+"");
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        alarmManager.cancel(pi);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);


        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherS = sp.getString("weather", null);
        if (weatherS != null) {
            Weather weather = Utility.handleWeatherResponse(weatherS);
            String weatherId = weather.basic.weatherId;
            String url = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=638acb50eb2545b480955672e8b59e7f";
            Log.e("GKZ", url);
            HttpUtil.sendOkHttpRequest(url, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
//                    Toast.makeText(getApplicationContext(), "获取天气信息失败onFailure", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseText = response.body().string();
                    Log.e("GKZ", responseText);
                    final Weather weather = Utility.handleWeatherResponse(responseText);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        e.putString("weather", responseText);
                        e.apply();
                    } else {
//                        Toast.makeText(getApplicationContext(), "获取天气信息失败", Toast.LENGTH_LONG).show();
                    }
                }

            });
        }

    }

    private void updatebackgroundPic() {
        String url = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bing_pic = response.body().string();
                SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                edit.putString("background_pic", bing_pic);
                edit.apply();
            }
        });
    }
}
