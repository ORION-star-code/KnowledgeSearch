import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { Edit3, FolderTree, Plus, RefreshCcw, Tag as TagIcon, Trash2 } from 'lucide-react';
import { knowledgeApi } from '../api/knowledge';
import { Button } from '../components/Button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataState } from '../components/DataState';
import { Modal } from '../components/Modal';
import type { Category, Tag } from '../types';
import { formatDateTime } from '../utils/format';
import type { PageProps } from './types';

type EditTarget =
  | { kind: 'category'; item?: Category; name: string; sort: number }
  | { kind: 'tag'; item?: Tag; name: string };

type DeleteTarget = { kind: 'category'; item: Category } | { kind: 'tag'; item: Tag };

export function TaxonomyPage({ notify }: PageProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editTarget, setEditTarget] = useState<EditTarget | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<DeleteTarget | null>(null);
  const [busy, setBusy] = useState(false);

  async function loadTaxonomy() {
    setLoading(true);
    setError('');
    try {
      const [categoryList, tagList] = await Promise.all([knowledgeApi.listCategories(), knowledgeApi.listTags()]);
      setCategories(categoryList);
      setTags(tagList);
    } catch (err) {
      const message = err instanceof Error ? err.message : '分类标签加载失败';
      setError(message);
      notify('error', message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadTaxonomy();
  }, []);

  const sortedCategories = useMemo(() => [...categories].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)), [categories]);

  async function submitEdit(event: FormEvent) {
    event.preventDefault();
    if (!editTarget?.name.trim()) {
      notify('error', '名称不能为空');
      return;
    }
    setBusy(true);
    try {
      if (editTarget.kind === 'category') {
        const payload = { name: editTarget.name.trim(), sort: editTarget.sort || 0 };
        if (editTarget.item) {
          await knowledgeApi.updateCategory(editTarget.item.id, payload);
          notify('success', '分类已更新');
        } else {
          await knowledgeApi.createCategory(payload);
          notify('success', '分类已创建');
        }
      } else if (editTarget.item) {
        await knowledgeApi.updateTag(editTarget.item.id, { name: editTarget.name.trim() });
        notify('success', '标签已更新');
      } else {
        await knowledgeApi.createTag({ name: editTarget.name.trim() });
        notify('success', '标签已创建');
      }
      setEditTarget(null);
      await loadTaxonomy();
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '保存失败');
    } finally {
      setBusy(false);
    }
  }

  async function submitDelete() {
    if (!deleteTarget) {
      return;
    }
    setBusy(true);
    try {
      if (deleteTarget.kind === 'category') {
        await knowledgeApi.deleteCategory(deleteTarget.item.id);
        notify('success', '分类已删除');
      } else {
        await knowledgeApi.deleteTag(deleteTarget.item.id);
        notify('success', '标签已删除');
      }
      setDeleteTarget(null);
      await loadTaxonomy();
    } catch (err) {
      notify('error', err instanceof Error ? err.message : '删除失败');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page-stack">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Taxonomy</p>
          <h1>分类与标签</h1>
          <p>分类和标签会影响文章管理、搜索筛选和 ES 文档重建。</p>
        </div>
        <Button variant="secondary" icon={<RefreshCcw size={16} />} onClick={loadTaxonomy}>
          刷新
        </Button>
      </header>

      <DataState loading={loading} error={error}>
        <section className="two-column">
          <div className="workspace-panel">
            <div className="panel-head">
              <div>
                <h2>分类</h2>
                <p>排序值越小越靠前。</p>
              </div>
              <Button variant="primary" icon={<Plus size={16} />} onClick={() => setEditTarget({ kind: 'category', name: '', sort: 0 })}>
                新建
              </Button>
            </div>
            <div className="stack-list">
              {sortedCategories.map((category) => (
                <div className="list-row" key={category.id}>
                  <FolderTree size={18} />
                  <div>
                    <strong>{category.name}</strong>
                    <span>排序 {category.sort ?? 0} · {formatDateTime(category.updatedAt)}</span>
                  </div>
                  <div className="row-actions">
                    <Button
                      className="icon-only"
                      variant="ghost"
                      icon={<Edit3 size={16} />}
                      aria-label="编辑分类"
                      onClick={() => setEditTarget({ kind: 'category', item: category, name: category.name, sort: category.sort ?? 0 })}
                    />
                    <Button
                      className="icon-only"
                      variant="danger"
                      icon={<Trash2 size={16} />}
                      aria-label="删除分类"
                      onClick={() => setDeleteTarget({ kind: 'category', item: category })}
                    />
                  </div>
                </div>
              ))}
              {sortedCategories.length === 0 ? <div className="muted-empty">暂无分类</div> : null}
            </div>
          </div>

          <div className="workspace-panel">
            <div className="panel-head">
              <div>
                <h2>标签</h2>
                <p>用于文章标注与搜索过滤。</p>
              </div>
              <Button variant="primary" icon={<Plus size={16} />} onClick={() => setEditTarget({ kind: 'tag', name: '' })}>
                新建
              </Button>
            </div>
            <div className="stack-list tag-list">
              {tags.map((tag) => (
                <div className="list-row" key={tag.id}>
                  <TagIcon size={18} />
                  <div>
                    <strong>{tag.name}</strong>
                    <span>{formatDateTime(tag.updatedAt)}</span>
                  </div>
                  <div className="row-actions">
                    <Button
                      className="icon-only"
                      variant="ghost"
                      icon={<Edit3 size={16} />}
                      aria-label="编辑标签"
                      onClick={() => setEditTarget({ kind: 'tag', item: tag, name: tag.name })}
                    />
                    <Button
                      className="icon-only"
                      variant="danger"
                      icon={<Trash2 size={16} />}
                      aria-label="删除标签"
                      onClick={() => setDeleteTarget({ kind: 'tag', item: tag })}
                    />
                  </div>
                </div>
              ))}
              {tags.length === 0 ? <div className="muted-empty">暂无标签</div> : null}
            </div>
          </div>
        </section>
      </DataState>

      <Modal
        open={Boolean(editTarget)}
        title={editTarget?.item ? '编辑' : '新建'}
        onClose={() => setEditTarget(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setEditTarget(null)} disabled={busy}>
              取消
            </Button>
            <Button variant="primary" type="submit" form="taxonomy-form" disabled={busy}>
              {busy ? '保存中' : '保存'}
            </Button>
          </>
        }
      >
        <form id="taxonomy-form" className="form-grid compact-form" onSubmit={submitEdit}>
          <label className="span-2">
            名称
            <input
              value={editTarget?.name ?? ''}
              required
              onChange={(event) =>
                setEditTarget((current) => (current ? { ...current, name: event.target.value } as EditTarget : current))
              }
            />
          </label>
          {editTarget?.kind === 'category' ? (
            <label className="span-2">
              排序值
              <input
                type="number"
                min={0}
                value={editTarget.sort}
                onChange={(event) =>
                  setEditTarget((current) =>
                    current?.kind === 'category' ? { ...current, sort: Number(event.target.value) } : current
                  )
                }
              />
            </label>
          ) : null}
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={deleteTarget?.kind === 'category' ? '删除分类' : '删除标签'}
        message={`确认删除「${deleteTarget?.item.name ?? ''}」？若已被文章使用，后端会阻止删除。`}
        confirmText="删除"
        danger
        busy={busy}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={submitDelete}
      />
    </div>
  );
}
