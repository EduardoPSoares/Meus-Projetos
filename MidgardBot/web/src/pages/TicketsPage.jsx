import { useState, useEffect } from 'react'
import { useApi } from '../hooks/useApi'
import {
  Ticket, User, Clock, X, MessageSquare, Shield,
  ChevronLeft, ChevronRight, Search, ArrowUpRight, ScrollText, Eye
} from 'lucide-react'

/* ─── Card do Ticket no grid ─── */
function TicketCard({ ticket, onClickTicket, onClickUser }) {
  const isOpen = ticket.status === 'open'
  const isHigh = ticket.priority === 'HIGH' || ticket.priority === 'URGENT'

  function handleUserClick(e) {
    e.stopPropagation()
    if (ticket.userId) onClickUser(ticket.userId, ticket.userName || ticket.userId)
  }

  return (
    <button
      onClick={() => onClickTicket(ticket)}
      className="rpg-card p-4 w-full text-left group hover:scale-[1.01] transition-all animate-stagger-in"
      style={{ animationDelay: `${(ticket._idx || 0) * 40}ms` }}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div onClick={handleUserClick} className="flex-shrink-0 cursor-pointer hover:ring-2 hover:ring-gold/40 rounded-lg transition-all" title="Ver tickets deste jogador">
            {ticket.userAvatar ? (
              <img src={ticket.userAvatar} alt="" className="w-10 h-10 rounded-lg border border-border" />
            ) : (
              <div className="w-10 h-10 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center">
                <User size={16} className="text-text-muted" />
              </div>
            )}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <span className="text-text-muted text-xs font-mono">#{ticket.id || '—'}</span>
              {isHigh && <span className="text-[10px]">🔴</span>}
            </div>
            <p onClick={handleUserClick} className="text-text-primary font-medium text-sm truncate cursor-pointer hover:text-gold transition-colors" title="Ver tickets deste jogador">
              {ticket.userName || ticket.userId || 'Desconhecido'}
            </p>
            {ticket.category && (
              <p className="text-text-muted text-xs">{ticket.category}</p>
            )}
          </div>
        </div>
        <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
          {isOpen ? (
            <span className="rpg-badge rpg-badge-green">Aberto</span>
          ) : (
            <span className="rpg-badge rpg-badge-red">Fechado</span>
          )}
          <span className="text-text-muted text-[10px] flex items-center gap-1">
            <Clock size={9} />
            {ticket.closedAt ? fmtDate(ticket.closedAt) : ticket.createdAt ? fmtDate(ticket.createdAt) : '—'}
          </span>
        </div>
      </div>
      <div className="mt-2 pt-2 border-t border-border/30 flex items-center justify-between">
        <span className="text-text-muted text-xs truncate">{ticket.channelName || '—'}</span>
        <ArrowUpRight size={12} className="text-text-muted opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0" />
      </div>
    </button>
  )
}

