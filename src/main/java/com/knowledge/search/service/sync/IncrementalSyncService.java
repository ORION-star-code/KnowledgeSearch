package com.knowledge.search.service.sync;

import com.knowledge.search.sync.canal.SyncEvent;

public interface IncrementalSyncService {

    void handle(SyncEvent event);
}
