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
 Copyright 2017 Amit Bhandari AB
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
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

                    Response.Listener<String> listener =  new Response.Listener<String>() {
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
