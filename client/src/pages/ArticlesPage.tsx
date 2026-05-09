import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { Edit3, Eye, FilePlus2, RefreshCcw, Send, Trash2, UploadCloud } from 'lucide-react';
import { knowledgeApi } from '../api/knowledge';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataState } from '../components/DataState';
import { Modal } from '../components/Modal';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import type {
  ArticleDetail,
  ArticleFormPayload,
  ArticleListItem,
  ArticlePageQuery,
  Category,
  PageResponse,
  Tag
} from '../types';
import { clampText, formatDateTime, fromLocalInputValue, toLocalInputValue } from '../utils/format';
import type { PageProps } from './types';

const emptyPage: PageResponse<ArticleListItem> = {
  pageNum: 1,
  pageSize: 10,
  total: 0,
  records: []
};

type ArticleFormState = ArticleFormPayload;

const blankForm: ArticleFormState = {
  title: '',
  summary: '',
  content: '',
  categoryId: 0,
  author: 'orion',
  tagIds: []
};

export function ArticlesPage({ notify }: PageProps) {
  const [query, setQuery] = useState<ArticlePageQuery>({ pageNum: 1, pageSize: 10, status: '', categoryId: '' });
  const [page, setPage] = useState<PageResponse<ArticleListItem>>(emptyPage);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ArticleListItem | null>(null);
  const [form, setForm] = useState<ArticleFormState>(blankForm);
  const [detail, setDetail] = useState<ArticleDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [confirmTarget, setConfirmTarget] = useState<{ action: 'delete' | 'offline'; article: ArticleListItem } | null>(null);
  const [publishTarget, setPublishTarget] = useState<ArticleListItem | null>(null);
  const [publishTime, setPublishTime] = useState(toLocalInputValue());
  const [busy, setBusy] = useState(false);

  async function loadDictionaries() {
    try {
      const [categoryList, tagList] = await Promise.all([knowledgeApi.listCategories(), knowledgeApi.listTags()]);
      setCategories(categoryList);
      setTags(tagList);
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '分类标签加载失败');
    }
  }

  async function loadArticles() {
    setLoading(true);
    setError('');
    try {
      setPage(await knowledgeApi.listArticles(query));
    } catch (err) {
      const message = err instanceof Error ? err.message : '文章列表加载失败';
      setError(message);
      notify('error', message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDictionaries();
  }, []);

  useEffect(() => {
    void loadArticles();
  }, [query.keyword, query.status, query.categoryId, query.pageNum, query.pageSize]);

  const categoryOptions = useMemo(() => categories.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)), [categories]);

  function openCreate() {
    setEditing(null);
    setForm({ ...blankForm, categoryId: categoryOptions[0]?.id ?? 0 });
    setFormOpen(true);
  }

  async function openEdit(article: ArticleListItem) {
    setBusy(true);
    try {
      const current = await knowledgeApi.getArticle(article.id);
      setEditing(article);
      setForm({
        title: current.title,
        summary: current.summary ?? '',
        content: current.content ?? '',
        categoryId: current.category?.id ?? categoryOptions[0]?.id ?? 0,
        author: current.author ?? 'orion',
        tagIds: current.tags?.map((tag) => tag.id) ?? []
      });
      setFormOpen(true);
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '文章详情加载失败');
    } finally {
      setBusy(false);
    }
  }

  async function openDetail(article: ArticleListItem) {
    setDetailLoading(true);
    try {
      setDetail(await knowledgeApi.getArticle(article.id));
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '文章详情加载失败');
    } finally {
      setDetailLoading(false);
    }
  }

  async function submitForm(event: FormEvent) {
    event.preventDefault();
    if (!form.categoryId) {
      notify('error', '请先选择分类');
      return;
    }
    setBusy(true);
    try {
      if (editing) {
        await knowledgeApi.updateArticle(editing.id, form);
        notify('success', '文章已更新');
      } else {
        await knowledgeApi.createArticle(form);
        notify('success', '文章已创建');
      }
      setFormOpen(false);
      await loadArticles();
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '文章保存失败');
    } finally {
      setBusy(false);
    }
  }

  async function submitPublish() {
    if (!publishTarget) {
      return;
    }
    setBusy(true);
    try {
      await knowledgeApi.publishArticle(publishTarget.id, fromLocalInputValue(publishTime));
      notify('success', '文章已发布');
      setPublishTarget(null);
      await loadArticles();
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '发布失败');
    } finally {
      setBusy(false);
    }
  }

  async function submitConfirm() {
    if (!confirmTarget) {
      return;
    }
    setBusy(true);
    try {
      if (confirmTarget.action === 'delete') {
        await knowledgeApi.deleteArticle(confirmTarget.article.id);
        notify('success', '文章已删除');
      } else {
        await knowledgeApi.offlineArticle(confirmTarget.article.id);
        notify('success', '文章已下线');
      }
      setConfirmTarget(null);
      await loadArticles();
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '操作失败');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page-stack">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Content Source</p>
          <h1>文章管理</h1>
          <p>业务写入只落 MySQL，发布与下线由同步链路投递到 Elasticsearch。</p>
        </div>
        <div className="heading-actions">
          <Button variant="secondary" icon={<RefreshCcw size={16} />} onClick={loadArticles}>
            刷新
          </Button>
          <Button variant="primary" icon={<FilePlus2 size={16} />} onClick={openCreate}>
            新建文章
          </Button>
        </div>
      </header>

      <section className="filter-bar">
        <label>
          关键字
          <input
            value={query.keyword ?? ''}
            placeholder="标题 / 摘要 / 正文"
            onChange={(event) => setQuery((current) => ({ ...current, keyword: event.target.value, pageNum: 1 }))}
          />
        </label>
        <label>
          状态
          <select
            value={query.status ?? ''}
            onChange={(event) =>
              setQuery((current) => ({ ...current, status: event.target.value as ArticlePageQuery['status'], pageNum: 1 }))
            }
          >
            <option value="">全部</option>
            <option value="draft">草稿</option>
            <option value="published">已发布</option>
            <option value="offline">已下线</option>
          </select>
        </label>
        <label>
          分类
          <select
            value={query.categoryId ?? ''}
            onChange={(event) =>
              setQuery((current) => ({
                ...current,
                categoryId: event.target.value ? Number(event.target.value) : '',
                pageNum: 1
              }))
            }
          >
            <option value="">全部</option>
            {categoryOptions.map((category) => (
              <option value={category.id} key={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>
      </section>

      <DataState loading={loading} error={error} empty={page.records.length === 0} emptyText="暂无文章">
        <div className="table-shell">
          <table>
            <thead>
              <tr>
                <th>文章</th>
                <th>分类</th>
                <th>标签</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {page.records.map((article) => (
                <tr key={article.id}>
                  <td className="main-cell">
                    <strong>{article.title}</strong>
                    <span className="subline">{clampText(article.summary || '无摘要', 86)}</span>
                  </td>
                  <td>{article.category?.name ?? '-'}</td>
                  <td>
                    <div className="tag-row">
                      {(article.tags ?? []).slice(0, 3).map((tag) => (
                        <span className="tag-chip" key={tag.id}>
                          {tag.name}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td>
                    <StatusBadge value={article.status} />
                  </td>
                  <td>{formatDateTime(article.updatedAt)}</td>
                  <td>
                    <div className="row-actions">
                      <Button className="icon-only" variant="ghost" icon={<Eye size={16} />} onClick={() => openDetail(article)} aria-label="查看" />
                      <Button className="icon-only" variant="ghost" icon={<Edit3 size={16} />} onClick={() => openEdit(article)} aria-label="编辑" disabled={busy} />
                      <Button
                        className="icon-only"
                        variant="ghost"
                        icon={<Send size={16} />}
                        onClick={() => {
                          setPublishTarget(article);
                          setPublishTime(toLocalInputValue(article.publishTime));
                        }}
                        aria-label="发布"
                      />
                      <Button
                        className="icon-only"
                        variant="ghost"
                        icon={<UploadCloud size={16} />}
                        onClick={() => setConfirmTarget({ action: 'offline', article })}
                        aria-label="下线"
                      />
                      <Button
                        className="icon-only"
                        variant="danger"
                        icon={<Trash2 size={16} />}
                        onClick={() => setConfirmTarget({ action: 'delete', article })}
                        aria-label="删除"
                      />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination
          pageNum={page.pageNum}
          pageSize={page.pageSize}
          total={page.total}
          onChange={(pageNum) => setQuery((current) => ({ ...current, pageNum }))}
        />
      </DataState>

      <Modal
        open={formOpen}
        title={editing ? '编辑文章' : '新建文章'}
        description="保存后由后端写入 MySQL，ES 索引由同步链路维护。"
        onClose={() => setFormOpen(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setFormOpen(false)} disabled={busy}>
              取消
            </Button>
            <Button variant="primary" type="submit" form="article-form" disabled={busy}>
              {busy ? '保存中' : '保存'}
            </Button>
          </>
        }
      >
        <form id="article-form" className="form-grid" onSubmit={submitForm}>
          <label className="span-2">
            标题
            <input value={form.title} required onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} />
          </label>
          <label>
            作者
            <input value={form.author} required onChange={(event) => setForm((current) => ({ ...current, author: event.target.value }))} />
          </label>
          <label>
            分类
            <select
              value={form.categoryId || ''}
              required
              onChange={(event) => setForm((current) => ({ ...current, categoryId: Number(event.target.value) }))}
            >
              <option value="">请选择</option>
              {categoryOptions.map((category) => (
                <option value={category.id} key={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label className="span-2">
            标签
            <select
              multiple
              value={form.tagIds.map(String)}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  tagIds: Array.from(event.target.selectedOptions).map((option) => Number(option.value))
                }))
              }
            >
              {tags.map((tag) => (
                <option value={tag.id} key={tag.id}>
                  {tag.name}
                </option>
              ))}
            </select>
          </label>
          <label className="span-2">
            摘要
            <textarea
              rows={3}
              value={form.summary ?? ''}
              onChange={(event) => setForm((current) => ({ ...current, summary: event.target.value }))}
            />
          </label>
          <label className="span-2">
            正文
            <textarea
              rows={8}
              value={form.content}
              required
              onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
            />
          </label>
          <button className="hidden-submit" type="submit" aria-hidden="true" tabIndex={-1} />
        </form>
      </Modal>

      <Modal
        open={Boolean(detail) || detailLoading}
        title="文章详情"
        onClose={() => setDetail(null)}
        footer={<Button onClick={() => setDetail(null)}>关闭</Button>}
      >
        <DataState loading={detailLoading} empty={!detail}>
          <article className="article-detail">
            <div>
              <h2>{detail?.title}</h2>
              <StatusBadge value={detail?.status} />
            </div>
            <p>{detail?.summary || '无摘要'}</p>
            <dl>
              <dt>作者</dt>
              <dd>{detail?.author}</dd>
              <dt>分类</dt>
              <dd>{detail?.category?.name ?? '-'}</dd>
              <dt>发布时间</dt>
              <dd>{formatDateTime(detail?.publishTime)}</dd>
              <dt>标签</dt>
              <dd>{detail?.tags?.map((tag) => tag.name).join(' / ') || '-'}</dd>
            </dl>
            <pre>{detail?.content}</pre>
          </article>
        </DataState>
      </Modal>

      <Modal
        open={Boolean(publishTarget)}
        title="发布文章"
        description={publishTarget?.title}
        onClose={() => setPublishTarget(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setPublishTarget(null)} disabled={busy}>
              取消
            </Button>
            <Button variant="primary" onClick={submitPublish} disabled={busy}>
              {busy ? '发布中' : '发布'}
            </Button>
          </>
        }
      >
        <label className="single-field">
          发布时间
          <input type="datetime-local" value={publishTime} onChange={(event) => setPublishTime(event.target.value)} />
        </label>
      </Modal>

      <ConfirmDialog
        open={Boolean(confirmTarget)}
        title={confirmTarget?.action === 'delete' ? '删除文章' : '下线文章'}
        message={`确认${confirmTarget?.action === 'delete' ? '删除' : '下线'}「${confirmTarget?.article.title ?? ''}」？`}
        confirmText={confirmTarget?.action === 'delete' ? '删除' : '下线'}
        danger={confirmTarget?.action === 'delete'}
        busy={busy}
        onCancel={() => setConfirmTarget(null)}
        onConfirm={submitConfirm}
      />
    </div>
  );
}
