import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ArrowRight, Database, FileText, RefreshCcw, Search, Tags } from 'lucide-react';
import { knowledgeApi } from '../api/knowledge';
import { Button } from '../components/Button';
import { DataState } from '../components/DataState';
import { StatusBadge } from '../components/StatusBadge';
import type { ArticleListItem, Category, SyncStats, Tag } from '../types';
import { formatDateTime } from '../utils/format';
import type { PageProps } from './types';

type DashboardData = {
  articles: ArticleListItem[];
  categories: Category[];
  tags: Tag[];
  syncStats: SyncStats;
  totalArticles: number;
};

export function DashboardPage({ navigate, notify }: PageProps) {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadDashboard() {
    setLoading(true);
    setError('');
    try {
      const [articlesPage, categories, tags, syncStats] = await Promise.all([
        knowledgeApi.listArticles({ pageNum: 1, pageSize: 100 }),
        knowledgeApi.listCategories(),
        knowledgeApi.listTags(),
        knowledgeApi.getSyncStats()
      ]);
      setData({
        articles: articlesPage.records,
        categories,
        tags,
        syncStats,
        totalArticles: articlesPage.total
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : '工作台数据加载失败';
      setError(message);
      notify('error', message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDashboard();
  }, []);

  const articleStats = useMemo(() => {
    const source = data?.articles ?? [];
    return {
      draft: source.filter((item) => item.status === 'draft').length,
      published: source.filter((item) => item.status === 'published').length,
      offline: source.filter((item) => item.status === 'offline').length
    };
  }, [data]);

  return (
    <div className="page-stack">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Knowledge Operations</p>
          <h1>知识库运营工作台</h1>
          <p>文章治理、检索联调与 CDC 同步补偿入口。</p>
        </div>
        <Button variant="secondary" icon={<RefreshCcw size={16} />} onClick={loadDashboard}>
          刷新
        </Button>
      </header>

      <DataState loading={loading} error={error}>
        <section className="metric-grid">
          <button className="metric-tile" onClick={() => navigate?.('articles')}>
            <FileText size={20} />
            <span>文章总量</span>
            <strong>{data?.totalArticles ?? 0}</strong>
          </button>
          <button className="metric-tile" onClick={() => navigate?.('articles')}>
            <Database size={20} />
            <span>已发布</span>
            <strong>{articleStats.published}</strong>
          </button>
          <button className="metric-tile" onClick={() => navigate?.('taxonomy')}>
            <Tags size={20} />
            <span>分类 / 标签</span>
            <strong>
              {data?.categories.length ?? 0} / {data?.tags.length ?? 0}
            </strong>
          </button>
          <button className="metric-tile warn" onClick={() => navigate?.('sync')}>
            <AlertCircle size={20} />
            <span>待处理同步</span>
            <strong>{(data?.syncStats.pending ?? 0) + (data?.syncStats.failed ?? 0)}</strong>
          </button>
        </section>

        <section className="workspace-grid">
          <div className="workspace-panel wide-panel">
            <div className="panel-head">
              <div>
                <h2>最近文章</h2>
                <p>来自 MySQL 管理接口，发布后可通过同步写入 ES。</p>
              </div>
              <Button variant="ghost" icon={<ArrowRight size={16} />} onClick={() => navigate?.('articles')}>
                进入文章
              </Button>
            </div>
            <div className="table-shell compact">
              <table>
                <thead>
                  <tr>
                    <th>标题</th>
                    <th>分类</th>
                    <th>状态</th>
                    <th>更新时间</th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.articles ?? []).slice(0, 6).map((article) => (
                    <tr key={article.id}>
                      <td>
                        <strong>{article.title}</strong>
                        <span className="subline">{article.author}</span>
                      </td>
                      <td>{article.category?.name ?? '-'}</td>
                      <td>
                        <StatusBadge value={article.status} />
                      </td>
                      <td>{formatDateTime(article.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="workspace-panel">
            <div className="panel-head">
              <div>
                <h2>状态分布</h2>
                <p>按列表接口当前可见数据聚合。</p>
              </div>
            </div>
            <div className="status-bars">
              <div>
                <span>草稿</span>
                <strong>{articleStats.draft}</strong>
                <i style={{ width: `${Math.max(8, articleStats.draft * 18)}%` }} />
              </div>
              <div>
                <span>已发布</span>
                <strong>{articleStats.published}</strong>
                <i style={{ width: `${Math.max(8, articleStats.published * 18)}%` }} />
              </div>
              <div>
                <span>已下线</span>
                <strong>{articleStats.offline}</strong>
                <i style={{ width: `${Math.max(8, articleStats.offline * 18)}%` }} />
              </div>
            </div>
          </div>

          <div className="workspace-panel">
            <div className="panel-head">
              <div>
                <h2>检索联调</h2>
                <p>搜索接口读取 Elasticsearch 副本。</p>
              </div>
            </div>
            <button className="search-jump" onClick={() => navigate?.('search')}>
              <Search size={22} />
              <span>打开搜索页验证关键词、筛选与高亮</span>
              <ArrowRight size={18} />
            </button>
          </div>

          <div className="workspace-panel">
            <div className="panel-head">
              <div>
                <h2>同步补偿</h2>
                <p>失败记录需可见、可重试。</p>
              </div>
            </div>
            <div className="sync-mini">
              <span>Pending</span>
              <strong>{data?.syncStats.pending ?? 0}</strong>
              <span>Retrying</span>
              <strong>{data?.syncStats.retrying ?? 0}</strong>
              <span>Failed</span>
              <strong>{data?.syncStats.failed ?? 0}</strong>
              <span>Resolved</span>
              <strong>{data?.syncStats.resolved ?? 0}</strong>
            </div>
          </div>
        </section>
      </DataState>
    </div>
  );
}
