package com.example.maplesunday.model;

import java.time.OffsetDateTime;
/** Cursor and last delivery are distinct: skipped expired notices also advance the cursor. */
public record NoticeState(boolean initialized, String lastProcessedNoticeId,
                          OffsetDateTime lastProcessedNoticeDate, String lastSentNoticeId) {
    public static NoticeState empty() { return new NoticeState(false, null, null, null); }
    public NoticeState processed(EventNotice n, String sentId) {
        return new NoticeState(true, n.id(), n.publishedAt(), sentId);
    }
}
