package com.example.maplesunday.model;

import java.time.OffsetDateTime;
public record EventNotice(String id, String title, String url, OffsetDateTime publishedAt,
                          OffsetDateTime startsAt, OffsetDateTime endsAt) {}
