import { useEffect, useState } from 'react';
import { Play, RefreshCcw, RotateCcw } from 'lucide-react';
import { knowledgeApi } from '../api/knowledge';
import { Button } from '../components/Button';
import { DataState } from '../components/DataState';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import type { PageResponse, SyncFailLog, SyncStats } from '../types';
import { clampText, formatDateTime } from '../utils/format';
import type { PageProps } from './types';

const emptyFailures: PageResponse<SyncFailLog> = {
  pageNum: 1,
  pageSize: 10,
  total: 0,
  records: []
};

const emptyStats: SyncStats = {
  pending: 0,
  retrying: 0,
  failed: 0,
  resolved: 0
};

export function SyncPage({ notify }: PageProps) {
  const [stats, setStats] = useState<SyncStats>(emptyStats);
  const [failures, setFailures] = useState<PageResponse<SyncFailLog>>(emptyFailures);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function loadSync(pageNum = failures.pageNum) {
    setLoading(true);
    setError('');
    try {
      const [nextStats, nextFailures] = await Promise.all([
        knowledgeApi.getSyncStats(),
        knowledgeApi.listFailures(pageNum, failures.pageSize)
      ]);
      setStats(nextStats);
      setFailures(nextFailures);
    } catch (err) {
      const message = err instanceof Error ? err.message : '同步数据加载失败';
      setError(message);
      notify('error', message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadSync(1);
  }, []);

  async function triggerFullSync() {
    setBusy(true);
    try {
      const result = await knowledgeApi.triggerFullSync();
      notify('success', result || '全量同步已完成');
      await loadSync(1);
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '全量同步失败');
    } finally {
      setBusy(false);
    }
  }

  async function retryFailure(id: number) {
    setBusy(true);
    try {
      await knowledgeApi.retryFailure(id);
      notify('success', '失败记录已提交重试');
      await loadSync(failures.pageNum);
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '重试失败');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page-stack">
      <header className="page-heading">
        <div>
          <p className="eyebrow">CDC Compensation</p>
          <h1>同步状态</h1>
          <p>观察 Canal 到 Sync Worker 的失败补偿闭环，并支持人工重试。</p>
        </div>
        <div className="heading-actions">
          <Button variant="secondary" icon={<RefreshCcw size={16} />} onClick={() => loadSync()}>
            刷新
          </Button>
          <Button variant="primary" icon={<Play size={16} />} onClick={triggerFullSync} disabled={busy}>
            {busy ? '执行中' : '全量同步'}
          </Button>
        </div>
      </header>

      <DataState loading={loading} error={error}>
        <section className="metric-grid four">
          <div className="metric-tile passive">
            <span>Pending</span>
            <strong>{stats.pending}</strong>
          </div>
          <div className="metric-tile passive">
            <span>Retrying</span>
            <strong>{stats.retrying}</strong>
          </div>
          <div className="metric-tile passive warn">
            <span>Failed</span>
            <strong>{stats.failed}</strong>
          </div>
          <div className="metric-tile passive">
            <span>Resolved</span>
            <strong>{stats.resolved}</strong>
          </div>
        </section>

        <section className="workspace-panel">
          <div className="panel-head">
            <div>
              <h2>失败日志</h2>
              <p>包含业务类型、业务 ID、载荷和错误信息，便于追踪与补偿。</p>
            </div>
          </div>

          <DataState empty={failures.records.length === 0} emptyText="暂无失败记录">
            <div className="table-shell">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>业务</th>
                    <th>状态</th>
                    <th>错误</th>
                    <th>重试</th>
                    <th>下次重试</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {failures.records.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>
                        <strong>{item.bizType}</strong>
                        <span className="subline">#{item.bizId}</span>
                      </td>
                      <td>
                        <StatusBadge value={item.status} />
                      </td>
                      <td title={item.errorMsg}>{clampText(item.errorMsg || item.payload || '-', 92)}</td>
                      <td>{item.retryCount ?? 0}</td>
                      <td>{formatDateTime(item.nextRetryTime)}</td>
                      <td>
                        <Button
                          variant="ghost"
                          icon={<RotateCcw size={16} />}
                          disabled={busy || item.status === 'SUCCESS'}
                          onClick={() => retryFailure(item.id)}
                        >
                          重试
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination pageNum={failures.pageNum} pageSize={failures.pageSize} total={failures.total} onChange={(page) => loadSync(page)} />
          </DataState>
        </section>
      </DataState>
    </div>
  );
}
