export type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
};

export type PageResponse<T> = {
  pageNum: number;
  pageSize: number;
  total: number;
  records: T[];
};

export type ArticleStatus = 'draft' | 'published' | 'offline';

export type Category = {
  id: number;
  name: string;
  sort?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type Tag = {
  id: number;
  name: string;
  createdAt?: string;
  updatedAt?: string;
};

export type ArticleListItem = {
  id: number;
  title: string;
  summary?: string;
  author: string;
  status: ArticleStatus;
  publishTime?: string;
  createdAt?: string;
  updatedAt?: string;
  category?: Pick<Category, 'id' | 'name'>;
  tags?: Pick<Tag, 'id' | 'name'>[];
};

export type ArticleDetail = ArticleListItem & {
  content: string;
};

export type ArticleFormPayload = {
  title: string;
  summary?: string;
  content: string;
  categoryId: number;
  author: string;
  tagIds: number[];
};

export type ArticlePageQuery = {
  keyword?: string;
  status?: ArticleStatus | '';
  categoryId?: number | '';
  pageNum?: number;
  pageSize?: number;
};

export type SearchQuery = {
  keyword?: string;
  categoryId?: number | '';
  tagNames?: string[];
  status?: ArticleStatus | '';
  author?: string;
  publishTimeStart?: string;
  publishTimeEnd?: string;
  updatedTimeStart?: string;
  updatedTimeEnd?: string;
  pageNum?: number;
  pageSize?: number;
  sort?: 'relevance' | 'latest';
  highlight?: boolean;
};

export type SearchResultItem = {
  id: number;
  title: string;
  titleHighlight?: string;
  summary?: string;
  summaryHighlight?: string;
  content?: string;
  contentHighlight?: string;
  categoryId?: number;
  categoryName?: string;
  tagNames?: string[];
  author?: string;
  status?: ArticleStatus;
  publishTime?: string;
  updatedAt?: string;
};

export type SearchResponse = {
  page: PageResponse<SearchResultItem>;
};

export type SyncStats = {
  pending: number;
  retrying: number;
  failed: number;
  resolved: number;
};

export type SyncFailStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

export type SyncFailLog = {
  id: number;
  bizType: string;
  bizId: number;
  payload?: string;
  errorMsg?: string;
  retryCount?: number;
  status: SyncFailStatus;
  nextRetryTime?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type CategoryPayload = {
  name: string;
  sort?: number;
};

export type TagPayload = {
  name: string;
};
