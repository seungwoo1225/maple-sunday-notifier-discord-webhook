package com.example.maplesunday.service;

import com.example.maplesunday.client.DiscordWebhookClient;
import com.example.maplesunday.client.NexonNoticeClient;
import com.example.maplesunday.model.EventNotice;
import com.example.maplesunday.model.NoticeState;
import com.example.maplesunday.storage.NoticeStateStore;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {
    private static final Logger log=LoggerFactory.getLogger(NoticeService.class);
    private static final Comparator<EventNotice> ORDER=Comparator.comparing(EventNotice::publishedAt)
            .thenComparing(EventNotice::id);
    private final NexonNoticeClient nexon;
    private final DiscordWebhookClient discord;
    private final NoticeStateStore store;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public NoticeService(NexonNoticeClient n, DiscordWebhookClient d, NoticeStateStore s) {
        this(n,d,s,Clock.systemUTC());
    }
    public NoticeService(NexonNoticeClient n, DiscordWebhookClient d, NoticeStateStore s, Clock clock) {
        this.nexon=n;this.discord=d;this.store=s;this.clock=clock;
    }
    public void checkOnce() {
        NoticeState state=store.load();
        List<EventNotice> notices=nexon.fetchEvents().stream().sorted(ORDER).toList();
        if(notices.isEmpty()) { log.info("No Sunday notices found");return; }
        EventNotice latest=notices.getLast();
        List<EventNotice> fresh=notices.stream().filter(n -> isNew(n,state)).toList();
        if(fresh.isEmpty()) { log.info("No new Sunday notices");return; }
        var now=clock.instant();
        // Missing end timestamp is not assumed valid; skip it with an explicit warning.
        for(EventNotice n: fresh) if(n.endsAt()==null)
            log.warn("Skipping notice with absent end date: {}", n.id());
        EventNotice selected=fresh.stream().filter(n -> n.endsAt()!=null && now.isBefore(n.endsAt().toInstant()))
                .max(ORDER).orElse(null);
        String sent=state.lastSentNoticeId();
        if (selected != null) {
            List<String> images = nexon.fetchEventImages(selected.id());
            log.info("Found {} event images", images.size());
            discord.send(selected, images);
            sent = selected.id();
            log.info("Discord delivered notice ID={}", selected.id());
        } else log.info("All new Sunday notices expired or have no event end date");
        // Advance beyond all notices observed, even those we intentionally skipped.
        store.save(state.processed(latest,sent));
        log.info("Saved cursor notice ID={}", latest.id());
    }
    private boolean isNew(EventNotice n, NoticeState state) {
        if(!state.initialized()) return true;
        int dateCompare=n.publishedAt().compareTo(state.lastProcessedNoticeDate());
        return dateCompare>0 || dateCompare==0 && n.id().compareTo(state.lastProcessedNoticeId())>0;
    }
}
