package org.codingmatters.poom.runner.tests;


import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Response;

public class ClientListener extends EventListener {
    @Override
    public void callStart(Call call) {
        super.callStart(call);
        System.out.println("calling : " + call.request().url());
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        super.responseHeadersEnd(call, response);
        System.out.println("response code : " + response.code());
    }
}
