package com.example.maplesunday.client;

import com.example.maplesunday.model.EventNotice;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NexonNoticeClient {
    private final RestClient client;
    private final String key;
    public NexonNoticeClient(@Value("${app.nexon.base-url}") String baseUrl,
                             @Value("${app.nexon.api-key}") String key) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.key = key;
    }
    public List<EventNotice> fetchEvents() {
        if (key.isBlank()) throw new IllegalStateException("NEXON_API_KEY is not configured");
        JsonNode root = Retry.run("Nexon API", () -> client.get().uri("/maplestory/v1/notice-event")
                .header("x-nxopen-api-key", key).retrieve().body(JsonNode.class));
        if (root == null || !root.path("event_notice").isArray())
            throw new IllegalStateException("Invalid Nexon response: event_notice array missing");
        var result = new ArrayList<EventNotice>();
        for (JsonNode n : root.path("event_notice")) {
            var title = required(n, "title");
            if (!title.replaceAll("\\s+", "").contains("썬데이메이플")) continue;
            var id = required(n, "notice_id");
            var url = required(n, "url");
            if (!url.startsWith("https://maplestory.nexon.com/") &&
                !url.startsWith("https://m.maplestory.nexon.com/"))
                throw new IllegalStateException("Unexpected official notice URL for ID=" + id);
            result.add(new EventNotice(id, title, url, parse(required(n,"date")),
                 parseOptional(n.path("date_event_start").asText(null)),
                 parseOptional(n.path("date_event_end").asText(null))));
        }
        return result;
    }
    private static String required(JsonNode n, String name) {
        var v=n.path(name).asText("");
        if (v.isBlank()) throw new IllegalStateException("Missing field " + name + " in Sunday notice");
        return v;
    }
    private static OffsetDateTime parseOptional(String s) {
        return s == null || s.isBlank() ? null : parse(s);
    }
    public static OffsetDateTime parse(String s) {
        try { return OffsetDateTime.parse(s); }
        catch (DateTimeParseException ignored) {
            try { return LocalDateTime.parse(s).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime(); }
            catch(DateTimeParseException ex) { throw new IllegalStateException("Invalid event date: " + s, ex); }
        }
    }
}
