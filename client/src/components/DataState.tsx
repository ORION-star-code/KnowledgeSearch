import type { ReactNode } from 'react';
import { AlertCircle, Inbox } from 'lucide-react';

type DataStateProps = {
  loading?: boolean;
  error?: string;
  empty?: boolean;
  emptyText?: string;
  children: ReactNode;
};

export function DataState({ loading, error, empty, emptyText = '暂无数据', children }: DataStateProps) {
  if (loading) {
    return (
      <div className="data-state">
        <span className="loader" />
        <span>正在加载</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="data-state error-state">
        <AlertCircle size={18} />
        <span>{error}</span>
      </div>
    );
  }

  if (empty) {
    return (
      <div className="data-state">
        <Inbox size={18} />
        <span>{emptyText}</span>
      </div>
    );
  }

  return <>{children}</>;
}
