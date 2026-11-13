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
                public void send(EventNotice n, List<String> images) {
        if (!url.matches(
                "https://(?:discord\\.com|discordapp\\.com)/api/webhooks/[0-9]+/[^/?#]+(?:\\?.*)?"
        )) {
                throw new IllegalStateException("Invalid Discord webhook URL");
        }

        var embeds = new ArrayList<Map<String, Object>>();

        // 첫 번째 Embed: 공지 정보
        var main = new LinkedHashMap<String, Object>();

        main.put("title", n.title());
        main.put("url", n.url());
        main.put("description",
                "🍁 이번 주 썬데이 메이플!\n\n"
                + "자세한 내용은 공지 제목을 클릭하세요."
        );
        main.put("color", 0xF29E38);

        // 첫 번째 이미지는 공지 Embed 안에 표시
        if (!images.isEmpty()) {
                main.put("image", Map.of("url", images.get(0)));
        }

        embeds.add(main);

        // 나머지 이미지는 별도 Embed에 표시
        for (int i = 1; i < Math.min(images.size(), 9); i++) {
                var imageEmbed = new LinkedHashMap<String, Object>();

                imageEmbed.put("color", 0xF29E38);
                imageEmbed.put("image", Map.of("url", images.get(i)));

                embeds.add(imageEmbed);
        }

        var payload = Map.of(
                "allowed_mentions", Map.of("parse", List.of()),
                "embeds", embeds
        );

        String target = url + (url.contains("?") ? "&" : "?") + "wait=true";

        JsonNode response = Retry.run("Discord webhook", () ->
                client.post()
                .uri(target)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class)
        );

        if (response == null ||
                response.path("id").asText("").isBlank()) {
                throw new IllegalStateException("Discord message not confirmed");
        }
}
