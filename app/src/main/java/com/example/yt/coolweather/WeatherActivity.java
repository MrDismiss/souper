package com.example.yt.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.yt.coolweather.gson.Forecast;
import com.example.yt.coolweather.gson.Weather;
import com.example.yt.coolweather.service.AutoUpdateService;
import com.example.yt.coolweather.util.HttpUtil;
import com.example.yt.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by yt on 17-4-26.
 */

public class WeatherActivity extends AppCompatActivity {

    private ScrollView mWeatherLayout;

    private TextView mCityTitle;

    private TextView mUpdateTime;

    private TextView mDegreeText;

    private TextView mWeatherInfoText;

    private LinearLayout mForecastlayout;

    private TextView mAPIText;

    private TextView mPM25Text;

    private TextView mComfortText;

    private TextView mCarwashText;

    private TextView mSportText;

    private ImageView mBackground;
    private SharedPreferences preferences;
    public SwipeRefreshLayout mRefreshLayout;
    private String weatherId;
    public DrawerLayout mDrawerLayout;
    private Button mNavButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View view = getWindow().getDecorView();
            //状态栏与背景图融为一体   布局中设置 fitsSystemWindows=true   给状态栏留出一定空间
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        mWeatherLayout = (ScrollView) findViewById(R.id.weather_layout);

        mCityTitle = (TextView) findViewById(R.id.title_city);

        mUpdateTime = (TextView) findViewById(R.id.title_update_time);

        mDegreeText = (TextView) findViewById(R.id.degree_text);

        mWeatherInfoText = (TextView) findViewById(R.id.weather_info_text);

        mForecastlayout = (LinearLayout) findViewById(R.id.forecast);

        mAPIText = (TextView) findViewById(R.id.aqi_text);

        mPM25Text = (TextView) findViewById(R.id.pm2_5_text);

        mComfortText = (TextView) findViewById(R.id.comfort_text);

        mCarwashText = (TextView) findViewById(R.id.car_wash_text);

        mSportText = (TextView) findViewById(R.id.sport_text);

        mBackground = (ImageView) findViewById(R.id.image_background);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavButton = (Button) findViewById(R.id.nav_button);

        initRefresh();

        initDrawLayout();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String weatherString = preferences.getString("weather", "");

        if (!TextUtils.isEmpty(weatherString)) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            weatherId = getIntent().getStringExtra("weather_id");
            mWeatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        String background_pic = preferences.getString("background_pic", null);
        if (!TextUtils.isEmpty(background_pic)) {
            Glide.with(this).load(background_pic).into(mBackground);
        } else {
            loadPic();
        }


    }

    private void initDrawLayout() {
        mNavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.START);
            }
        });
    }

    private void initRefresh() {
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent,R.color.colorPrimary);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
    }

    private void loadPic() {
        String url = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bing_pic = response.body().string();
                SharedPreferences.Editor edit = preferences.edit();
                edit.putString("background_pic", bing_pic);
                edit.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bing_pic).into(mBackground);
                    }
                });
            }
        });
    }

    public void requestWeather(final String weatherId) {

        String url = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=638acb50eb2545b480955672e8b59e7f";
        Log.e("GKZ", url);
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "获取天气信息失败onFailure", Toast.LENGTH_LONG).show();
                        mRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                Log.e("GKZ", responseText);
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                            e.putString("weather", responseText);
                            e.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(getApplicationContext(), "获取天气信息失败", Toast.LENGTH_LONG).show();
                        }
                        mRefreshLayout.setRefreshing(false);
                    }
                });

            }
        });
        loadPic();

    }

    private void showWeatherInfo(Weather weather) {

        startService(new Intent(WeatherActivity.this, AutoUpdateService.class));

        String cityName = weather.basic.cityName;

        String updateTime = weather.basic.update.updateTime.split(" ")[1];

        String degree = weather.now.temperature + "℃";

        String weatherInfo = weather.now.more.info;

        mCityTitle.setText(cityName);

        mUpdateTime.setText(updateTime);

        mDegreeText.setText(degree);

        mWeatherInfoText.setText(weatherInfo);

        mForecastlayout.removeAllViews();

        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mForecastlayout, false);

            ((TextView) view.findViewById(R.id.date_text)).setText(forecast.date);
            ((TextView) view.findViewById(R.id.info_text)).setText(forecast.more.info);
            ((TextView) view.findViewById(R.id.max_text)).setText(forecast.temperature.max);
            ((TextView) view.findViewById(R.id.min_text)).setText(forecast.temperature.min);

            mForecastlayout.addView(view);


        }

        if (weather.aqi != null) {
            mAPIText.setText(weather.aqi.city.aqi);
            mPM25Text.setText(weather.aqi.city.pm25);
        }

        mComfortText.setText("舒适度:" + weather.suggestion.comfort.info);
        mCarwashText.setText("洗车指数:" + weather.suggestion.carWash.info);
        mSportText.setText("运动建议:" + weather.suggestion.sport.info);

        mWeatherLayout.setVisibility(View.VISIBLE);
    }
}
