package com.example.maplesunday.storage;

import com.example.maplesunday.model.NoticeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NoticeStateStore {
    private final Path path;
    private final ObjectMapper mapper;
    public NoticeStateStore(@Value("${app.storage.path}") String location, ObjectMapper mapper) {
        path=Path.of(location).toAbsolutePath();this.mapper=mapper;
    }
    public NoticeState load() {
        if(!Files.exists(path)) return NoticeState.empty();
        try {
            var node=mapper.readTree(path.toFile());
            if(node==null || !node.isObject() || !node.has("initialized") ||
                  !node.path("initialized").isBoolean()) throw new IOException("Invalid state schema");
            NoticeState state=mapper.treeToValue(node, NoticeState.class);
            if(state.initialized() && (state.lastProcessedNoticeDate()==null ||
                    state.lastProcessedNoticeId()==null || state.lastProcessedNoticeId().isBlank()))
                throw new IOException("Initialized state is missing cursor");
            return state;
        } catch(Exception e) { throw new IllegalStateException("State is invalid; refusing to send", e); }
    }
    public void save(NoticeState state) {
        Path tmp=null;
        try {
            Files.createDirectories(path.getParent());
            tmp=Files.createTempFile(path.getParent(), "state-", ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(),state);
            try { Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING); }
        } catch(IOException e) { throw new IllegalStateException("Unable to persist state",e); }
        finally { if(tmp!=null) try { Files.deleteIfExists(tmp); } catch(IOException ignored) {} }
    }
}
