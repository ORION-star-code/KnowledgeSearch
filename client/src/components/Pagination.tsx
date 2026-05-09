import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

type PaginationProps = {
  pageNum: number;
  pageSize: number;
  total: number;
  onChange: (pageNum: number) => void;
};

export function Pagination({ pageNum, pageSize, total, onChange }: PaginationProps) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  return (
    <div className="pagination">
      <span>
        第 {pageNum} / {pages} 页，共 {total} 条
      </span>
      <div className="pagination-actions">
        <Button
          variant="ghost"
          icon={<ChevronLeft size={16} />}
          disabled={pageNum <= 1}
          onClick={() => onChange(pageNum - 1)}
        >
          上一页
        </Button>
        <Button
          variant="ghost"
          icon={<ChevronRight size={16} />}
          disabled={pageNum >= pages}
          onClick={() => onChange(pageNum + 1)}
        >
          下一页
        </Button>
      </div>
    </div>
  );
}
