import { useState, useEffect, useRef } from 'react'
import { ScrollText, X, ChevronDown, Filter } from 'lucide-react'
import { useApi } from '../hooks/useApi'

const CATEGORY_LABELS = {
  punishment: { label: 'Punição', color: 'text-blood-light' },
  ticket: { label: 'Ticket', color: 'text-rune-blue-light' },
  whitelist: { label: 'Whitelist', color: 'text-emerald-light' },
  moderation: { label: 'Moderação', color: 'text-gold' },
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  } catch { return '' }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
  } catch { return '' }
}

function groupByDate(logs) {
  const groups = {}
  for (const log of logs) {
    const date = log.createdAt ? log.createdAt.split(' ')[0] || log.createdAt.split('T')[0] : 'unknown'
    if (!groups[date]) groups[date] = []
    groups[date].push(log)
  }
  return groups
}

export default function LogsWidget() {
  const [open, setOpen] = useState(false)
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [category, setCategory] = useState('')
  const [showFilters, setShowFilters] = useState(false)
  const [unread, setUnread] = useState(0)
  const lastSeenRef = useRef(0)
  const panelRef = useRef(null)
  const api = useApi()

  // Buscar logs
  async function fetchLogs() {
    setLoading(true)
    try {
      const params = new URLSearchParams()
      if (category) params.append('category', category)
      params.append('limit', '200')
      const res = await api.get(`/logs?${params}`)
      setLogs(res.logs || [])
    } catch (err) {
      console.error('Erro ao buscar logs:', err)
    } finally {
      setLoading(false)
    }
  }

  // Polling a cada 30s quando aberto
  useEffect(() => {
    fetchLogs()
    if (open) {
      const interval = setInterval(fetchLogs, 30000)
      return () => clearInterval(interval)
    }
  }, [open, category])

  // Contar unread quando fechado
  useEffect(() => {
    if (!open && logs.length > 0) {
      const newCount = logs.filter(l => l.id > lastSeenRef.current).length
      setUnread(newCount)
    }
  }, [logs, open])

  // Marcar como lido ao abrir
  useEffect(() => {
    if (open && logs.length > 0) {
      lastSeenRef.current = Math.max(...logs.map(l => l.id))
      setUnread(0)
    }
  }, [open, logs])

  // Fechar ao clicar fora
  useEffect(() => {
    function handleClick(e) {
      if (panelRef.current && !panelRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    if (open) document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [open])

  const grouped = groupByDate(logs)

  return (
    <div ref={panelRef} className="fixed bottom-5 right-5 z-50">
      {/* Panel */}
      {open && (
        <div className="absolute bottom-16 right-0 w-[380px] max-h-[520px] bg-parchment border border-border rounded-xl shadow-2xl flex flex-col overflow-hidden animate-fade-in">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-parchment-light">
            <div className="flex items-center gap-2">
              <ScrollText size={16} className="text-gold" />
              <span className="text-sm font-semibold text-text-primary">Logs do Sistema</span>
              <span className="text-[10px] text-text-muted bg-parchment-lighter px-1.5 py-0.5 rounded">
                {logs.length}
              </span>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setShowFilters(!showFilters)}
                className={`p-1.5 rounded-lg transition-colors ${showFilters ? 'text-gold bg-gold/10' : 'text-text-muted hover:text-text-primary'}`}
                title="Filtros"
              >
                <Filter size={14} />
              </button>
              <button
                onClick={() => setOpen(false)}
                className="p-1.5 rounded-lg text-text-muted hover:text-text-primary transition-colors"
              >
                <X size={14} />
              </button>
            </div>
          </div>

          {/* Filters */}
          {showFilters && (
            <div className="px-4 py-2 border-b border-border bg-parchment-light/50 flex gap-2 flex-wrap">
              <button
                onClick={() => setCategory('')}
                className={`text-[11px] px-2 py-1 rounded-md border transition-colors ${
                  !category ? 'border-gold/30 bg-gold/10 text-gold' : 'border-border text-text-muted hover:text-text-secondary'
                }`}
              >
                Todas
              </button>
              {Object.entries(CATEGORY_LABELS).map(([key, { label }]) => (
                <button
                  key={key}
                  onClick={() => setCategory(key)}
                  className={`text-[11px] px-2 py-1 rounded-md border transition-colors ${
                    category === key ? 'border-gold/30 bg-gold/10 text-gold' : 'border-border text-text-muted hover:text-text-secondary'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          )}

          {/* Logs list */}
          <div className="flex-1 overflow-y-auto px-3 py-2 space-y-3">
            {loading && logs.length === 0 ? (
              <div className="flex items-center justify-center py-8">
                <div className="w-5 h-5 border-2 border-gold/30 border-t-gold rounded-full animate-spin" />
              </div>
            ) : logs.length === 0 ? (
              <div className="text-center py-8 text-text-muted text-sm">
                Nenhum log encontrado
              </div>
            ) : (
              Object.entries(grouped).map(([date, dayLogs]) => (
                <div key={date}>
                  <div className="flex items-center gap-2 mb-2">
                    <div className="h-px flex-1 bg-border" />
                    <span className="text-[10px] text-text-muted uppercase tracking-wider">
                      {formatDate(date)}
                    </span>
                    <div className="h-px flex-1 bg-border" />
                  </div>
                  <div className="space-y-1.5">
                    {dayLogs.map(log => (
                      <LogEntry key={log.id} log={log} />
                    ))}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Floating button */}
      <button
        onClick={() => setOpen(!open)}
        className={`
          w-12 h-12 rounded-full shadow-lg flex items-center justify-center
          transition-all duration-200 hover:scale-105 active:scale-95
          ${open
            ? 'bg-parchment-lighter border border-border text-text-secondary'
            : 'bg-gold text-parchment-dark hover:bg-gold-dark'
          }
        `}
        title="Logs do Sistema"
      >
        {open ? <ChevronDown size={20} /> : <ScrollText size={20} />}
        {!open && unread > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-blood text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>
    </div>
  )
}

function LogEntry({ log }) {
  const [expanded, setExpanded] = useState(false)
  const cat = CATEGORY_LABELS[log.category] || { label: log.category, color: 'text-text-muted' }
  const hasLongMessage = log.message && log.message.length > 100

  return (
    <div
      className="group bg-parchment-light/50 hover:bg-parchment-light border border-border/50 rounded-lg px-3 py-2 cursor-pointer transition-colors"
      onClick={() => setExpanded(!expanded)}
    >
      <div className="flex items-start gap-2">
        {/* Icon / Avatar */}
        {log.targetAvatar ? (
          <img src={log.targetAvatar} alt="" className="w-7 h-7 rounded-full border border-border mt-0.5 shrink-0" />
        ) : (
          <span className="text-base mt-0.5 shrink-0">{log.icon || '📋'}</span>
        )}

        <div className="flex-1 min-w-0">
          {/* Title + time */}
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-medium text-text-primary truncate">{log.title}</span>
            <span className="text-[10px] text-text-muted shrink-0">{formatTime(log.createdAt)}</span>
          </div>

          {/* Category + target */}
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className={`text-[10px] font-medium ${cat.color}`}>{cat.label}</span>
            {log.targetName && (
              <>
                <span className="text-[10px] text-text-muted">•</span>
                <span className="text-[10px] text-text-secondary truncate">{log.targetName}</span>
              </>
            )}
          </div>

          {/* Message preview / expanded */}
          {log.message && (
            <div className={`mt-1 text-[11px] text-text-muted leading-relaxed whitespace-pre-wrap ${
              !expanded && hasLongMessage ? 'line-clamp-2' : ''
            }`}>
              {log.message}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
