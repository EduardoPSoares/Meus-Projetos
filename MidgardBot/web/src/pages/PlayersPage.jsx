import { useState, useEffect, useRef, useCallback } from 'react'
import { useApi } from '../hooks/useApi'
import { Search, User, Shield, ScrollText, AlertTriangle, ExternalLink, Gamepad2, Ticket, Clock, Eye, X } from 'lucide-react'
import { PickaxeIcon } from '../assets/minecraft-icons'

function useDebounce(value, delay) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}

export default function PlayersPage() {
  const api = useApi()
  const [query, setQuery] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [results, setResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [selected, setSelected] = useState(null)
  const [profile, setProfile] = useState(null)
  const [loadingProfile, setLoadingProfile] = useState(false)
  const [userTickets, setUserTickets] = useState([])
  const [ticketsLoading, setTicketsLoading] = useState(false)
  const [highlightIdx, setHighlightIdx] = useState(-1)
  const [transcriptId, setTranscriptId] = useState(null)
  const suggestionsRef = useRef(null)
  const inputRef = useRef(null)

  const debouncedQuery = useDebounce(query, 300)

  // Autocomplete: busca sugestões conforme digita
  useEffect(() => {
    if (debouncedQuery.length < 2) {
      setSuggestions([])
      setShowSuggestions(false)
      return
    }
    let cancelled = false
    ;(async () => {
      try {
        const data = await api.get(`/players/search?q=${encodeURIComponent(debouncedQuery)}`)
        if (!cancelled) {
          setSuggestions(data.players || [])
          setShowSuggestions(true)
          setHighlightIdx(-1)
        }
      } catch (e) {
        console.error(e)
      }
    })()
    return () => { cancelled = true }
  }, [debouncedQuery])

  // Fecha dropdown ao clicar fora
  useEffect(() => {
    function handleClickOutside(e) {
      if (suggestionsRef.current && !suggestionsRef.current.contains(e.target) &&
          inputRef.current && !inputRef.current.contains(e.target)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  function handleKeyDown(e) {
    if (!showSuggestions || suggestions.length === 0) {
      if (e.key === 'Enter') handleSearch()
      return
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setHighlightIdx(i => Math.min(i + 1, suggestions.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlightIdx(i => Math.max(i - 1, 0))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      if (highlightIdx >= 0 && highlightIdx < suggestions.length) {
        pickSuggestion(suggestions[highlightIdx])
      } else {
        handleSearch()
      }
    } else if (e.key === 'Escape') {
      setShowSuggestions(false)
    }
  }

  function pickSuggestion(player) {
    setQuery(player.displayName || player.name || player.nickname || '')
    setShowSuggestions(false)
    setSuggestions([])
    setResults([player])
    selectPlayer(player)
  }

  async function handleSearch() {
    if (query.length < 2) return
    setShowSuggestions(false)
    setSearching(true)
    try {
      const data = await api.get(`/players/search?q=${encodeURIComponent(query)}`)
      setResults(data.players || [])
    } catch (e) {
      console.error(e)
    } finally {
      setSearching(false)
    }
  }

  async function selectPlayer(player) {
    setSelected(player)
    setLoadingProfile(true)
    setUserTickets([])
    try {
      const data = await api.get(`/players/${player.discordId}`)
      setProfile(data)
    } catch (e) {
      console.error(e)
      setProfile(null)
    } finally {
      setLoadingProfile(false)
    }
    setTicketsLoading(true)
    try {
      const tData = await api.get(`/tickets/user/${player.discordId}`)
      setUserTickets(tData.tickets || [])
    } catch (e) {
      console.error(e)
    } finally {
      setTicketsLoading(false)
    }
  }

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Search with autocomplete */}
      <div className="rpg-card !overflow-visible p-4 sm:p-5 animate-slide-down">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative" ref={inputRef}>
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted z-10" />
            <input
              type="text"
              value={query}
              onChange={e => { setQuery(e.target.value); if (e.target.value.length < 2) setShowSuggestions(false) }}
              onKeyDown={handleKeyDown}
              onFocus={() => { if (suggestions.length > 0 && query.length >= 2) setShowSuggestions(true) }}
              placeholder="Buscar por nome ou nick..."
              className="rpg-input w-full pl-10"
              autoComplete="off"
            />
            {/* Dropdown de sugestões */}
            {showSuggestions && suggestions.length > 0 && (
              <div
                ref={suggestionsRef}
                className="absolute left-0 right-0 top-full mt-1 z-[999] rounded-xl border border-border shadow-xl max-h-72 overflow-y-auto"
                style={{ background: 'linear-gradient(160deg, var(--color-parchment) 0%, var(--color-parchment-light) 100%)' }}
              >
                {suggestions.map((p, idx) => (
                  <button
                    key={p.discordId}
                    onClick={() => pickSuggestion(p)}
                    onMouseEnter={() => setHighlightIdx(idx)}
                    className={`w-full flex items-center gap-3 px-3 py-2.5 text-left transition-colors
                      ${idx === highlightIdx ? 'bg-gold/10' : 'hover:bg-parchment-lighter'}`}
                  >
                    {p.avatar ? (
                      <img src={p.avatar} alt="" className="w-8 h-8 rounded-lg border border-border flex-shrink-0" />
                    ) : (
                      <div className="w-8 h-8 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center flex-shrink-0">
                        <User size={14} className="text-text-muted" />
                      </div>
                    )}
                    <div className="min-w-0">
                      <p className="text-text-primary font-medium text-sm truncate">{p.displayName || p.name}</p>
                      <div className="flex items-center gap-2">
                        <span className="text-text-muted text-xs truncate">{p.name}</span>
                        {p.nickname && (
                          <span className="text-emerald-400 text-xs flex items-center gap-1 flex-shrink-0">
                            <PickaxeIcon size={10} /> {p.nickname}
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
          <button onClick={handleSearch} className="rpg-button whitespace-nowrap" disabled={searching}>
            {searching ? 'Buscando...' : 'Buscar'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 sm:gap-6">
        {/* Results list */}
        <div className="lg:col-span-1 space-y-2">
          {results.length === 0 ? (
            <div className="rpg-card p-8 text-center">
              <Search size={32} className="mx-auto text-text-muted mb-2 opacity-40" />
              <p className="text-text-muted text-sm">Busque um jogador pelo nome</p>
            </div>
          ) : (
            results.map(p => (
              <button
                key={p.discordId}
                onClick={() => selectPlayer(p)}
                className={`w-full rpg-card p-4 flex items-center gap-3 text-left transition-all animate-stagger-in
                  ${selected?.discordId === p.discordId ? 'border-gold/40 rpg-glow' : ''}`}
                style={{ animationDelay: `${results.indexOf(p) * 60}ms` }}
              >
                {p.avatar ? (
                  <img src={p.avatar} alt="" className="w-10 h-10 rounded-lg border border-border" />
                ) : (
                  <div className="w-10 h-10 rounded-lg bg-parchment-lighter border border-border flex items-center justify-center">
                    <User size={16} className="text-text-muted" />
                  </div>
                )}
                <div>
                  <p className="text-text-primary font-medium text-sm">{p.displayName || p.name}</p>
                  <p className="text-text-muted text-xs">{p.name}</p>
                  {p.nickname && (
                    <p className="text-emerald-400 text-xs flex items-center gap-1"><PickaxeIcon size={10} /> {p.nickname}</p>
                  )}
                </div>
              </button>
            ))
          )}
        </div>

        {/* Player profile */}
        <div className="lg:col-span-2">
          {!selected ? (
            <div className="rpg-card p-12 text-center">
              <User size={48} className="mx-auto text-text-muted mb-3 opacity-30" />
              <p className="text-text-muted">Selecione um jogador para ver o perfil</p>
            </div>
          ) : loadingProfile ? (
            <div className="rpg-card p-12 text-center text-text-muted">Carregando perfil...</div>
          ) : profile ? (
            <div className="space-y-4 animate-fade-in">
              {/* Profile header */}
              <div className="rpg-card rpg-glow p-6 animate-slide-up">
                <div className="flex items-center gap-4">
                  {profile.avatar ? (
                    <img src={profile.avatar} alt="" className="w-16 h-16 rounded-xl border-2 border-gold/30" />
                  ) : (
                    <div className="w-16 h-16 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center">
                      <User size={24} className="text-text-muted" />
                    </div>
                  )}
                  <div>
                    <h3 className="text-xl font-bold text-text-primary">{profile.displayName || profile.name}</h3>
                    <p className="text-text-muted text-xs font-mono">{profile.discordId}</p>
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      {profile.roles?.slice(0, 5).map(r => (
                        <span
                          key={r.id}
                          className="text-[10px] px-2 py-0.5 rounded-full border"
                          style={{ color: r.color, borderColor: r.color + '40', background: r.color + '15' }}
                        >
                          {r.name}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              {/* Status cards */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2 sm:gap-3">
                {profile.whitelistNickname && (
                  <div className="rpg-card p-3 text-center animate-stagger-in" style={{ animationDelay: '100ms' }}>
                    <Gamepad2 size={16} className="mx-auto text-emerald-400 mb-1" />
                    <p className="text-xs text-text-muted">Nick In-Game</p>
                    <p className="text-sm font-semibold text-text-primary">{profile.whitelistNickname}</p>
                  </div>
                )}
                <div className="rpg-card p-3 text-center animate-stagger-in" style={{ animationDelay: '150ms' }}>
                  <ScrollText size={16} className="mx-auto text-gold mb-1" />
                  <p className="text-xs text-text-muted">Whitelist</p>
                  <p className={`text-sm font-semibold capitalize ${
                    profile.whitelistStatus === 'approved' ? 'text-emerald-400' :
                    profile.whitelistStatus === 'rejected' ? 'text-blood-light' :
                    profile.whitelistStatus === 'pending' ? 'text-amber-400' : 'text-text-muted'
                  }`}>{{
                    approved: 'Aprovada', rejected: 'Rejeitada', pending: 'Pendente', none: 'Nenhuma'
                  }[profile.whitelistStatus] || profile.whitelistStatus}</p>
                </div>
                <div className="rpg-card p-3 text-center animate-stagger-in" style={{ animationDelay: '200ms' }}>
                  <Shield size={16} className="mx-auto text-blood-light mb-1" />
                  <p className="text-xs text-text-muted">Punições</p>
                  <p className="text-sm font-semibold text-text-primary">{profile.punishments?.length || 0}</p>
                </div>
                <div className="rpg-card p-3 text-center animate-stagger-in" style={{ animationDelay: '250ms' }}>
                  <AlertTriangle size={16} className={`mx-auto mb-1 ${profile.isFlagged ? 'text-blood-light' : 'text-text-muted'}`} />
                  <p className="text-xs text-text-muted">Flagged</p>
                  <p className="text-sm font-semibold text-text-primary">{profile.isFlagged ? 'Sim' : 'Não'}</p>
                </div>
                <div className="rpg-card p-3 text-center animate-stagger-in" style={{ animationDelay: '300ms' }}>
                  <ExternalLink size={16} className={`mx-auto mb-1 ${profile.isBlacklisted ? 'text-blood-light' : 'text-text-muted'}`} />
                  <p className="text-xs text-text-muted">Blacklist</p>
                  <p className="text-sm font-semibold text-text-primary">{profile.isBlacklisted ? 'Sim' : 'Não'}</p>
                </div>
              </div>

              {/* Punishments history */}
              {profile.punishments && profile.punishments.length > 0 && (
                <div className="rpg-card p-4 sm:p-5">
                  <h4 className="text-gold text-xs sm:text-sm font-semibold mb-3 flex items-center gap-2">
                    <Shield size={14} />
                    Histórico de Punições
                  </h4>
                  <div className="space-y-2">
                    {profile.punishments.map(p => (
                      <div key={p.id} className="bg-parchment rounded-lg p-3 flex items-center justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className={`rpg-badge ${
                              p.type === 'ban' || p.type === 'tempban' ? 'rpg-badge-red' :
                              p.type === 'warn' ? 'rpg-badge-gold' : 'rpg-badge-blue'
                            }`}>{p.type}</span>
                            <span className="text-text-secondary text-xs">{formatDate(p.startTime)}</span>
                          </div>
                          <p className="text-text-primary text-sm mt-1">{p.reason || 'Sem motivo'}</p>
                        </div>
                        {p.removedBy ? (
                          <span className="rpg-badge rpg-badge-green text-[10px]">Removida</span>
                        ) : (
                          <span className="rpg-badge rpg-badge-red text-[10px]">Ativa</span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* User Tickets */}
              <div className="rpg-card p-4 sm:p-5">
                <h4 className="text-gold text-xs sm:text-sm font-semibold mb-3 flex items-center gap-2">
                  <Ticket size={14} />
                  Tickets ({userTickets.length})
                </h4>
                {ticketsLoading ? (
                  <p className="text-text-muted text-sm py-4 text-center">Carregando tickets...</p>
                ) : userTickets.length === 0 ? (
                  <p className="text-text-muted text-sm py-4 text-center">Nenhum ticket encontrado</p>
                ) : (
                  <div className="space-y-2">
                    {userTickets.map(t => (
                      <button
                        key={`${t.status}-${t.id}`}
                        onClick={() => t.status === 'closed' && setTranscriptId(t.id)}
                        className={`w-full bg-parchment rounded-lg p-3 flex items-center justify-between text-left transition-colors group ${
                          t.status === 'closed' ? 'hover:bg-parchment-lighter cursor-pointer' : 'cursor-default'
                        }`}
                      >
                        <div className="min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="text-text-muted text-xs font-mono">#{t.id}</span>
                            {t.status === 'open' ? (
                              <span className="rpg-badge rpg-badge-green">Aberto</span>
                            ) : (
                              <span className="rpg-badge rpg-badge-red">Fechado</span>
                            )}
                            <span className={`rpg-badge ${
                              t.priority === 'HIGH' || t.priority === 'URGENT' ? 'rpg-badge-red' : 'rpg-badge-blue'
                            }`}>
                              {t.priority === 'HIGH' ? 'Alta' : t.priority === 'URGENT' ? 'Urgente' : 'Normal'}
                            </span>
                          </div>
                          <p className="text-text-secondary text-xs mt-1 truncate">{t.channelName || '—'}</p>
                        </div>
                        <div className="flex items-center gap-2 flex-shrink-0 ml-2">
                          <span className="text-text-muted text-[10px] flex items-center gap-1">
                            <Clock size={9} />
                            {t.closedAt ? formatDate(t.closedAt) : t.createdAt ? formatDate(t.createdAt) : '—'}
                          </span>
                          {t.status === 'closed' && (
                            <Eye size={12} className="text-text-muted opacity-0 group-hover:opacity-100 transition-opacity" />
                          )}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="rpg-card p-12 text-center text-text-muted">Erro ao carregar perfil</div>
          )}
        </div>
      </div>

      {/* Transcript viewer modal */}
      {transcriptId && (
        <TranscriptViewer ticketId={transcriptId} onClose={() => setTranscriptId(null)} api={api} />
      )}
    </div>
  )
}

function formatDate(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

/* ─── Visualizador de Transcrição ─── */
const AUTHOR_COLORS = [
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
  if (!authorId) return AUTHOR_COLORS[0]
  let hash = 0
  for (let i = 0; i < authorId.length; i++) hash = (hash * 31 + authorId.charCodeAt(i)) | 0
  return AUTHOR_COLORS[Math.abs(hash) % AUTHOR_COLORS.length]
}

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
            {u?.avatar && <img src={u.avatar} alt="" className="w-3.5 h-3.5 rounded-full inline" />}
            @{u?.displayName || u?.name || match[1]}
          </span>
        )
      } else if (match[2]) {
        const r = roles[match[2]]
        const c = r?.color || '#99AAB5'
        parts.push(
          <span key={match.index} className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium" style={{ background: c + '20', color: c }}>
            @{r?.name || match[2]}
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

  const BOT_ID = '891253756862271510'

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
                const isBot = group.authorId === BOT_ID
                const colors = authorColor(group.authorId)
                const avatar = users[group.authorId]?.avatar || null
                const displayName = users[group.authorId]?.displayName || users[group.authorId]?.name || group.author || 'Desconhecido'
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
                        <span className="text-sm font-semibold" style={{ color: colors.text }}>{displayName}</span>
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
