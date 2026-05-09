package com.knowledge.search.sync.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.knowledge.search.config.AppCanalProperties;
import com.knowledge.search.service.sync.IncrementalSyncService;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.canal", name = "enabled", havingValue = "true")
public class CanalSyncListener implements SmartLifecycle {

    private static final long EMPTY_BATCH_ID = -1L;
    private static final String SUBSCRIPTION = "knowledge_search\\.(kb_article|kb_category|kb_tag|kb_article_tag)";

    private final AppCanalProperties appCanalProperties;
    private final CanalEventAdapter canalEventAdapter;
    private final IncrementalSyncService incrementalSyncService;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;
    private CanalConnector connector;

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::pollLoop);
        log.info("canal listener started, server={}, destination={}", appCanalProperties.server(), appCanalProperties.destination());
    }

    @Override
    public void stop() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        if (connector != null) {
            connector.disconnect();
        }
        log.info("canal listener stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                ensureConnected();
                Message message = connector.getWithoutAck(100);
                long batchId = message.getId();
                if (batchId == EMPTY_BATCH_ID || message.getEntries().isEmpty()) {
                    sleepQuietly(1000);
                    continue;
                }
                handleEntries(message.getEntries());
                connector.ack(batchId);
            } catch (Exception exception) {
                log.error("canal consume failed, server={}, destination={}", appCanalProperties.server(), appCanalProperties.destination(), exception);
                rollbackQuietly();
                disconnectQuietly();
                sleepQuietly(5000);
            }
        }
    }

    private void ensureConnected() {
        if (connector != null) {
            return;
        }
        InetSocketAddress address = parseAddress(appCanalProperties.server());
        connector = CanalConnectors.newSingleConnector(
                address,
                appCanalProperties.destination(),
                normalizeCredential(appCanalProperties.username()),
                normalizeCredential(appCanalProperties.password()));
        connector.connect();
        connector.subscribe(SUBSCRIPTION);
        connector.rollback();
        log.info("canal connected, server={}, destination={}", appCanalProperties.server(), appCanalProperties.destination());
    }

    private void handleEntries(List<CanalEntry.Entry> entries) throws InvalidProtocolBufferException {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }

            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            if (rowChange.getIsDdl()) {
                continue;
            }

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                CanalMessage message = new CanalMessage(
                        appCanalProperties.destination(),
                        entry.getHeader().getSchemaName(),
                        entry.getHeader().getTableName(),
                        rowChange.getEventType().name(),
                        toColumnMap(rowData.getBeforeColumnsList()),
                        toColumnMap(rowData.getAfterColumnsList()),
                        toOccurredAt(entry.getHeader().getExecuteTime()));
                SyncEvent event = canalEventAdapter.adapt(message);
                if (event == null) {
                    log.debug("ignore canal message, table={}, eventType={}", message.tableName(), message.eventType());
                    continue;
                }
                incrementalSyncService.handle(event);
            }
        }
    }

    private Map<String, Object> toColumnMap(List<CanalEntry.Column> columns) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (CanalEntry.Column column : columns) {
            values.put(column.getName(), column.getIsNull() ? null : column.getValue());
        }
        return values;
    }

    private LocalDateTime toOccurredAt(long executeTime) {
        return executeTime <= 0
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(executeTime), ZoneId.systemDefault());
    }

    private InetSocketAddress parseAddress(String server) {
        if (!StringUtils.hasText(server) || !server.contains(":")) {
            throw new IllegalArgumentException("Invalid canal server: " + server);
        }
        String[] parts = server.split(":", 2);
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    private String normalizeCredential(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void rollbackQuietly() {
        if (connector == null) {
            return;
        }
        try {
            connector.rollback();
        } catch (Exception exception) {
            log.warn("canal rollback failed", exception);
        }
    }

    private void disconnectQuietly() {
        if (connector == null) {
            return;
        }
        try {
            connector.disconnect();
        } catch (Exception exception) {
            log.warn("canal disconnect failed", exception);
        } finally {
            connector = null;
        }
    }
}
