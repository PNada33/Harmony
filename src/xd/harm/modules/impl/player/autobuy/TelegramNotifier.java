package xd.harm.modules.impl.player.autobuy;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelegramNotifier {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    
    private String botToken = "";
    private long chatId = 0;
    
    public void setToken(String token) {
        this.botToken = token;
    }
    
    public String getToken() {
        return botToken;
    }
    
    public void setChatId(long id) {
        this.chatId = id;
    }
    
    public long getChatId() {
        return chatId;
    }
    
    public boolean isConfigured() {
        return !botToken.isEmpty();
    }
    
    public void sendTransaction(TransactionLog log) {
        if (!isConfigured()) return;
        
        executor.submit(() -> {
            try {
                String emoji = log.type == TransactionLog.Type.BUY ? "\uD83D\uDCB0" : "\uD83D\uDCB8";
                String action = log.type == TransactionLog.Type.BUY ? "Куплено" : "Продано";
                
                String message = String.format(
                    "%s %s\n\n" +
                    "\uD83D\uDCE6 Предмет: %s\n" +
                    "\uD83D\uDD22 Количество: %d\n" +
                    "\uD83D\uDCB5 Цена: %,d\n" +
                    "\uD83D\uDCC5 Дата: %s\n" +
                    "\uD83C\uDFAE Сервер: %s\n",
                    emoji, action,
                    log.itemName,
                    log.quantity,
                    log.price,
                    log.timestamp.format(formatter),
                    log.server,
                    log.account
                );
                
                if (chatId == 0) {
                    sendMessageToUpdates(message);
                } else {
                    sendMessage(message, chatId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void sendMessageToUpdates(String text) throws Exception {
        String urlString = "https://api.telegram.org/bot" + botToken + "/getUpdates";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String responseStr = response.toString();
        if (responseStr.contains("\"chat\":{\"id\":")) {
            int startIdx = responseStr.indexOf("\"chat\":{\"id\":") + 13;
            int endIdx = responseStr.indexOf(",", startIdx);
            if (endIdx == -1) endIdx = responseStr.indexOf("}", startIdx);
            String chatIdStr = responseStr.substring(startIdx, endIdx);
            chatId = Long.parseLong(chatIdStr.trim());
            sendMessage(text, chatId);
        }
        
        conn.disconnect();
    }
    
    private void sendMessage(String text, long targetChatId) throws Exception {
        String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        String jsonPayload = String.format(
            "{\"chat_id\":%d,\"text\":\"%s\",\"parse_mode\":\"HTML\"}",
            targetChatId,
            text.replace("\"", "\\\"").replace("\n", "\\n")
        );
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            System.err.println("Telegram API error: " + responseCode);
        }
        
        conn.disconnect();
    }
}
