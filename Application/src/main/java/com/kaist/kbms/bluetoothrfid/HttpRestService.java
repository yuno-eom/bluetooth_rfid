package com.kaist.kbms.bluetoothrfid;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class HttpRestService {

    public static final String TAG = "HttpRestService";

    public static final String REST_URL = "http://192.168.1.71:1337";

    public interface HttpRestClient {
        @GET("/")
        Call<ResponseBody> getTagUserInfo(@Query("tag") String tagId);
    }

    /* -> BluetoothFragment
    public static String[] requestTagInfo(final String tagId) {
        final String[] returnRequest = {""};

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(REST_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HttpRestService.HttpRestClient client = retrofit.create(HttpRestService.HttpRestClient.class);
        Call<ResponseBody> call = client.getTagUserInfo(tagId);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "response: " + responseBody);
                    if(TextUtils.isEmpty(responseBody)) responseBody = "{ \"tagId\": \"" + tagId + "\" }";
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        returnRequest[0] = json.getString("tagId") + " | " + json.getString("bicId") + " | " + json.getString("userId");
                        Log.d(TAG, "return:" + returnRequest[0]);
                    } catch (JSONException e) {
                        Log.e(TAG, "jsonobject error", e);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "response error", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Rest failure", t);
            }
        });

        return returnRequest;
    }
    */
}