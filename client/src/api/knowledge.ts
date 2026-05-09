import { apiClient } from './client';
import type {
  ArticleDetail,
  ArticleFormPayload,
  ArticleListItem,
  ArticlePageQuery,
  Category,
  CategoryPayload,
  PageResponse,
  SearchQuery,
  SearchResponse,
  SyncFailLog,
  SyncStats,
  Tag,
  TagPayload
} from '../types';

export const knowledgeApi = {
  listArticles(query: ArticlePageQuery) {
    return apiClient.get<PageResponse<ArticleListItem>>('/admin/article', query);
  },
  getArticle(id: number) {
    return apiClient.get<ArticleDetail>(`/admin/article/${id}`);
  },
  createArticle(payload: ArticleFormPayload) {
    return apiClient.post<number>('/admin/article', payload);
  },
  updateArticle(id: number, payload: ArticleFormPayload) {
    return apiClient.put<void>(`/admin/article/${id}`, payload);
  },
  deleteArticle(id: number) {
    return apiClient.delete<void>(`/admin/article/${id}`);
  },
  publishArticle(id: number, publishTime: string) {
    return apiClient.put<void>(`/admin/article/${id}/publish`, { publishTime });
  },
  offlineArticle(id: number) {
    return apiClient.put<void>(`/admin/article/${id}/offline`);
  },
  listCategories() {
    return apiClient.get<Category[]>('/admin/category');
  },
  createCategory(payload: CategoryPayload) {
    return apiClient.post<number>('/admin/category', payload);
  },
  updateCategory(id: number, payload: CategoryPayload) {
    return apiClient.put<void>(`/admin/category/${id}`, payload);
  },
  deleteCategory(id: number) {
    return apiClient.delete<void>(`/admin/category/${id}`);
  },
  listTags() {
    return apiClient.get<Tag[]>('/admin/tag');
  },
  createTag(payload: TagPayload) {
    return apiClient.post<number>('/admin/tag', payload);
  },
  updateTag(id: number, payload: TagPayload) {
    return apiClient.put<void>(`/admin/tag/${id}`, payload);
  },
  deleteTag(id: number) {
    return apiClient.delete<void>(`/admin/tag/${id}`);
  },
  search(query: SearchQuery) {
    return apiClient.get<SearchResponse>('/search', query);
  },
  getSyncStats() {
    return apiClient.get<SyncStats>('/admin/sync/stats');
  },
  triggerFullSync() {
    return apiClient.post<string>('/admin/sync/full');
  },
  listFailures(pageNum: number, pageSize: number) {
    return apiClient.get<PageResponse<SyncFailLog>>('/admin/sync/fail/list', { pageNum, pageSize });
  },
  retryFailure(failId: number) {
    return apiClient.post<void>(`/admin/sync/retry/${failId}`);
  }
};