/* ─── Modal de Detalhe do Ticket ─── */
function TicketDetailModal({ ticket, onClose, onViewUserTickets, onViewTranscript }) {
  if (!ticket) return null
  const isOpen = ticket.status === 'open'

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-backdrop" onClick={onClose}>
      <div className="rpg-card rpg-glow p-5 sm:p-6 max-w-lg w-full max-h-[85vh] overflow-y-auto animate-scale-in mc-border-top"
        onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            {ticket.userAvatar ? (
              <img src={ticket.userAvatar} alt="" className="w-12 h-12 rounded-xl border-2 border-gold/30" />
            ) : (
              <div className="w-12 h-12 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center">
                <User size={20} className="text-text-muted" />
              </div>
            )}
            <div>
              <h3 className="text-lg font-bold text-text-primary">{ticket.userName || 'Desconhecido'}</h3>
              <p className="text-text-muted text-xs font-mono">Ticket #{ticket.id}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-parchment-lighter transition-colors">
            <X size={18} className="text-text-muted" />
          </button>
        </div>

        <div className="rpg-divider"></div>

        {/* Info grid */}
        <div className="grid grid-cols-2 gap-3 my-4">
          <div className="bg-shadow/40 rounded-lg p-3">
            <p className="text-text-muted text-[10px] uppercase tracking-wider mb-1">Status</p>
            {isOpen
              ? <span className="rpg-badge rpg-badge-green">Aberto</span>
              : <span className="rpg-badge rpg-badge-red">Fechado</span>}
          </div>
          <div className="bg-shadow/40 rounded-lg p-3">
            <p className="text-text-muted text-[10px] uppercase tracking-wider mb-1">Prioridade</p>
            <span className={`rpg-badge ${ticket.priority === 'HIGH' || ticket.priority === 'URGENT' ? 'rpg-badge-red' : 'rpg-badge-blue'}`}>
              {ticket.priority === 'HIGH' ? '🔴 Alta' : ticket.priority === 'URGENT' ? '🔴 Urgente' : 'Normal'}
            </span>
          </div>
          {ticket.category && (
            <div className="bg-shadow/40 rounded-lg p-3">
              <p className="text-text-muted text-[10px] uppercase tracking-wider mb-1">Categoria</p>
              <p className="text-text-primary text-sm font-medium">{ticket.category}</p>
            </div>
          )}
          <div className="bg-shadow/40 rounded-lg p-3">
            <p className="text-text-muted text-[10px] uppercase tracking-wider mb-1">Canal</p>
            <p className="text-text-secondary text-xs font-mono truncate">{ticket.channelName || '—'}</p>
          </div>
        </div>

        {/* Dates */}
        <div className="space-y-2 my-4">
          {ticket.createdAt && (
            <div className="flex items-center gap-2 text-sm">
              <Clock size={14} className="text-text-muted flex-shrink-0" />
              <span className="text-text-muted">Criado:</span>
              <span className="text-text-secondary">{fmtDateFull(ticket.createdAt)}</span>
            </div>
          )}
          {ticket.closedAt && (
            <div className="flex items-center gap-2 text-sm">
              <Clock size={14} className="text-text-muted flex-shrink-0" />
              <span className="text-text-muted">Fechado:</span>
              <span className="text-text-secondary">{fmtDateFull(ticket.closedAt)}</span>
            </div>
          )}
        </div>

        {/* Staff info */}
        {ticket.staffName && (
          <>
            <div className="rpg-divider"></div>
            <div className="my-4">
              <p className="text-gold text-xs font-semibold uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <Shield size={12} /> Staff Responsável
              </p>
              <div className="flex items-center gap-2 bg-shadow/40 rounded-lg p-3">
                {ticket.staffAvatar ? (
                  <img src={ticket.staffAvatar} alt="" className="w-8 h-8 rounded-lg" />
                ) : (
                  <div className="w-8 h-8 rounded-lg bg-parchment-lighter flex items-center justify-center">
                    <Shield size={14} className="text-gold" />
                  </div>
                )}
                <span className="text-text-primary text-sm font-medium">{ticket.staffName}</span>
              </div>
            </div>
          </>
        )}

        {/* Collaborators */}
        {ticket.collaborators?.length > 0 && (
          <div className="my-4">
            <p className="text-text-muted text-xs font-semibold uppercase tracking-wider mb-2">Colaboradores</p>
            <div className="flex flex-wrap gap-2">
              {ticket.collaborators.map(c => (
                <span key={c.id} className="rpg-badge rpg-badge-blue">{c.name}</span>
              ))}
            </div>
          </div>
        )}

        {/* Activity (open tickets) */}
        {ticket.messageCount != null && (
          <>
            <div className="rpg-divider"></div>
            <div className="my-4">
              <p className="text-gold text-xs font-semibold uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <MessageSquare size={12} /> Atividade
              </p>
              <div className="grid grid-cols-2 gap-2">
                <div className="bg-shadow/40 rounded-lg p-3 text-center">
                  <p className="text-xl font-bold text-text-primary">{ticket.messageCount}</p>
                  <p className="text-text-muted text-[10px]">Mensagens</p>
                </div>
                {ticket.lastAuthor && (
                  <div className="bg-shadow/40 rounded-lg p-3 text-center">
                    <p className="text-sm font-medium text-text-primary truncate">{ticket.lastAuthor}</p>
                    <p className="text-text-muted text-[10px]">Última msg</p>
                  </div>
                )}
              </div>
            </div>
          </>
        )}

        {/* Action buttons */}
        <div className="rpg-divider"></div>
        <div className="flex flex-wrap gap-2 mt-4">
          {ticket.userId && (
            <button
              onClick={() => onViewUserTickets(ticket.userId, ticket.userName || ticket.userId)}
              className="rpg-button-secondary text-xs flex items-center gap-1.5"
            >
              <User size={12} /> Ver tickets do jogador
            </button>
          )}
          {ticket.hasTranscript && (
            <button
              onClick={() => onViewTranscript(ticket.id)}
              className="rpg-button text-xs flex items-center gap-1.5"
            >
              <ScrollText size={12} /> Ler Transcrição
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

/* ─── Modal de Tickets do Usuário ─── */
function UserTicketsModal({ userId, userName, tickets, loading, onClose, onSelectTicket }) {
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4 animate-backdrop" onClick={onClose}>
      <div className="rpg-card rpg-glow p-5 sm:p-6 max-w-2xl w-full max-h-[85vh] overflow-y-auto animate-scale-in mc-border-top"
        onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-lg font-bold text-text-primary flex items-center gap-2">
              <User size={18} className="text-gold" /> {userName}
            </h3>
            <p className="text-text-muted text-xs">Todos os tickets deste jogador</p>
          </div>
          <button onClick={onClose} className="p-1 rounded-lg hover:bg-parchment-lighter transition-colors">
            <X size={18} className="text-text-muted" />
          </button>
        </div>

        <div className="rpg-divider"></div>

        {loading ? (
          <div className="py-10 text-center text-text-muted text-sm">Carregando tickets...</div>
        ) : tickets.length === 0 ? (
          <div className="py-10 text-center">
            <Ticket size={32} className="mx-auto text-text-muted mb-2 opacity-40" />
            <p className="text-text-muted text-sm">Nenhum ticket encontrado</p>
          </div>
        ) : (
          <div className="space-y-2 mt-4">
            {tickets.map(t => (
              <button
                key={`${t.status}-${t.id}`}
                onClick={() => onSelectTicket(t)}
                className="w-full bg-parchment hover:bg-parchment-lighter rounded-lg p-3 flex items-center justify-between transition-colors text-left group"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <span className="text-text-muted text-xs font-mono flex-shrink-0">#{t.id}</span>
                  {t.status === 'open'
                    ? <span className="rpg-badge rpg-badge-green flex-shrink-0">Aberto</span>
                    : <span className="rpg-badge rpg-badge-red flex-shrink-0">Fechado</span>}
                  <span className={`rpg-badge flex-shrink-0 ${t.priority === 'HIGH' || t.priority === 'URGENT' ? 'rpg-badge-red' : 'rpg-badge-blue'}`}>
                    {t.priority === 'HIGH' ? 'Alta' : t.priority === 'URGENT' ? 'Urgente' : 'Normal'}
                  </span>
                  <span className="text-text-secondary text-xs truncate">{t.channelName || '—'}</span>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0 ml-2">
                  <span className="text-text-muted text-[10px] flex items-center gap-1">
                    <Clock size={9} />
                    {t.closedAt ? fmtDate(t.closedAt) : t.createdAt ? fmtDate(t.createdAt) : '—'}
                  </span>
                  <Eye size={12} className="text-text-muted opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

/* ─── Visualizador de Transcrição ─── */
function TranscriptViewer({ ticketId, onClose, api }) {
  const [messages, setMessages] = useState(null)
  const [users, setUsers] = useState({})
  const [roles, setRoles] = useState({})
  const [rawHtml, setRawHtml] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await api.get(`/tickets/${ticketId}/transcript`)
        const content = data.transcript
        if (cancelled) return

        if (data.users) setUsers(data.users)
        if (data.roles) setRoles(data.roles)

        const trimmed = content.trim()
        if (trimmed.startsWith('[')) {
          try {
            setMessages(JSON.parse(trimmed))
          } catch {
            setRawHtml(content)
          }
        } else {
          setRawHtml(content)
        }
      } catch (e) {
        if (!cancelled) setError(e.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [ticketId])

  function fmtMsgTime(ts) {
    if (!ts) return ''
    const d = new Date(ts)
    if (isNaN(d)) return ts
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }) + ' ' +
           d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  }

  function groupMessages(msgs) {
    const groups = []
    for (const msg of msgs) {
      const last = groups[groups.length - 1]
      if (last && last.authorId === msg.authorId) {
        last.messages.push(msg)
      } else {
        groups.push({ author: msg.author, authorId: msg.authorId, messages: [msg] })
      }
    }
    return groups
  }

  // Renderiza menções <@ID>, <@!ID>, <@&roleID>, <#channelID> e emojis custom
  function renderContent(text) {
    if (!text) return null
    const parts = []
    const regex = /<@!?(\d+)>|<@&(\d+)>|<#(\d+)>/g
    let lastIndex = 0
    let match
    while ((match = regex.exec(text)) !== null) {
      if (match.index > lastIndex) parts.push(text.slice(lastIndex, match.index))
      if (match[1]) {
        // User mention
        const u = users[match[1]]
        parts.push(
          <span key={match.index} className="inline-flex items-center gap-1 bg-[#5865F2]/20 text-[#5865F2] px-1.5 py-0.5 rounded text-xs font-medium">
            {u?.avatar && <img src={u.avatar} alt="" className="w-3.5 h-3.5 rounded-full inline" />}
            @{u?.displayName || u?.name || match[1]}
          </span>
        )
      } else if (match[2]) {
        // Role mention
        const r = roles[match[2]]
        const c = r?.color || '#99AAB5'
        parts.push(
          <span key={match.index} className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium" style={{ background: c + '20', color: c }}>
            @{r?.name || match[2]}
          </span>
        )
      } else if (match[3]) {
        // Channel mention
        parts.push(
          <span key={match.index} className="inline-flex items-center bg-[#5865F2]/20 text-[#5865F2] px-1.5 py-0.5 rounded text-xs font-medium">
            #{match[3]}
          </span>
        )
      }
      lastIndex = regex.lastIndex
    }
    if (lastIndex < text.length) parts.push(text.slice(lastIndex))
    return parts.length > 0 ? parts : text
  }

  function getAuthorAvatar(authorId) {
    return users[authorId]?.avatar || null
  }

  function getAuthorDisplayName(authorId, fallback) {
    const u = users[authorId]
    return u?.displayName || u?.name || fallback || 'Desconhecido'
  }

  const BOT_ID = '891253756862271510'

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-[70] p-2 sm:p-4 animate-backdrop" onClick={onClose}>
      <div className="bg-parchment-dark border border-border rounded-xl w-full max-w-4xl h-[90vh] flex flex-col animate-fade-in overflow-hidden"
        onClick={e => e.stopPropagation()}>
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-border">
          <h3 className="text-text-primary font-bold flex items-center gap-2">
            <ScrollText size={16} className="text-gold" />
            Transcrição — Ticket #{ticketId}
          </h3>
          <div className="flex items-center gap-2">
            {messages && (
              <span className="text-text-muted text-xs">{messages.length} mensagens</span>
            )}
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-parchment-lighter transition-colors">
              <X size={18} className="text-text-muted" />
            </button>
          </div>
        </div>
        {/* Content */}
        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center h-full text-text-muted text-sm">
              Carregando transcrição...
            </div>
          ) : error ? (
            <div className="flex items-center justify-center h-full text-blood-light text-sm">
              {error}
            </div>
          ) : messages ? (
            <div className="p-4 space-y-4">
              {groupMessages(messages).map((group, gi) => {
                const isBot = group.authorId === BOT_ID
                const colors = authorColor(group.authorId)
                const avatar = getAuthorAvatar(group.authorId)
                const displayName = getAuthorDisplayName(group.authorId, group.author)
                return (
                  <div key={gi} className="flex gap-3 hover:bg-shadow/20 -mx-2 px-2 py-1 rounded-lg transition-colors">
                    {avatar ? (
                      <img src={avatar} alt="" className="w-10 h-10 rounded-full flex-shrink-0 border border-border/50" />
                    ) : (
                      <div className="w-10 h-10 rounded-full flex-shrink-0 flex items-center justify-center text-sm font-bold text-white"
                        style={{ background: colors.bg }}>
                        {isBot ? '🤖' : (group.author || '?')[0].toUpperCase()}
                      </div>
                    )}
                    <div className="min-w-0 flex-1">
                      <div className="flex items-baseline gap-2 mb-0.5">
                        <span className="text-sm font-semibold" style={{ color: colors.text }}>
                          {displayName}
                        </span>
                        {isBot && <span className="text-[9px] bg-[#5865F2] text-white px-1.5 py-0.5 rounded font-semibold uppercase">Bot</span>}
                        <span className="text-text-muted text-[10px]">{fmtMsgTime(group.messages[0].timestamp)}</span>
                      </div>
                      <div className="space-y-0.5">
                        {group.messages.map((m, mi) => {
                          if (!m.content) return null
                          return (
                            <p key={mi} className="text-text-secondary text-sm break-words leading-relaxed">
                              {renderContent(m.content)}
                              {mi > 0 && m.timestamp && (
                                <span className="text-text-muted text-[9px] ml-2 opacity-0 hover:opacity-100 transition-opacity">
                                  {fmtMsgTime(m.timestamp)}
                                </span>
                              )}
                            </p>
                          )
                        })}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          ) : rawHtml ? (
            // Fallback: HTML via iframe
            <iframe
              title={`Transcript ${ticketId}`}
              sandbox="allow-same-origin allow-scripts"
              srcDoc={rawHtml}
              className="w-full h-full border-0 bg-white rounded-b-xl"
            />
          ) : null}
        </div>
      </div>
    </div>
  )
}

/* ─── Página Principal ─── */
export default function TicketsPage() {
  const api = useApi()
  const [tickets, setTickets] = useState([])
  const [stats, setStats] = useState(null)
  const [filter, setFilter] = useState('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')

  // Modais
  const [selectedTicket, setSelectedTicket] = useState(null)
  const [userModal, setUserModal] = useState(null) // { userId, userName, tickets, loading }
  const [transcriptId, setTranscriptId] = useState(null)

  useEffect(() => {
    load()
    const interval = setInterval(load, 30000)
    return () => clearInterval(interval)
  }, [filter, page])
  useEffect(() => {
    loadStats()
    const interval = setInterval(loadStats, 30000)
    return () => clearInterval(interval)
  }, [])

  async function load() {
    setLoading(true)
    try {
      const params = new URLSearchParams({ page, limit: 18 })
      if (filter) params.set('status', filter)
      const data = await api.get(`/tickets?${params}`)
      setTickets(data.tickets || [])
      setTotalPages(data.pages || 1)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  async function loadStats() {
    try {
      const data = await api.get('/tickets/stats')
      setStats(data)
    } catch (e) {
      console.error(e)
    }
  }

  async function openTicketDetail(ticket) {
    setSelectedTicket(ticket)
    try {
      const detail = await api.get(`/tickets/${ticket.id}`)
      setSelectedTicket(detail)
    } catch (e) {
      console.error(e)
    }
  }

  async function openUserTickets(userId, userName) {
    setUserModal({ userId, userName, tickets: [], loading: true })
    try {
      const data = await api.get(`/tickets/user/${userId}`)
      setUserModal({ userId, userName, tickets: data.tickets || [], loading: false })
    } catch (e) {
      console.error(e)
      setUserModal(prev => prev ? { ...prev, loading: false } : null)
    }
  }

  function handleUserTicketSelect(ticket) {
    setUserModal(null)
    openTicketDetail(ticket)
  }

  const filtered = searchQuery.length >= 2
    ? tickets.filter(t =>
        (t.userName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (t.channelName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        String(t.id).includes(searchQuery)
      )
    : tickets

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-3 gap-3">
          <div className="rpg-card p-3 sm:p-4 text-center group hover:scale-[1.02] transition-transform animate-stagger-in" style={{ animationDelay: '0ms' }}>
            <p className="text-xl sm:text-2xl font-bold text-emerald-light">{stats.open}</p>
            <p className="text-text-muted text-[10px] sm:text-xs uppercase mt-1">Abertos</p>
          </div>
          <div className="rpg-card p-3 sm:p-4 text-center group hover:scale-[1.02] transition-transform animate-stagger-in" style={{ animationDelay: '80ms' }}>
            <p className="text-xl sm:text-2xl font-bold text-text-secondary">{stats.closed}</p>
            <p className="text-text-muted text-[10px] sm:text-xs uppercase mt-1">Fechados</p>
          </div>
          <div className="rpg-card p-3 sm:p-4 text-center group hover:scale-[1.02] transition-transform animate-stagger-in" style={{ animationDelay: '160ms' }}>
            <p className="text-xl sm:text-2xl font-bold text-gold">{stats.total}</p>
            <p className="text-text-muted text-[10px] sm:text-xs uppercase mt-1">Total</p>
          </div>
        </div>
      )}

      {/* Toolbar */}
      <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
        <div className="flex flex-wrap gap-2">
          {['', 'open', 'closed'].map(f => (
            <button
              key={f}
              onClick={() => { setFilter(f); setPage(1) }}
              className={`rpg-button-secondary text-xs sm:text-sm ${filter === f ? 'active' : ''}`}
            >
              {f === '' ? 'Todos' : f === 'open' ? 'Abertos' : 'Fechados'}
            </button>
          ))}
        </div>
        <div className="relative w-full sm:w-64">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Filtrar por nome ou #..."
            className="rpg-input w-full pl-9 py-2 text-xs"
          />
        </div>
      </div>

      {/* Grid de Tickets */}
      {loading ? (
        <div className="text-center py-16">
          <div className="animate-pulse-glow w-14 h-14 rounded-xl bg-gold/15 border border-gold/25 flex items-center justify-center mx-auto mb-3">
            <Ticket size={24} className="text-gold" />
          </div>
          <p className="text-text-muted text-sm">Carregando tickets...</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="rpg-card p-12 text-center">
          <Ticket size={40} className="mx-auto text-text-muted mb-3 opacity-40" />
          <p className="text-text-muted">Nenhum ticket encontrado.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {filtered.map((t, idx) => (
            <TicketCard
              key={`${t.status}-${t.id}`}
              ticket={{ ...t, _idx: idx }}
              onClickTicket={openTicketDetail}
              onClickUser={openUserTickets}
            />
          ))}
        </div>
      )}

      {/* Paginação */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-2">
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page <= 1}
            className="rpg-button text-sm disabled:opacity-40 flex items-center gap-1"
          >
            <ChevronLeft size={14} /> Anterior
          </button>
          <span className="px-4 py-2 text-text-muted text-sm font-mono">{page}/{totalPages}</span>
          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page >= totalPages}
            className="rpg-button text-sm disabled:opacity-40 flex items-center gap-1"
          >
            Próxima <ChevronRight size={14} />
          </button>
        </div>
      )}

      {/* Modal: Detalhe do Ticket */}
      {selectedTicket && (
        <TicketDetailModal
          ticket={selectedTicket}
          onClose={() => setSelectedTicket(null)}
          onViewUserTickets={(uid, name) => {
            setSelectedTicket(null)
            openUserTickets(uid, name)
          }}
          onViewTranscript={(id) => {
            setSelectedTicket(null)
            setTranscriptId(id)
          }}
        />
      )}

      {/* Modal: Tickets do Usuário */}
      {userModal && (
        <UserTicketsModal
          userId={userModal.userId}
          userName={userModal.userName}
          tickets={userModal.tickets}
          loading={userModal.loading}
          onClose={() => setUserModal(null)}
          onSelectTicket={handleUserTicketSelect}
        />
      )}

      {/* Modal: Visualizador de Transcrição */}
      {transcriptId && (
        <TranscriptViewer
          ticketId={transcriptId}
          api={api}
          onClose={() => setTranscriptId(null)}
        />
      )}
    </div>
  )
}

/* ─── Helpers ─── */
function fmtDate(v) {
  if (!v) return '—'
  const d = new Date(v)
  if (isNaN(d)) return v
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function fmtDateFull(v) {
  if (!v) return '—'
  const d = new Date(v)
  if (isNaN(d)) return v
  return d.toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

// Cor estável por authorId para o chat de transcrição
const AUTHOR_COLORS = [
  { bg: '#5865F2', text: '#7983F5' }, // blurple
  { bg: '#57F287', text: '#57F287' }, // green
  { bg: '#FEE75C', text: '#E5D04E' }, // yellow
  { bg: '#EB459E', text: '#EB459E' }, // fuchsia
  { bg: '#ED4245', text: '#ED4245' }, // red
  { bg: '#FF7B3A', text: '#FF7B3A' }, // orange
  { bg: '#3BA5B9', text: '#3BA5B9' }, // teal
  { bg: '#9B59B6', text: '#B07CC6' }, // purple
]
function authorColor(authorId) {
  if (!authorId) return AUTHOR_COLORS[0]
  let hash = 0
  for (let i = 0; i < authorId.length; i++) hash = (hash * 31 + authorId.charCodeAt(i)) | 0
  return AUTHOR_COLORS[Math.abs(hash) % AUTHOR_COLORS.length]
}
