package com.example.yt.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by yt on 17-4-25.
 */

public class AQI {
    public AQICity city;

    public class AQICity{

        public String aqi;

        public String pm25;
    }
}
