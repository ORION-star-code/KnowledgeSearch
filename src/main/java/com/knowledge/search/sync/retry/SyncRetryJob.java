package com.knowledge.search.sync.retry;

import com.knowledge.search.config.AppSyncProperties;
import com.knowledge.search.service.sync.SyncFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncRetryJob {

    private final AppSyncProperties appSyncProperties;
    private final SyncFailureService syncFailureService;

    @Scheduled(fixedDelayString = "${app.sync.retry.scan-interval:PT5M}")
    public void scanAndRetry() {
        if (!appSyncProperties.retry().enabled()) {
            return;
        }
        log.info("sync retry scan triggered");
        syncFailureService.retryPendingFailures();
    }
}
