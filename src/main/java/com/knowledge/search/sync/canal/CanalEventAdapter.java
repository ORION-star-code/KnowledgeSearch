package com.knowledge.search.sync.canal;

public interface CanalEventAdapter {

    SyncEvent adapt(CanalMessage message);
}
