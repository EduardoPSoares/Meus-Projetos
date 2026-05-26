import { useState, useEffect, useRef, useCallback } from 'react'
import { useApi } from '../hooks/useApi'
import { ScrollText, Clock, CheckCircle, XCircle, User, Shield, Calendar, Bot, FileText, Search, X, ChevronLeft, ChevronRight } from 'lucide-react'

function StatusBadge({ status, label }) {
  const map = {
    pending: { label: 'Pendente', cls: 'rpg-badge-gold' },
    approved: { label: 'Aprovada', cls: 'rpg-badge-green' },
    rejected: { label: 'Rejeitada', cls: 'rpg-badge-red' },
  }
  const s = map[status] || { label: status, cls: 'rpg-badge-blue' }
  return <span className={`rpg-badge ${s.cls}`}>{label || s.label}</span>
}

export default function WhitelistPage() {
  const api = useApi()
  const [whitelists, setWhitelists] = useState([])
  const [filter, setFilter] = useState('all')
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState(null)
  const [detail, setDetail] = useState(null)
  const [loadingDetail, setLoadingDetail] = useState(false)

  // Paginação
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [total, setTotal] = useState(0)

  // Busca
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const searchRef = useRef(null)
  const debounceRef = useRef(null)

  useEffect(() => { setPage(1) }, [filter, search])
  useEffect(() => {
    load()
    const interval = setInterval(load, 30000)
    return () => clearInterval(interval)
  }, [filter, page, search])

  // Fechar sugestões ao clicar fora
  useEffect(() => {
    function handleClick(e) {
      if (searchRef.current && !searchRef.current.contains(e.target)) setShowSuggestions(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  async function load() {
    setLoading(true)
    try {
      let url = `/whitelists?status=${filter}&page=${page}&limit=50`
      if (search) url += `&search=${encodeURIComponent(search)}`
      const data = await api.get(url)
      setWhitelists(data.whitelists || [])
      setTotalPages(data.pages || 1)
      setTotal(data.total || 0)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  // Buscar sugestões com debounce
  function handleSearchInput(value) {
    setSearchInput(value)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (!value.trim()) {
      setSuggestions([])
      setShowSuggestions(false)
      return
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const data = await api.get(`/whitelists?status=${filter}&search=${encodeURIComponent(value)}&page=1&limit=8`)
        setSuggestions(data.whitelists || [])
        setShowSuggestions(true)
      } catch (_) {
        setSuggestions([])
      }
    }, 300)
  }

  function submitSearch() {
    setSearch(searchInput.trim())
    setShowSuggestions(false)
  }

  function clearSearch() {
    setSearchInput('')
    setSearch('')
    setSuggestions([])
    setShowSuggestions(false)
  }

  function selectSuggestion(wl) {
    setShowSuggestions(false)
    openDetail(wl)
  }

  async function openDetail(wl) {
    setSelected(wl)
    setDetail(null)
    setLoadingDetail(true)
    try {
      const data = await api.get(`/whitelists/${wl.discordId}`)
      setDetail(data)
    } catch (e) {
      console.error(e)
    } finally {
      setLoadingDetail(false)
    }
  }

  function closeDetail() {
    setSelected(null)
    setDetail(null)
  }

  return (
    <div className="space-y-4 sm:space-y-6">
      {/* Search bar */}
      <div ref={searchRef} className="relative animate-fade-in">
        <div className="rpg-card p-3 flex items-center gap-2">
          <Search size={16} className="text-text-muted shrink-0" />
          <input
            type="text"
            value={searchInput}
            onChange={e => handleSearchInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && submitSearch()}
            onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
            placeholder="Buscar por nick, Discord ou ID..."
            className="flex-1 bg-transparent text-text-primary text-sm placeholder:text-text-muted/50 focus:outline-none"
          />
          {searchInput && (
            <button onClick={clearSearch} className="text-text-muted hover:text-text-primary transition-colors">
              <X size={14} />
            </button>
          )}
          <button onClick={submitSearch} className="rpg-button text-xs px-3 py-1.5">Buscar</button>
        </div>

        {/* Sugestões */}
        {showSuggestions && suggestions.length > 0 && (
          <div className="absolute top-full left-0 right-0 mt-1 rpg-card z-40 overflow-hidden border border-border shadow-xl animate-slide-down">
            {suggestions.map(wl => (
              <button
                key={wl.discordId}
                onClick={() => selectSuggestion(wl)}
                className="w-full flex items-center gap-3 px-4 py-2.5 text-left hover:bg-parchment-light/50 transition-colors border-b border-border/30 last:border-0"
              >
                {wl.discordAvatar ? (
                  <img src={wl.discordAvatar} alt="" className="w-7 h-7 rounded-full border border-border" />
                ) : (
                  <div className="w-7 h-7 rounded-full bg-parchment-lighter border border-border flex items-center justify-center">
                    <User size={12} className="text-text-muted" />
                  </div>
                )}
                <div className="min-w-0 flex-1">
                  <p className="text-text-primary text-sm font-medium truncate">
                    {wl.discordDisplayName || wl.discordName || wl.nickname || wl.discordId}
                  </p>
                  <p className="text-text-muted text-[10px] truncate">
                    {wl.discordName && `@${wl.discordName}`}{wl.nickname ? ` · ${wl.nickname}` : ''}
                  </p>
                </div>
                <StatusBadge status={wl.status} />
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-2 animate-slide-down">
        {['all', 'pending', 'approved', 'rejected'].map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`rpg-button-secondary text-xs sm:text-sm
              ${filter === f ? 'active' : ''}`}
          >
            {f === 'all' ? 'Todas' : f === 'pending' ? 'Pendentes' : f === 'approved' ? 'Aprovadas' : 'Rejeitadas'}
          </button>
        ))}
        <span className="ml-auto text-text-muted text-xs sm:text-sm">{total} resultados</span>
      </div>

      {loading ? (
        <div className="text-center py-12 text-text-muted">Carregando...</div>
      ) : whitelists.length === 0 ? (
        <div className="rpg-card p-12 text-center">
          <ScrollText size={40} className="mx-auto text-text-muted mb-3 opacity-50" />
          <p className="text-text-muted">Nenhuma whitelist encontrada.</p>
        </div>
      ) : (
        <div className="rpg-card overflow-hidden animate-fade-in">
          <div className="table-responsive">
          <table className="rpg-table">
            <thead>
              <tr>
                <th>Jogador</th>
                <th>Status</th>
                <th className="hide-mobile">Nickname</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {whitelists.map((wl, i) => (
                <tr key={wl.discordId || i} className="cursor-pointer" onClick={() => openDetail(wl)}>
                  <td>
                    <div className="flex items-center gap-3">
                      {wl.discordAvatar ? (
                        <img src={wl.discordAvatar} alt="" className="w-8 h-8 rounded-full border border-border" />
                      ) : (
                        <div className="w-8 h-8 rounded-full bg-parchment-lighter border border-border flex items-center justify-center">
                          <User size={14} className="text-text-muted" />
                        </div>
                      )}
                      <div className="min-w-0">
                        <p className="text-text-primary font-medium text-sm truncate">
                          {wl.discordDisplayName || wl.discordName || wl.discordId}
                        </p>
                        {wl.discordName && (
                          <p className="text-text-muted text-[10px] truncate">@{wl.discordName}</p>
                        )}
                      </div>
                    </div>
                  </td>
                  <td><StatusBadge status={wl.status} /></td>
                  <td className="text-text-muted font-mono text-xs hide-mobile">{wl.nickname || '—'}</td>
                  <td className="text-right">
                    <button className="text-gold/60 hover:text-gold text-xs transition-colors whitespace-nowrap">
                      Detalhes →
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        </div>
      )}

      {/* Paginação */}
      {!loading && totalPages > 1 && (
        <div className="flex justify-center items-center gap-3 animate-fade-in">
          <button
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page <= 1}
            className="rpg-button text-xs disabled:opacity-30 flex items-center gap-1"
          >
            <ChevronLeft size={14} /> Anterior
          </button>
          <span className="text-text-muted text-xs">
            Página {page} de {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page >= totalPages}
            className="rpg-button text-xs disabled:opacity-30 flex items-center gap-1"
          >
            Próxima <ChevronRight size={14} />
          </button>
        </div>
      )}

      {/* Detail modal */}
      {selected && (
        <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4 animate-backdrop" onClick={closeDetail}>
          <div className="rpg-card rpg-glow p-4 sm:p-6 max-w-3xl w-full max-h-[90vh] overflow-y-auto animate-scale-in mc-border-top" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gold flex items-center gap-2">
                <FileText size={20} />
                Detalhes da Whitelist
              </h3>
              <button onClick={closeDetail} className="text-text-muted hover:text-text-primary text-xl">✕</button>
            </div>
            <div className="rpg-divider"></div>

            {loadingDetail ? (
              <div className="text-center py-12 text-text-muted">Carregando detalhes...</div>
            ) : detail ? (
              <div className="space-y-5 mt-4">
                {/* Header com avatar e info básica */}
                <div className="flex items-center gap-4 bg-parchment rounded-xl p-4">
                  {detail.discordAvatar ? (
                    <img src={detail.discordAvatar} alt="" className="w-14 h-14 rounded-xl border-2 border-gold/30" />
                  ) : (
                    <div className="w-14 h-14 rounded-xl bg-parchment-lighter border-2 border-border flex items-center justify-center">
                      <User size={24} className="text-text-muted" />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-text-primary font-semibold text-lg">{detail.discordName || detail.discordId}</p>
                    <p className="text-text-muted text-xs font-mono">{detail.discordId}</p>
                  </div>
                  <StatusBadge status={detail.status} label={detail.statusLabel} />
                </div>

                {/* Info grid */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-parchment rounded-lg p-3 text-center">
                    <p className="text-text-muted text-xs mb-1 flex items-center justify-center gap-1"><User size={12} /> Nickname</p>
                    <p className="text-text-primary text-sm font-semibold">{detail.nickname || 'N/A'}</p>
                  </div>
                  <div className="bg-parchment rounded-lg p-3 text-center">
                    <p className="text-text-muted text-xs mb-1 flex items-center justify-center gap-1"><Calendar size={12} /> Data</p>
                    <p className="text-text-primary text-sm font-semibold">{detail.timestamp || 'N/A'}</p>
                  </div>
                  <div className="bg-parchment rounded-lg p-3 text-center">
                    <p className="text-text-muted text-xs mb-1 flex items-center justify-center gap-1"><Shield size={12} /> Staff</p>
                    <p className="text-text-primary text-sm font-semibold">{detail.staffName || (detail.staffId ? detail.staffId : 'N/A')}</p>
                  </div>
                  {detail.aiScore && (
                    <div className="bg-parchment rounded-lg p-3 text-center">
                      <p className="text-text-muted text-xs mb-1 flex items-center justify-center gap-1"><Bot size={12} /> Score IA</p>
                      <p className="text-gold text-sm font-bold">{detail.aiScore}</p>
                    </div>
                  )}
                  {detail.termsAccepted && (
                    <div className="bg-parchment rounded-lg p-3 text-center">
                      <p className="text-text-muted text-xs mb-1">Termos</p>
                      <p className="text-green-400 text-sm font-semibold">✅ Aceito</p>
                    </div>
                  )}
                </div>

                {/* Motivo/Observação */}
                {detail.reason && (
                  <div className="bg-parchment rounded-lg p-4">
                    <p className="text-text-muted text-xs mb-2 font-semibold uppercase tracking-wide">Motivo / Observação</p>
                    <p className="text-text-primary text-sm whitespace-pre-wrap">{detail.reason}</p>
                  </div>
                )}

                {/* Seções de perguntas/respostas */}
                {detail.sections && detail.sections.map((section, si) => (
                  <div key={si}>
                    <div className="rpg-divider"></div>
                    <h4 className="text-gold text-sm font-semibold mt-4 mb-3">{section.title}</h4>
                    <div className="space-y-3">
                      {section.fields.map((field, fi) => (
                        <div key={fi} className="bg-parchment rounded-lg p-3">
                          <p className="text-gold/70 text-xs mb-1.5 font-medium">📝 {field.question}</p>
                          <p className="text-text-primary text-sm whitespace-pre-wrap leading-relaxed">
                            {field.answer || <span className="text-text-muted italic">Sem resposta</span>}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-12 text-text-muted">Erro ao carregar detalhes.</div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
