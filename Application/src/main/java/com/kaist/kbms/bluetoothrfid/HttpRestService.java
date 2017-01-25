package com.kaist.kbms.bluetoothrfid;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class HttpRestService {

    public static final String TAG = "HttpRestService";

    public static final String REST_URL = "http://192.168.1.71:1337";

    public interface HttpRestClient {
        @GET("/")
        Call<ResponseBody> getTagUserInfo(@Query("tag") String tagId);
    }
}