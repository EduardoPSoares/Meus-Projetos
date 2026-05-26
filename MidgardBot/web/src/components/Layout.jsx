import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import {
  LayoutDashboard,
  ScrollText,
  Shield,
  Ticket,
  Search,
  LogOut,
  Menu,
  X,
  ChevronRight,
  FileText,
  Sun,
  Moon
} from 'lucide-react'
import { useState, useEffect } from 'react'
import { SwordIcon, DiamondIcon, GrassBlockIcon } from '../assets/minecraft-icons'
import OnlineUsers from './OnlineUsers'
import LogsWidget from './LogsWidget'
import RoleOverrideWidget from './RoleOverrideWidget'
import { usePresence } from '../hooks/usePresence'

const ADMIN_ROLES = ['CEOO', 'MODERADOR', 'DEV']

const NAV_ITEMS = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard', mcIcon: null, roles: ADMIN_ROLES },
  { to: '/whitelists', icon: ScrollText, label: 'Whitelists', mcIcon: null },
  { to: '/moderation', icon: Shield, label: 'Moderação', mcIcon: null, roles: ADMIN_ROLES },
  { to: '/tickets', icon: Ticket, label: 'Tickets', mcIcon: null },
  { to: '/staff', icon: null, label: 'Staff', mcIcon: SwordIcon },
  { to: '/players', icon: Search, label: 'Jogadores', mcIcon: null },
  { to: '/reports', icon: FileText, label: 'Relatórios', mcIcon: null },
]

export default function Layout({ children }) {
  const { user, logout, hasRole } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(false)
  usePresence()

  useEffect(() => {
    const check = () => setIsMobile(window.innerWidth < 1024)
    check()
    window.addEventListener('resize', check)
    return () => window.removeEventListener('resize', check)
  }, [])

  // Fechar sidebar ao navegar no mobile
  useEffect(() => {
    if (isMobile) setSidebarOpen(false)
  }, [location.pathname])

  const pageTitle = NAV_ITEMS.find(i => i.to === location.pathname)?.label || 'Midgard'

  return (
    <div className="min-h-screen flex bg-shadow">
      {/* Sidebar */}
      <aside className={`
        fixed inset-y-0 left-0 z-50 w-[260px] bg-parchment border-r border-border
        flex flex-col h-screen overflow-hidden
        transform transition-transform duration-300 ease-in-out
        lg:sticky lg:top-0 lg:translate-x-0
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        {/* Logo / Brand */}
        <div className="shrink-0 p-5 border-b border-border mc-border-top">
          <div className="flex items-center gap-3 pt-1">
            <div className="w-10 h-10 rounded-lg bg-gold/15 border border-gold/30 flex items-center justify-center animate-pulse-glow">
              <GrassBlockIcon size={22} />
            </div>
            <div>
              <h1 className="text-gold font-bold text-lg leading-tight tracking-wide">Midgard</h1>
              <p className="text-text-muted text-[10px] uppercase tracking-widest">Admin Panel</p>
            </div>
          </div>
        </div>

        {/* Navigation — scrollable */}
        <nav className="flex-1 min-h-0 overflow-y-auto p-3 space-y-0.5">
          <p className="text-text-muted text-[10px] uppercase tracking-widest px-3 pt-2 pb-2">Navegação</p>
          {NAV_ITEMS.filter(item => !item.roles || hasRole(...item.roles)).map(({ to, icon: Icon, label, mcIcon: McIcon }, navIdx) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) => `
                flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium
                transition-all duration-200 group relative animate-slide-right
                ${isActive
                  ? 'bg-gold/10 text-gold border border-gold/20 shadow-[inset_0_1px_0_rgba(200,168,78,0.1)]'
                  : 'text-text-secondary hover:text-text-primary hover:bg-parchment-light border border-transparent'
                }
              `}
              style={{ animationDelay: `${navIdx * 40}ms` }}
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <div className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-gold rounded-r-full" />
                  )}
                  {McIcon ? (
                    <McIcon size={18} className={`transition-transform group-hover:scale-110 ${isActive ? 'opacity-100' : 'opacity-60 group-hover:opacity-100'}`} />
                  ) : (
                    <Icon size={18} className={`transition-transform group-hover:scale-110 ${isActive ? '' : 'opacity-60 group-hover:opacity-100'}`} />
                  )}
                  <span>{label}</span>
                  {isActive && <ChevronRight size={14} className="ml-auto opacity-50" />}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Bottom section — always visible, pinned to bottom */}
        <div className="shrink-0">
          <OnlineUsers />

          <div className="p-3 border-t border-border">
            <div className="flex items-center gap-3 p-2 rounded-lg bg-parchment-light/50">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="" className="w-9 h-9 rounded-lg border border-border" />
              ) : (
                <div className="w-9 h-9 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center">
                  <span className="text-text-muted text-sm">?</span>
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-text-primary truncate">{user?.username}</p>
                <p className="text-[10px] text-text-muted uppercase tracking-wide">Staff</p>
              </div>
            </div>
            <button
              onClick={logout}
              className="flex items-center gap-2 w-full px-3 py-2 mt-2 rounded-lg text-xs text-blood-light
                         hover:bg-blood/10 transition-colors border border-transparent hover:border-blood/20"
            >
              <LogOut size={14} />
              Sair
            </button>
          </div>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col min-h-screen min-w-0">
        {/* Header */}
        <header className="sticky top-0 z-30 bg-parchment/90 backdrop-blur-md border-b border-border">
          <div className="flex items-center gap-4 px-4 sm:px-6 py-3">
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="lg:hidden p-2 -ml-2 rounded-lg text-text-secondary hover:text-gold hover:bg-parchment-light transition-colors"
            >
              {sidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
            <div className="flex items-center gap-2">
              <h2 className="text-base sm:text-lg font-semibold text-text-primary">{pageTitle}</h2>
            </div>
            <div className="ml-auto flex items-center gap-2">
              <button
                onClick={toggleTheme}
                className="p-2 rounded-lg text-text-secondary hover:text-gold hover:bg-parchment-light transition-colors"
                title={theme === 'dark' ? 'Tema claro' : 'Tema escuro'}
              >
                {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <span className="rpg-badge rpg-badge-green text-xs">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-light animate-pulse mr-1.5"></span>
                Online
              </span>
              {/* Mobile user avatar */}
              {isMobile && user?.avatarUrl && (
                <img src={user.avatarUrl} alt="" className="w-7 h-7 rounded-lg border border-border lg:hidden" />
              )}
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 p-4 sm:p-6 animate-fade-in page-container">
          {children}
        </main>
      </div>

      {/* Floating logs widget */}
      <LogsWidget />

      {/* Role override widget (owner only) */}
      <RoleOverrideWidget />
    </div>
  )
}
