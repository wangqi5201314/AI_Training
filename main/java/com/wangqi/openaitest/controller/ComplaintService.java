package com.wangqi.openaitest.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class ComplaintService {
    private static final String API_KEY = "sk-wVYhlDdnKtXkCVXqDCeKkyGQn3QqoKURjpwGKhTIw1aD1bkP";
    private static final String BASE_URL = "https://api.chatanywhere.tech/v1/chat/completions";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    // 生成随机投诉
    public String generateRandomComplaint() {
        String[] complaints = {
                "你们餐厅怎么回事？我刚才在你们这里买的餐点里面居然有虫卵！太恶心了！你们怎么解释？",
                "你们的服务太差了！我等了半小时才上菜，这还算是服务吗？",
                "你们的食物不新鲜，我吃了一口就觉得不对劲，现在肚子很不舒服！",
                "你们的餐厅环境太脏了，桌子上还有食物残渣，怎么吃饭？",
                "你们的服务员态度非常恶劣，完全没有礼貌！",
                "我点的外卖居然送错了，怎么办？",
                "你们的饮料里居然有一根头发，太恶心了！",
                "我经过餐区过道时摔倒在地，你们的地面太滑了，谁来负责？"
        };
        return complaints[new Random().nextInt(complaints.length)];
    }

    // 初始化对话
    public List<JSONObject> initConversation(String initialComplaint) {
        List<JSONObject> conversationHistory = new ArrayList<>();
        conversationHistory.add(createMessage("system", "你是一个难缠的顾客，对餐厅员工进行投诉。"));
        conversationHistory.add(createMessage("user", "您好，欢迎光临。请问有什么可以帮您的？"));
        conversationHistory.add(createMessage("assistant", initialComplaint));

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.add("messages", gson.toJsonTree(conversationHistory));
        requestBody.addProperty("max_tokens", 10000);
        requestBody.addProperty("n", 1);
        requestBody.addProperty("temperature", 0.7);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + API_KEY)
                .build();

        return conversationHistory;
    }

    // 处理回复
    public List<JSONObject> processReply(List<JSONObject> history, String message) {
        // 在此可以使用 sessionId 存储历史记录或进行其他处理（示例中未实现具体的session管理）。
        history.add(createMessage("user", message));
        return history;
    }

    // 异步获取GPT-4的响应
    @Async
    public String getGPT4Response(List<JSONObject> conversationHistory) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.add("messages", gson.toJsonTree(conversationHistory));
        requestBody.addProperty("max_tokens", 10000);
        requestBody.addProperty("n", 1);
        requestBody.addProperty("temperature", 0.7);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
                return responseBody.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim();
            } else {
                throw new IOException("Unexpected code " + response);
            }
        }
    }

    // 异步获取GPT-4的反馈
    @Async
    public String getGPT4Feedback(List<JSONObject> conversationHistory) throws IOException {
        conversationHistory.add(createMessage("system", "请根据以上对话给出基于 LAST 原则的评分，并提供改进建议。"));

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.add("messages", gson.toJsonTree(conversationHistory));
        requestBody.addProperty("max_tokens", 10000);
        requestBody.addProperty("n", 1);
        requestBody.addProperty("temperature", 0.7);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .header("Authorization", "Bearer " + API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
                return responseBody.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim();
            } else {
                throw new IOException("Unexpected code " + response);
            }
        }
    }

    private JSONObject createMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
