package com.example.maplesunday.client;

import com.example.maplesunday.model.EventNotice;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DiscordWebhookClient {
    private final RestClient client=RestClient.create();
    private final String url;
    public DiscordWebhookClient(@Value("${app.discord.webhook-url}") String url) { this.url=url; }
    public void send(EventNotice n) {
        if (!url.matches("https://(?:discord\\.com|discordapp\\.com)/api/webhooks/[0-9]+/[^/?#]+(?:\\?.*)?"))
            throw new IllegalStateException("DISCORD_WEBHOOK_URL is missing or invalid");
        var embed = new LinkedHashMap<String,Object>();
        embed.put("title", n.title().length() > 256 ? n.title().substring(0,255)+"…" : n.title());
        embed.put("url", n.url());
        embed.put("description", "🍁 새로운 썬데이 메이플 공지가 등록됐어요!\n공지 등록: "
                + n.publishedAt().atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " (KST)\n자세한 내용은 제목을 눌러 공식 공지를 확인하세요.");
        embed.put("color", 0xF29E38);
        var payload=Map.of("username", "썬데이 메이플 알림", "allowed_mentions", Map.of("parse", List.of()),
                "embeds", List.of(embed));
        String target = url + (url.contains("?") ? "&" : "?") + "wait=true";
        JsonNode response = Retry.run("Discord webhook", () -> client.post().uri(target)
                .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class));
        if (response == null || response.path("id").asText("").isBlank())
            throw new IllegalStateException("Discord did not confirm created message ID");
    }
}
