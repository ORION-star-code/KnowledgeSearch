import { type FormEvent, useEffect, useState } from 'react';
import { RefreshCcw, Search } from 'lucide-react';
import { knowledgeApi } from '../api/knowledge';
import { Button } from '../components/Button';
import { DataState } from '../components/DataState';
import { HighlightedText } from '../components/HighlightedText';
import { Pagination } from '../components/Pagination';
import { StatusBadge } from '../components/StatusBadge';
import type { Category, PageResponse, SearchQuery, SearchResultItem, Tag } from '../types';
import { clampText, formatDateTime } from '../utils/format';
import type { PageProps } from './types';

const emptyResults: PageResponse<SearchResultItem> = {
  pageNum: 1,
  pageSize: 10,
  total: 0,
  records: []
};

export function SearchPage({ notify }: PageProps) {
  const [query, setQuery] = useState<SearchQuery>({
    keyword: '',
    status: 'published',
    sort: 'relevance',
    highlight: true,
    pageNum: 1,
    pageSize: 10,
    categoryId: '',
    tagNames: []
  });
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [page, setPage] = useState<PageResponse<SearchResultItem>>(emptyResults);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function loadDictionaries() {
    try {
      const [categoryList, tagList] = await Promise.all([knowledgeApi.listCategories(), knowledgeApi.listTags()]);
      setCategories(categoryList);
      setTags(tagList);
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '筛选数据加载失败');
    }
  }

  async function runSearch(nextQuery = query) {
    setLoading(true);
    setError('');
    try {
      const response = await knowledgeApi.search(nextQuery);
      setPage(response.page);
    } catch (err) {
      const message = err instanceof Error ? err.message : '搜索请求失败';
      setError(message);
      notify('error', message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadDictionaries();
    void runSearch();
  }, []);

  function submit(event: FormEvent) {
    event.preventDefault();
    const next = { ...query, pageNum: 1 };
    setQuery(next);
    void runSearch(next);
  }

  function changePage(pageNum: number) {
    const next = { ...query, pageNum };
    setQuery(next);
    void runSearch(next);
  }

  return (
    <div className="page-stack">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Search Replica</p>
          <h1>知识检索</h1>
          <p>通过 Elasticsearch 副本验证关键词、多字段权重、筛选条件和高亮片段。</p>
        </div>
        <Button variant="secondary" icon={<RefreshCcw size={16} />} onClick={() => runSearch()}>
          刷新
        </Button>
      </header>

      <form className="search-console" onSubmit={submit}>
        <label className="search-keyword">
          关键词
          <div>
            <Search size={18} />
            <input
              value={query.keyword ?? ''}
              placeholder="Spring Boot / MySQL / Canal"
              onChange={(event) => setQuery((current) => ({ ...current, keyword: event.target.value }))}
            />
          </div>
        </label>
        <div className="filter-bar tight">
          <label>
            分类
            <select
              value={query.categoryId ?? ''}
              onChange={(event) =>
                setQuery((current) => ({ ...current, categoryId: event.target.value ? Number(event.target.value) : '' }))
              }
            >
              <option value="">全部</option>
              {categories.map((category) => (
                <option value={category.id} key={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            标签
            <select
              multiple
              value={query.tagNames ?? []}
              onChange={(event) =>
                setQuery((current) => ({
                  ...current,
                  tagNames: Array.from(event.target.selectedOptions).map((option) => option.value)
                }))
              }
            >
              {tags.map((tag) => (
                <option value={tag.name} key={tag.id}>
                  {tag.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            状态
            <select
              value={query.status ?? ''}
              onChange={(event) => setQuery((current) => ({ ...current, status: event.target.value as SearchQuery['status'] }))}
            >
              <option value="published">已发布</option>
              <option value="">全部</option>
              <option value="draft">草稿</option>
              <option value="offline">已下线</option>
            </select>
          </label>
          <label>
            作者
            <input value={query.author ?? ''} onChange={(event) => setQuery((current) => ({ ...current, author: event.target.value }))} />
          </label>
          <label>
            发布时间起
            <input
              type="datetime-local"
              value={query.publishTimeStart ?? ''}
              onChange={(event) => setQuery((current) => ({ ...current, publishTimeStart: event.target.value }))}
            />
          </label>
          <label>
            排序
            <select
              value={query.sort ?? 'relevance'}
              onChange={(event) => setQuery((current) => ({ ...current, sort: event.target.value as SearchQuery['sort'] }))}
            >
              <option value="relevance">相关度</option>
              <option value="latest">最新发布</option>
            </select>
          </label>
          <label className="toggle-label">
            <input
              type="checkbox"
              checked={query.highlight ?? true}
              onChange={(event) => setQuery((current) => ({ ...current, highlight: event.target.checked }))}
            />
            返回高亮
          </label>
          <Button variant="primary" type="submit" icon={<Search size={16} />}>
            搜索
          </Button>
        </div>
      </form>

      <DataState loading={loading} error={error} empty={page.records.length === 0} emptyText="暂无搜索结果">
        <section className="result-list">
          {page.records.map((item) => (
            <article className="result-item" key={item.id}>
              <div className="result-topline">
                <h2>
                  <HighlightedText text={item.titleHighlight} fallback={item.title} />
                </h2>
                <StatusBadge value={item.status} />
              </div>
              <p>
                <HighlightedText text={item.summaryHighlight} fallback={clampText(item.summary || item.content || '', 160)} />
              </p>
              {item.contentHighlight ? (
                <p className="content-hit">
                  <HighlightedText text={item.contentHighlight} />
                </p>
              ) : null}
              <footer>
                <span>{item.categoryName ?? '-'}</span>
                <span>{item.tagNames?.join(' / ') || '-'}</span>
                <span>{item.author ?? '-'}</span>
                <span>{formatDateTime(item.publishTime || item.updatedAt)}</span>
              </footer>
            </article>
          ))}
        </section>
        <Pagination pageNum={page.pageNum} pageSize={page.pageSize} total={page.total} onChange={changePage} />
      </DataState>
    </div>
  );
}
