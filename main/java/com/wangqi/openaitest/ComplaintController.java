package com.wangqi.openaitest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/complaint")
public class ComplaintController {

    @Value("${openai.api.key}")
    private String API_KEY;

    @Value("${openai.base.url}")
    private String BASE_URL;

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private String initialComplaint;
    private List<JsonObject> conversationHistory;

    @GetMapping("/start")
    public String startComplaintSimulation() throws IOException {
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

        initialComplaint = complaints[new Random().nextInt(complaints.length)];

        conversationHistory = new ArrayList<>();
        conversationHistory.add(createMessage("system", "你是一个难缠的顾客，对餐厅员工进行投诉。"));
        conversationHistory.add(createMessage("user", "您好，欢迎光临。请问有什么可以帮您的？"));
        conversationHistory.add(createMessage("assistant", initialComplaint));

        return initialComplaint;
    }

    private JsonObject createMessage(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    @PostMapping("/response")
    public String getResponse(@RequestBody String employeeResponse) throws IOException {
        conversationHistory.add(createMessage("user", employeeResponse));

        String customerResponse = getGPT4Response(conversationHistory);
        conversationHistory.add(createMessage("assistant", customerResponse));

        return customerResponse;
    }

    private String getGPT4Response(List<JsonObject> conversationHistory) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.add("messages", gson.toJsonTree(conversationHistory));
        requestBody.addProperty("max_tokens", 150);
        requestBody.addProperty("n", 1);
        requestBody.addProperty("temperature", 0.7);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json"))).header("Authorization", "Bearer " + API_KEY)
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

    @GetMapping("/feedback")
    public String getFeedback() throws IOException {
        conversationHistory.add(createMessage("system", "请根据以上对话给出基于 LAST 原则的评分，并提供改进建议。"));

        String feedback = getGPT4Feedback(conversationHistory);
        return feedback;
    }

    private String getGPT4Feedback(List<JsonObject> conversationHistory) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4o-mini");
        requestBody.add("messages", gson.toJsonTree(conversationHistory));
        requestBody.addProperty("max_tokens", 600);
        requestBody.addProperty("n", 1);
        requestBody.addProperty("temperature", 0.7);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json"))).header("Authorization", "Bearer " + API_KEY)
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
}
