import type { ArticleStatus, SyncFailStatus } from '../types';

export function formatDateTime(value?: string) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ');
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date);
}

export function toLocalInputValue(value?: string) {
  const date = value ? new Date(value) : new Date();
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function fromLocalInputValue(value: string) {
  if (!value) {
    return new Date().toISOString().slice(0, 19);
  }
  return value.length === 16 ? `${value}:00` : value;
}

export function statusText(status?: ArticleStatus) {
  const map: Record<ArticleStatus, string> = {
    draft: '草稿',
    published: '已发布',
    offline: '已下线'
  };
  return status ? (map[status] ?? status) : '-';
}

export function syncStatusText(status?: SyncFailStatus) {
  const map: Record<SyncFailStatus, string> = {
    PENDING: '待重试',
    SUCCESS: '已恢复',
    FAILED: '失败'
  };
  return status ? (map[status] ?? status) : '-';
}

export function clampText(value = '', max = 120) {
  return value.length > max ? `${value.slice(0, max)}...` : value;
}
