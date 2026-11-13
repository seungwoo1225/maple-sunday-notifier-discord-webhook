package com.example.maplesunday.client;

import java.util.concurrent.Callable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

public final class Retry {
    private Retry() {}
    public static <T> T run(String operation, Callable<T> action) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try { return action.call(); }
            catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if ((status != 429 && status < 500) || attempt == 3)
                    throw new IllegalStateException(operation + " failed HTTP " + status, e);
                sleep(waitMs(e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After"), attempt));
            } catch (ResourceAccessException e) {
                if(attempt == 3) throw new IllegalStateException(operation + " failed after retries", e);
                sleep(attempt * 1500L);
            } catch (Exception e) {
                throw new IllegalStateException(operation + " failed", e);
            }
        }
        throw new IllegalStateException(operation + " retry exhausted");
    }
    static long waitMs(String retryAfter, int attempt) {
        if(retryAfter != null) {
            try { return Math.min(30_000L, Math.max(1000L, (long)(Double.parseDouble(retryAfter) * 1000))); }
            catch(NumberFormatException ignored) {}
        }
        return attempt * 1500L;
    }
    private static void sleep(long millis) {
        try { Thread.sleep(millis); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted", e); }
    }
}
