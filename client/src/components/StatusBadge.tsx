import type { ArticleStatus, SyncFailStatus } from '../types';
import { statusText, syncStatusText } from '../utils/format';

type StatusBadgeProps = {
  value?: ArticleStatus | SyncFailStatus | string;
};

export function StatusBadge({ value }: StatusBadgeProps) {
  const normalized = value ?? 'unknown';
  const label = normalized === normalized.toUpperCase()
    ? syncStatusText(normalized as SyncFailStatus)
    : statusText(normalized as ArticleStatus);

  return <span className={`status-badge status-${String(normalized).toLowerCase()}`}>{label}</span>;
}
