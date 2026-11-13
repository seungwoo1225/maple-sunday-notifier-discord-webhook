
package com.example.maplesunday.service;

import com.example.maplesunday.client.DiscordWebhookClient;
import com.example.maplesunday.client.NexonNoticeClient;
import com.example.maplesunday.model.EventNotice;
import com.example.maplesunday.model.NoticeState;
import com.example.maplesunday.storage.NoticeStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.mockito.Mockito.*;

class NoticeServiceTest {

    NexonNoticeClient nexon = mock(NexonNoticeClient.class);
    DiscordWebhookClient discord = mock(DiscordWebhookClient.class);
    NoticeStateStore store = mock(NoticeStateStore.class);

    Clock clock = Clock.fixed(
        Instant.parse("2026-11-13T01:00:00Z"),
        ZoneOffset.UTC
    );

    NoticeService service = new NoticeService(
        nexon, discord, store, clock
    );

    private static final List<String> IMAGES =
        List.of("https://example.com/event.png");

    EventNotice notice(String id, String published, String end) {
        return new EventNotice(
            id,
            "[썬데이 메이플] 테스트",
            "https://maplestory.nexon.com/News/Event/123",
            OffsetDateTime.parse(published),
            OffsetDateTime.parse(published),
            end == null ? null : OffsetDateTime.parse(end)
        );
    }

    @BeforeEach
    void fresh() {
        when(store.load()).thenReturn(NoticeState.empty());

        when(nexon.fetchEventImages(anyString()))
            .thenReturn(IMAGES);
    }

    @Test
    void firstRunSendsOnlyNewestActiveNotice() {
        var a = notice("1", "2026-11-12T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");
        var b = notice("2", "2026-11-13T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");

        when(nexon.fetchEvents()).thenReturn(List.of(b, a));

        service.checkOnce();

        verify(nexon).fetchEventImages("2");
        verify(discord).send(b, IMAGES);
        verify(discord, never()).send(eq(a), anyList());

        verify(store).save(
            new NoticeState(true, "2", b.publishedAt(), "2")
        );
    }

    @Test
    void expiredLatestFallsBackToNewestActive() {
        var a = notice("1", "2026-11-12T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");
        var b = notice("2", "2026-11-13T10:00:00+09:00",
            "2026-11-13T09:59:59+09:00");

        when(nexon.fetchEvents()).thenReturn(List.of(a, b));

        service.checkOnce();

        verify(nexon).fetchEventImages("1");
        verify(discord).send(a, IMAGES);

        verify(store).save(
            new NoticeState(true, "2", b.publishedAt(), "1")
        );
    }

    @Test
    void expiredNoticesAdvanceCursorWithoutMessage() {
        var old = notice("1", "2026-11-10T10:00:00+09:00",
            "2026-11-12T00:00:00+09:00");

        when(nexon.fetchEvents()).thenReturn(List.of(old));

        service.checkOnce();

        verifyNoInteractions(discord);
        verify(nexon, never()).fetchEventImages(anyString());

        verify(store).save(
            new NoticeState(true, "1", old.publishedAt(), null)
        );
    }

    @Test
    void sameNoticeIsNotResent() {
        var n = notice("1", "2026-11-12T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");

        when(store.load()).thenReturn(
            new NoticeState(true, "1", n.publishedAt(), "1")
        );
        when(nexon.fetchEvents()).thenReturn(List.of(n));

        service.checkOnce();

        verifyNoInteractions(discord);
        verify(nexon, never()).fetchEventImages(anyString());
        verify(store, never()).save(any());
    }

    @Test
    void webhookFailureNeverAdvancesCursor() {
        var n = notice("1", "2026-11-12T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");

        when(nexon.fetchEvents()).thenReturn(List.of(n));

        doThrow(new IllegalStateException("Webhook failed"))
            .when(discord).send(n, IMAGES);

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            service::checkOnce
        );

        verify(nexon).fetchEventImages("1");
        verify(store, never()).save(any());
    }

    @Test
    void unknownEndDateSkipped() {
        var n = notice("1", "2026-11-12T10:00:00+09:00", null);

        when(nexon.fetchEvents()).thenReturn(List.of(n));

        service.checkOnce();

        verifyNoInteractions(discord);
        verify(nexon, never()).fetchEventImages(anyString());

        verify(store).save(
            new NoticeState(true, "1", n.publishedAt(), null)
        );
    }

    @Test
    void noImagesStillSendsNotice() {
        var n = notice("1", "2026-11-12T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");

        when(nexon.fetchEvents()).thenReturn(List.of(n));
        when(nexon.fetchEventImages("1"))
            .thenReturn(List.of());

        service.checkOnce();

        verify(discord).send(n, List.of());
        verify(store).save(
            new NoticeState(true, "1", n.publishedAt(), "1")
        );
    }

    @Test
    void imageFetchFailureNeverAdvancesCursor() {
        var n = notice("1", "2026-11-12T10:00:00+09:00",
            "2026-11-15T23:59:59+09:00");

        when(nexon.fetchEvents()).thenReturn(List.of(n));

        when(nexon.fetchEventImages("1"))
            .thenThrow(new IllegalStateException("Image fetch failed"));

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            service::checkOnce
        );

        verifyNoInteractions(discord);
        verify(store, never()).save(any());
    }
}
