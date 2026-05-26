import { useState, useEffect } from 'react'
import { useApi } from '../hooks/useApi'
import { useAuth } from '../context/AuthContext'
import { User, ChevronDown, ChevronUp, Users, X, Ticket, Clock, MessageSquare, Eye, Search, ScrollText } from 'lucide-react'
import { SwordIcon, ShieldPixelIcon } from '../assets/minecraft-icons'

const TICKET_VIEW_ROLES = ['AJUDANTE', 'MODERADOR', 'DEV', 'CEOO', 'FUNDADOR']

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

/* ─── Modal: Tickets do Staff ─── */
function LegacyStaffTicketsModal({ staff, onClose, api }) {
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [transcriptId, setTranscriptId] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await api.get(`/staff/${staff.discordId}/tickets`)
        if (!cancelled) setTickets(data.tickets || [])
      } catch (e) {
        console.error(e)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [staff.discordId])

  const filtered = searchQuery.length >= 2
    ? tickets.filter(t =>
        (t.userName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (t.channelName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        String(t.id).includes(searchQuery)
      )
    : tickets

  const openCount = tickets.filter(t => t.status === 'open').length
  const closedCount = tickets.filter(t => t.status === 'closed').length

  return (
    <>
      <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-backdrop" onClick={onClose}>
        <div className="rpg-card rpg-glow p-5 sm:p-6 max-w-3xl w-full max-h-[85vh] overflow-y-auto animate-scale-in mc-border-top"
          onClick={e => e.stopPropagation()}>
          {/* Header */}
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-3">
              {staff.avatar ? (
                <img src={staff.avatar} alt="" className="w-12 h-12 rounded-xl border-2 border-gold/30" />
              ) : (
                <div className="w-12 h-12 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center">
                  <User size={20} className="text-text-muted" />
                </div>
              )}
              <div>
                <h3 className="text-lg font-bold text-text-primary">{staff.displayName || staff.name}</h3>
                <p className="text-text-muted text-xs">Tickets atendidos por este membro</p>
              </div>
            </div>
            <button onClick={onClose} className="p-1 rounded-lg hover:bg-parchment-lighter transition-colors">
              <X size={18} className="text-text-muted" />
            </button>
          </div>

          <div className="rpg-divider" />

          {/* Stats mini */}
          {!loading && tickets.length > 0 && (
            <div className="grid grid-cols-3 gap-2 my-4">
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-gold">{tickets.length}</p>
                <p className="text-text-muted text-[10px]">Total</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-emerald-light">{openCount}</p>
                <p className="text-text-muted text-[10px]">Abertos</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-text-secondary">{closedCount}</p>
                <p className="text-text-muted text-[10px]">Fechados</p>
              </div>
            </div>
          )}

          {/* Search */}
          {!loading && tickets.length > 3 && (
            <div className="relative mb-4">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Filtrar por nome ou #..."
                className="rpg-input w-full pl-9 py-2 text-xs"
              />
            </div>
          )}

          {/* Lista de tickets */}
          {loading ? (
            <div className="py-10 text-center text-text-muted text-sm">Carregando tickets...</div>
          ) : filtered.length === 0 ? (
            <div className="py-10 text-center">
              <Ticket size={32} className="mx-auto text-text-muted mb-2 opacity-40" />
              <p className="text-text-muted text-sm">
                {tickets.length === 0 ? 'Nenhum ticket encontrado' : 'Nenhum resultado para o filtro'}
              </p>
            </div>
          ) : (
            <div className="space-y-2 mt-2">
              {filtered.map(t => (
                <div
                  key={`${t.status}-${t.id}`}
                  className="bg-parchment hover:bg-parchment-lighter rounded-lg p-3 flex items-center justify-between transition-colors group border border-border/30"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    {t.userAvatar ? (
                      <img src={t.userAvatar} alt="" className="w-8 h-8 rounded-lg border border-border flex-shrink-0" />
                    ) : (
                      <div className="w-8 h-8 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center flex-shrink-0">
                        <User size={14} className="text-text-muted" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-text-muted text-xs font-mono">#{t.id || '—'}</span>
                        {t.status === 'open'
                          ? <span className="rpg-badge rpg-badge-green">Aberto</span>
                          : <span className="rpg-badge rpg-badge-red">Fechado</span>}
                        {t.staffMessageCount != null && (
                          <span className="text-text-muted text-[10px] flex items-center gap-0.5">
                            <MessageSquare size={9} /> {t.staffMessageCount} msgs
                          </span>
                        )}
                      </div>
                      <p className="text-text-primary text-sm truncate">
                        {t.userName || t.channelName || 'Desconhecido'}
                      </p>
                      {t.category && (
                        <p className="text-text-muted text-[10px]">{t.category}</p>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0 ml-2">
                    <span className="text-text-muted text-[10px] flex items-center gap-1">
                      <Clock size={9} />
                      {t.closedAt ? fmtDate(t.closedAt) : t.createdAt ? fmtDate(t.createdAt) : '—'}
                    </span>
                    {t.hasTranscript && (
                      <button
                        onClick={() => setTranscriptId(t.id)}
                        className="p-1 rounded hover:bg-gold/10 transition-colors"
                        title="Ver transcrição"
                      >
                        <ScrollText size={14} className="text-gold" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Transcript viewer */}
      {transcriptId && (
        <TranscriptViewer ticketId={transcriptId} api={api} onClose={() => setTranscriptId(null)} />
      )}
    </>
  )
}

/* ─── Visualizador de Transcrição (simplificado) ─── */
function WhitelistStatusBadge({ category, label }) {
  const map = {
    approved: 'rpg-badge-green',
    rejected: 'rpg-badge-red',
    pending: 'rpg-badge-blue',
  }

  return <span className={`rpg-badge ${map[category] || 'rpg-badge-blue'}`}>{label || 'Sem status'}</span>
}

function StaffActivityModal({ staff, onClose, api }) {
  const [tickets, setTickets] = useState([])
  const [whitelists, setWhitelists] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [transcriptId, setTranscriptId] = useState(null)
  const [activeTab, setActiveTab] = useState('tickets')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [ticketsResult, whitelistsResult] = await Promise.allSettled([
          api.get(`/staff/${staff.discordId}/tickets`),
          api.get(`/staff/${staff.discordId}/whitelists`)
        ])

        if (cancelled) return

        if (ticketsResult.status === 'fulfilled') {
          setTickets(ticketsResult.value.tickets || [])
        } else {
          console.error(ticketsResult.reason)
        }

        if (whitelistsResult.status === 'fulfilled') {
          setWhitelists(whitelistsResult.value.whitelists || [])
        } else {
          console.error(whitelistsResult.reason)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => { cancelled = true }
  }, [api, staff.discordId])

  const normalizedSearch = searchQuery.trim().toLowerCase()
  const filteredTickets = normalizedSearch.length >= 2
    ? tickets.filter(t =>
        (t.userName || '').toLowerCase().includes(normalizedSearch) ||
        (t.channelName || '').toLowerCase().includes(normalizedSearch) ||
        String(t.id).includes(normalizedSearch)
      )
    : tickets

  const filteredWhitelists = normalizedSearch.length >= 2
    ? whitelists.filter(w =>
        (w.nickname || '').toLowerCase().includes(normalizedSearch) ||
        (w.discordName || '').toLowerCase().includes(normalizedSearch) ||
        (w.discordDisplayName || '').toLowerCase().includes(normalizedSearch) ||
        (w.reason || '').toLowerCase().includes(normalizedSearch) ||
        String(w.discordId || '').includes(normalizedSearch)
      )
    : whitelists

  const openCount = tickets.filter(t => t.status === 'open').length
  const closedCount = tickets.filter(t => t.status === 'closed').length
  const approvedCount = whitelists.filter(w => w.statusCategory === 'approved').length
  const rejectedCount = whitelists.filter(w => w.statusCategory === 'rejected').length
  const pendingCount = whitelists.filter(w => w.statusCategory === 'pending').length
  const currentItems = activeTab === 'tickets' ? tickets : whitelists

  return (
    <>
      <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-backdrop" onClick={onClose}>
        <div className="rpg-card rpg-glow p-5 sm:p-6 max-w-3xl w-full max-h-[85vh] overflow-y-auto animate-scale-in mc-border-top"
          onClick={e => e.stopPropagation()}>
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-3">
              {staff.avatar ? (
                <img src={staff.avatar} alt="" className="w-12 h-12 rounded-xl border-2 border-gold/30" />
              ) : (
                <div className="w-12 h-12 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center">
                  <User size={20} className="text-text-muted" />
                </div>
              )}
              <div>
                <h3 className="text-lg font-bold text-text-primary">{staff.displayName || staff.name}</h3>
                <p className="text-text-muted text-xs">Tickets atendidos e whitelists respondidas</p>
              </div>
            </div>
            <button onClick={onClose} className="p-1 rounded-lg hover:bg-parchment-lighter transition-colors">
              <X size={18} className="text-text-muted" />
            </button>
          </div>

          <div className="rpg-divider" />

          <div className="flex gap-2 my-4 border-b border-border-subtle pb-2">
            <button
              onClick={() => { setActiveTab('tickets'); setSearchQuery('') }}
              className={`px-3 py-2 text-xs sm:text-sm font-medium rounded-t transition-colors ${
                activeTab === 'tickets'
                  ? 'bg-bg-card text-gold-primary border-b-2 border-gold-primary'
                  : 'text-text-muted hover:text-text-secondary'
              }`}
            >
              Tickets
            </button>
            <button
              onClick={() => { setActiveTab('whitelists'); setSearchQuery('') }}
              className={`px-3 py-2 text-xs sm:text-sm font-medium rounded-t transition-colors ${
                activeTab === 'whitelists'
                  ? 'bg-bg-card text-gold-primary border-b-2 border-gold-primary'
                  : 'text-text-muted hover:text-text-secondary'
              }`}
            >
              Whitelists
            </button>
          </div>

          {!loading && activeTab === 'tickets' && tickets.length > 0 && (
            <div className="grid grid-cols-3 gap-2 my-4">
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-gold">{tickets.length}</p>
                <p className="text-text-muted text-[10px]">Total</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-emerald-light">{openCount}</p>
                <p className="text-text-muted text-[10px]">Abertos</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-text-secondary">{closedCount}</p>
                <p className="text-text-muted text-[10px]">Fechados</p>
              </div>
            </div>
          )}

          {!loading && activeTab === 'whitelists' && whitelists.length > 0 && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 my-4">
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-gold">{whitelists.length}</p>
                <p className="text-text-muted text-[10px]">Total</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-emerald-light">{approvedCount}</p>
                <p className="text-text-muted text-[10px]">Aprovadas</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-blood-light">{rejectedCount}</p>
                <p className="text-text-muted text-[10px]">Reprovadas</p>
              </div>
              <div className="bg-shadow/40 rounded-lg p-2.5 text-center">
                <p className="text-lg font-bold text-text-secondary">{pendingCount}</p>
                <p className="text-text-muted text-[10px]">Outras</p>
              </div>
            </div>
          )}

          {!loading && (currentItems.length > 0 || searchQuery) && (
            <div className="relative mb-4">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder={activeTab === 'tickets'
                  ? 'Filtrar tickets por nome, canal ou #...'
                  : 'Filtrar whitelists por nome, nick, ID ou motivo...'}
                className="rpg-input w-full pl-9 py-2 text-xs"
              />
            </div>
          )}

          {loading ? (
            <div className="py-10 text-center text-text-muted text-sm">Carregando atividades...</div>
          ) : activeTab === 'tickets' ? (
            filteredTickets.length === 0 ? (
              <div className="py-10 text-center">
                <Ticket size={32} className="mx-auto text-text-muted mb-2 opacity-40" />
                <p className="text-text-muted text-sm">
                  {tickets.length === 0 ? 'Nenhum ticket encontrado' : 'Nenhum resultado para o filtro'}
                </p>
              </div>
            ) : (
              <div className="space-y-2 mt-2">
                {filteredTickets.map(t => (
                  <div
                    key={`${t.status}-${t.id}`}
                    className="bg-parchment hover:bg-parchment-lighter rounded-lg p-3 flex items-center justify-between transition-colors group border border-border/30"
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      {t.userAvatar ? (
                        <img src={t.userAvatar} alt="" className="w-8 h-8 rounded-lg border border-border flex-shrink-0" />
                      ) : (
                        <div className="w-8 h-8 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center flex-shrink-0">
                          <User size={14} className="text-text-muted" />
                        </div>
                      )}
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-text-muted text-xs font-mono">#{t.id || 'â€”'}</span>
                          {t.status === 'open'
                            ? <span className="rpg-badge rpg-badge-green">Aberto</span>
                            : <span className="rpg-badge rpg-badge-red">Fechado</span>}
                          {t.staffMessageCount != null && (
                            <span className="text-text-muted text-[10px] flex items-center gap-0.5">
                              <MessageSquare size={9} /> {t.staffMessageCount} msgs
                            </span>
                          )}
                        </div>
                        <p className="text-text-primary text-sm truncate">
                          {t.userName || t.channelName || 'Desconhecido'}
                        </p>
                        {t.category && (
                          <p className="text-text-muted text-[10px]">{t.category}</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0 ml-2">
                      <span className="text-text-muted text-[10px] flex items-center gap-1">
                        <Clock size={9} />
                        {t.closedAt ? fmtDate(t.closedAt) : t.createdAt ? fmtDate(t.createdAt) : 'â€”'}
                      </span>
                      {t.hasTranscript && (
                        <button
                          onClick={() => setTranscriptId(t.id)}
                          className="p-1 rounded hover:bg-gold/10 transition-colors"
                          title="Ver transcriÃ§Ã£o"
                        >
                          <ScrollText size={14} className="text-gold" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )
          ) : filteredWhitelists.length === 0 ? (
            <div className="py-10 text-center">
              <ScrollText size={32} className="mx-auto text-text-muted mb-2 opacity-40" />
              <p className="text-text-muted text-sm">
                {whitelists.length === 0 ? 'Nenhuma whitelist encontrada' : 'Nenhum resultado para o filtro'}
              </p>
            </div>
          ) : (
            <div className="space-y-2 mt-2">
              {filteredWhitelists.map(w => (
                <div
                  key={`${w.discordId}-${w.timestamp || 'sem-data'}-${w.statusKey}`}
                  className="bg-parchment hover:bg-parchment-lighter rounded-lg p-3 flex items-start justify-between transition-colors group border border-border/30 gap-3"
                >
                  <div className="flex items-start gap-3 min-w-0">
                    {w.discordAvatar ? (
                      <img src={w.discordAvatar} alt="" className="w-8 h-8 rounded-lg border border-border flex-shrink-0" />
                    ) : (
                      <div className="w-8 h-8 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center flex-shrink-0">
                        <User size={14} className="text-text-muted" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <WhitelistStatusBadge category={w.statusCategory} label={w.statusLabel} />
                        {w.nickname && (
                          <span className="text-text-muted text-[10px]">Nick: {w.nickname}</span>
                        )}
                      </div>
                      <p className="text-text-primary text-sm truncate">
                        {w.discordDisplayName || w.discordName || w.discordId}
                      </p>
                      <p className="text-text-muted text-[10px]">{w.discordId}</p>
                      {w.reason && (
                        <p className="text-text-secondary text-xs mt-1 break-words">{w.reason}</p>
                      )}
                    </div>
                  </div>
                  <span className="text-text-muted text-[10px] flex-shrink-0">
                    {fmtDate(w.timestamp)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {transcriptId && (
        <TranscriptViewer ticketId={transcriptId} api={api} onClose={() => setTranscriptId(null)} />
      )}
    </>
  )
}

function TranscriptViewer({ ticketId, onClose, api }) {
  const [messages, setMessages] = useState(null)
  const [users, setUsers] = useState({})
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
        const trimmed = content.trim()
        if (trimmed.startsWith('[')) {
          try { setMessages(JSON.parse(trimmed)) } catch { setRawHtml(content) }
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

  function renderContent(text) {
    if (!text) return null
    const parts = []
    const regex = /<@!?(\d+)>|<@&(\d+)>|<#(\d+)>/g
    let lastIndex = 0
    let match
    while ((match = regex.exec(text)) !== null) {
      if (match.index > lastIndex) parts.push(text.slice(lastIndex, match.index))
      if (match[1]) {
        const u = users[match[1]]
        parts.push(
          <span key={match.index} className="inline-flex items-center gap-1 bg-[#5865F2]/20 text-[#5865F2] px-1.5 py-0.5 rounded text-xs font-medium">
            @{u?.displayName || u?.name || match[1]}
          </span>
        )
      } else if (match[3]) {
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

  function getAuthorAvatar(authorId) { return users[authorId]?.avatar || null }
  function getAuthorDisplayName(authorId, fallback) {
    const u = users[authorId]
    return u?.displayName || u?.name || fallback || 'Desconhecido'
  }

  const COLORS = [
    { bg: '#5865F2', text: '#7983F5' },
    { bg: '#57F287', text: '#57F287' },
    { bg: '#FEE75C', text: '#E5D04E' },
    { bg: '#EB459E', text: '#EB459E' },
    { bg: '#ED4245', text: '#ED4245' },
    { bg: '#FF7B3A', text: '#FF7B3A' },
    { bg: '#3BA5B9', text: '#3BA5B9' },
    { bg: '#9B59B6', text: '#B07CC6' },
  ]
  function authorColor(authorId) {
    if (!authorId) return COLORS[0]
    let hash = 0
    for (let i = 0; i < authorId.length; i++) hash = (hash * 31 + authorId.charCodeAt(i)) | 0
    return COLORS[Math.abs(hash) % COLORS.length]
  }

  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-[70] p-2 sm:p-4 animate-backdrop" onClick={onClose}>
      <div className="bg-parchment-dark border border-border rounded-xl w-full max-w-4xl h-[90vh] flex flex-col animate-fade-in overflow-hidden"
        onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between p-4 border-b border-border">
          <h3 className="text-text-primary font-bold flex items-center gap-2">
            <ScrollText size={16} className="text-gold" />
            Transcrição — Ticket #{ticketId}
          </h3>
          <div className="flex items-center gap-2">
            {messages && <span className="text-text-muted text-xs">{messages.length} mensagens</span>}
            <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-parchment-lighter transition-colors">
              <X size={18} className="text-text-muted" />
            </button>
          </div>
        </div>
        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="flex items-center justify-center h-full text-text-muted text-sm">Carregando transcrição...</div>
          ) : error ? (
            <div className="flex items-center justify-center h-full text-blood-light text-sm">{error}</div>
          ) : messages ? (
            <div className="p-4 space-y-4">
              {groupMessages(messages).map((group, gi) => {
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
                        {(group.author || '?')[0].toUpperCase()}
                      </div>
                    )}
                    <div className="min-w-0 flex-1">
                      <div className="flex items-baseline gap-2 mb-0.5">
                        <span className="text-sm font-semibold" style={{ color: colors.text }}>{displayName}</span>
                        <span className="text-text-muted text-[10px]">{fmtMsgTime(group.messages[0].timestamp)}</span>
                      </div>
                      <div className="space-y-0.5">
                        {group.messages.map((m, mi) => {
                          if (!m.content) return null
                          return (
                            <p key={mi} className="text-text-secondary text-sm break-words leading-relaxed">
                              {renderContent(m.content)}
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
export default function StaffPage() {
  const api = useApi()
  const { hasRole } = useAuth()
  const canViewTickets = hasRole(...TICKET_VIEW_ROLES)
  const [staffList, setStaffList] = useState([])
  const [loading, setLoading] = useState(true)
  const [selectedStaff, setSelectedStaff] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    load()
    const interval = setInterval(load, 30000)
    return () => clearInterval(interval)
  }, [])

  async function load() {
    try {
      const data = await api.get('/staff')
      setStaffList(data.staff || [])
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <div className="text-center py-12 text-text-muted">Carregando...</div>
  }

  if (staffList.length === 0) {
    return (
      <div className="rpg-card p-12 text-center">
        <SwordIcon size={40} className="mx-auto mb-3 opacity-30" />
        <p className="text-text-muted">Nenhuma estatística de staff encontrada.</p>
      </div>
    )
  }

  // Filtro de busca
  const filteredStaff = searchQuery.length >= 2
    ? staffList.filter(s =>
        (s.displayName || s.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.discordId.includes(searchQuery)
      )
    : staffList

  // Agrupar por cargo principal
  const grouped = []
  const seen = new Set()
  for (const staff of filteredStaff) {
    const role = staff.primaryRole || { id: 'unknown', name: 'Outros', color: '#888888', key: 'OTHER' }
    const key = role.key || role.id
    if (!seen.has(key)) {
      seen.add(key)
      grouped.push({ role, members: [] })
    }
    grouped.find(g => (g.role.key || g.role.id) === key).members.push(staff)
  }

  return (
    <div className="space-y-5">
      {/* Resumo */}
      <div className="rpg-card p-4 flex items-center gap-3 animate-fade-in mc-border-top">
        <Users size={18} className="text-gold" />
        <span className="text-text-primary font-semibold text-sm">{staffList.length} membros da Staff</span>
        <span className="text-text-muted text-xs">em {grouped.length} {grouped.length === 1 ? 'categoria' : 'categorias'}</span>
      </div>

      {/* Barra de busca */}
      {staffList.length > 5 && (
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Pesquisar ajudante por nome..."
            className="rpg-input w-full pl-9 py-2.5 text-sm"
          />
        </div>
      )}

      {/* Categorias */}
      {grouped.map((group, gi) => (
        <RoleCategory key={group.role.key || group.role.id} group={group} index={gi} onClickStaff={canViewTickets ? setSelectedStaff : null} />
      ))}

      {searchQuery.length >= 2 && filteredStaff.length === 0 && (
        <div className="rpg-card p-8 text-center">
          <p className="text-text-muted">Nenhum membro encontrado para "{searchQuery}"</p>
        </div>
      )}

      {/* Modal: Atividades do Staff */}
      {selectedStaff && (
        <StaffActivityModal
          staff={selectedStaff}
          onClose={() => setSelectedStaff(null)}
          api={api}
        />
      )}
    </div>
  )
}

function RoleCategory({ group, index, onClickStaff }) {
  const [expanded, setExpanded] = useState(true)
  const color = group.role.color || '#c8a84e'

  return (
    <div className="rpg-card overflow-hidden animate-stagger-in" style={{ animationDelay: `${index * 80}ms` }}>
      {/* Header da categoria */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between p-4 hover:bg-parchment-light/50 transition-colors"
      >
        <div className="flex items-center gap-3">
          <div
            className="w-10 h-10 rounded-lg flex items-center justify-center border"
            style={{ background: color + '18', borderColor: color + '35' }}
          >
            <ShieldPixelIcon size={20} style={{ color }} />
          </div>
          <div className="text-left">
            <h3 className="font-bold text-sm" style={{ color }}>{group.role.name}</h3>
            <p className="text-text-muted text-[10px]">
              {group.members.length} {group.members.length === 1 ? 'membro' : 'membros'}
            </p>
          </div>
        </div>
        {expanded
          ? <ChevronUp size={16} className="text-text-muted" />
          : <ChevronDown size={16} className="text-text-muted" />
        }
      </button>

      {/* Grid de membros */}
      {expanded && (
        <div className="px-4 pb-4 animate-slide-down">
          <div className="rpg-divider" />
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3 mt-4">
            {group.members.map((staff, i) => (
              <StaffCard key={staff.discordId} staff={staff} index={i} onClick={onClickStaff ? () => onClickStaff(staff) : null} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function StaffCard({ staff, index, onClick }) {
  const Tag = onClick ? 'button' : 'div'
  return (
    <Tag
      onClick={onClick || undefined}
      className={`bg-parchment rounded-lg p-4 border border-border transition-all group animate-stagger-in text-left w-full ${onClick ? 'hover:border-gold/40 hover:shadow-[0_0_12px_rgba(200,168,78,0.1)] cursor-pointer' : ''}`}
      style={{ animationDelay: `${index * 50}ms` }}
      title={onClick ? 'Clique para ver tickets e whitelists respondidas' : undefined}
    >
      <div className="flex items-center gap-3 mb-3">
        {staff.avatar ? (
          <img src={staff.avatar} alt="" className="w-11 h-11 rounded-xl border-2 border-border group-hover:border-gold/30 transition-colors" />
        ) : (
          <div className="w-11 h-11 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center">
            <User size={18} className="text-text-muted" />
          </div>
        )}
        <div className="min-w-0">
          <p className="text-text-primary font-semibold text-sm truncate group-hover:text-gold transition-colors">{staff.displayName || staff.name || staff.discordId}</p>
          {staff.roles && staff.roles.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-1">
              {staff.roles.slice(0, 3).map(r => (
                <span
                  key={r.id}
                  className="text-[9px] px-1.5 py-0.5 rounded border"
                  style={{ color: r.color, borderColor: r.color + '40', background: r.color + '15' }}
                >
                  {r.name}
                </span>
              ))}
            </div>
          )}
        </div>
        {onClick && <Eye size={14} className="text-text-muted opacity-0 group-hover:opacity-100 transition-opacity ml-auto flex-shrink-0" />}
      </div>

      {staff.stats && Object.keys(staff.stats).length > 0 ? (
        <div className="grid grid-cols-2 gap-1.5">
          {Object.entries(staff.stats).map(([key, val]) => (
            <div key={key} className="bg-parchment-lighter/50 rounded-md p-1.5 text-center">
              <p className="text-sm font-bold text-text-primary">{val}</p>
              <p className="text-text-muted text-[9px]">{key}</p>
            </div>
          ))}
        </div>
      ) : (
        <p className="text-text-muted text-xs">Sem estatísticas disponíveis</p>
      )}
    </Tag>
  )
}
