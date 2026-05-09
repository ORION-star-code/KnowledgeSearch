import { useState } from 'react';
import { BookOpen, DatabaseZap, FileText, LayoutDashboard, Search, Tags } from 'lucide-react';
import { ToastHost } from './components/ToastHost';
import { useToast } from './hooks/useToast';
import { ArticlesPage } from './pages/ArticlesPage';
import { DashboardPage } from './pages/DashboardPage';
import { SearchPage } from './pages/SearchPage';
import { SyncPage } from './pages/SyncPage';
import { TaxonomyPage } from './pages/TaxonomyPage';

type ViewKey = 'dashboard' | 'articles' | 'search' | 'taxonomy' | 'sync';

const navItems: Array<{ key: ViewKey; label: string; desc: string; icon: typeof LayoutDashboard }> = [
  { key: 'dashboard', label: '工作台', desc: '运行概览', icon: LayoutDashboard },
  { key: 'articles', label: '文章', desc: 'CRUD / 发布', icon: FileText },
  { key: 'search', label: '搜索', desc: 'ES 联调', icon: Search },
  { key: 'taxonomy', label: '分类标签', desc: '元数据', icon: Tags },
  { key: 'sync', label: '同步', desc: '补偿重试', icon: DatabaseZap }
];

function App() {
  const [activeView, setActiveView] = useState<ViewKey>('dashboard');
  const { messages, notify } = useToast();
  const active = navItems.find((item) => item.key === activeView) ?? navItems[0];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">
            <BookOpen size={22} />
          </div>
          <div>
            <strong>Knowledge Search</strong>
            <span>CDC Console</span>
          </div>
        </div>
        <nav className="side-nav" aria-label="主导航">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={item.key === activeView ? 'active' : ''}
                key={item.key}
                type="button"
                onClick={() => setActiveView(item.key)}
              >
                <Icon size={18} />
                <span>
                  <strong>{item.label}</strong>
                  <small>{item.desc}</small>
                </span>
              </button>
            );
          })}
        </nav>
        <div className="side-footer">
          <span>API 代理</span>
          <strong>/api → localhost:8080</strong>
        </div>
      </aside>

      <main className="main-shell">
        <header className="topbar">
          <div>
            <span>{active.label}</span>
            <strong>{active.desc}</strong>
          </div>
          <div className="topbar-status">
            <i />
            <span>V1 Canal → Sync Worker → ES</span>
          </div>
        </header>
        <section className="page-surface">
          {activeView === 'dashboard' ? <DashboardPage notify={notify} navigate={(view) => setActiveView(view as ViewKey)} /> : null}
          {activeView === 'articles' ? <ArticlesPage notify={notify} /> : null}
          {activeView === 'search' ? <SearchPage notify={notify} /> : null}
          {activeView === 'taxonomy' ? <TaxonomyPage notify={notify} /> : null}
          {activeView === 'sync' ? <SyncPage notify={notify} /> : null}
        </section>
      </main>

      <ToastHost messages={messages} />
    </div>
  );
}

export default App;
