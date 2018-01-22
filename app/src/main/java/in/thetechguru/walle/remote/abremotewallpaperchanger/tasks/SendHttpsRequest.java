package in.thetechguru.walle.remote.abremotewallpaperchanger.tasks;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

import in.thetechguru.walle.remote.abremotewallpaperchanger.MyApp;
import in.thetechguru.walle.remote.abremotewallpaperchanger.helpers.FirebaseUtil;
import in.thetechguru.walle.remote.abremotewallpaperchanger.model.HttpsRequestPayload;

/**
 * Created by abami on 1/18/2018.
 * call firebase function to invoke FCM on server
 * @todo error handler for retrying in case request does not go through
 */

public class SendHttpsRequest extends Thread {

        private static String baseUrl = "https://us-central1-walle-73480.cloudfunctions.net/notifyRequest";

        public SendHttpsRequest(HttpsRequestPayload payload){
            super(getRunnable(payload));
        }

        private static Runnable getRunnable(final HttpsRequestPayload payload){
            return new Runnable() {
                @Override
                public void run() {

                    RequestQueue queue = Volley.newRequestQueue(MyApp.getContext());

                    Response.Listener<String> listener = new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("volley", "onResponse: "+ response);
                        }
                    };

                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            VolleyLog.d("volley", "Error: " + error.getMessage());
                        }
                    };

                    StringRequest jsonObjRequest = new StringRequest(Request.Method.POST,
                            baseUrl,listener, errorListener) {
                        @Override
                        public String getBodyContentType() {
                            return "application/x-www-form-urlencoded; charset=UTF-8";
                        }

                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            Map<String, String> params = new HashMap<String, String>();

                            params.put("from", payload.fromUserName);
                            params.put("to", payload.toUserName);
                            params.put("status", payload.statusCode);
                            if(payload.wallpaperUrl!=null){
                                params.put("id", payload.wallpaperUrl);
                            }
                            return params;
                        }
                    };

                    queue.add(jsonObjRequest);
                }
            };
        }
}
