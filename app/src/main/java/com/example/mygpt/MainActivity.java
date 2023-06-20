package com.example.mygpt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView welcomeMsg;
    EditText edMessage;
    ImageButton btnSend;
    List<MessageModel> arrMessage;

    MessageAdapter messageAdapter;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        welcomeMsg = findViewById(R.id.txt_welcome_message);
        edMessage = findViewById(R.id.ed_message);
        btnSend = findViewById(R.id.btn_send);
        arrMessage = new ArrayList<>();

        // Recycler view setup
        messageAdapter = new MessageAdapter(MainActivity.this,arrMessage);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = edMessage.getText().toString().trim();
                if(question.equals("")){
                    Toast.makeText(MainActivity.this,"Please write something",Toast.LENGTH_LONG).show();
                }else {
                    addToChat(question, MessageModel.SENT_BY_ME);
                    edMessage.setText("");
                    callApi(question);
                    welcomeMsg.setVisibility(View.GONE);
                }
            }
        });


    }

    void addToChat(String message,String sentBy){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                arrMessage.add(new MessageModel(message,sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());

            }
        });

    }

    void addToResponse(String response){
        arrMessage.remove(arrMessage.size()-1);
        addToChat(response,MessageModel.SENT_BY_GPT);
    }


    void callApi(String question){

        arrMessage.add(new MessageModel("Typing...",MessageModel.SENT_BY_GPT));

        // we use okhttp library

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model","text-davinci-003");
            jsonBody.put("prompt",question);
            jsonBody.put("max_tokens",4000);
            jsonBody.put("temperature",0);
        }catch (JSONException e){
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization","Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addToResponse("Failed to load response due to "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                if(response.isSuccessful()){
                    JSONObject jsonObject = null;
                    try {
                        assert response.body() != null;
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addToResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }else{
                    assert response.body() != null;
                    addToResponse("Failed to load response due to "+response.body().string());
                }

            }
        });

    }

}