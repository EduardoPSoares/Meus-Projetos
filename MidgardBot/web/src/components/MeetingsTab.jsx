import { useState, useEffect, useRef, useMemo, useCallback } from 'react'
import { useApi } from '../hooks/useApi'
import {
  Mic, Clock, Users, Calendar, Download, Trash2, Save,
  Play, Pause, Volume2, VolumeX, ChevronLeft, ChevronRight,
  FileAudio, Edit3, X, CheckCircle, AlertTriangle, Search, Radio, Headphones
} from 'lucide-react'

export default function MeetingsTab() {
  const api = useApi()
  const [meetings, setMeetings] = useState([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [activeRecordings, setActiveRecordings] = useState([])
  const limit = 10

  const activeIds = useMemo(() => new Set(activeRecordings.map(r => r.meetingId)), [activeRecordings])

  const loadActive = useCallback(async () => {
    try {
      const data = await api.get('/meetings/active')
      setActiveRecordings(data.active || [])
    } catch { /* silencioso */ }
  }, [])

  useEffect(() => {
    loadMeetings()
    const interval = setInterval(loadMeetings, 30000)
    return () => clearInterval(interval)
  }, [page])

  useEffect(() => {
    loadActive()
    const interval = setInterval(loadActive, 15000)
    return () => clearInterval(interval)
  }, [loadActive])

  async function loadMeetings() {
    setLoading(true)
    try {
      const data = await api.get(`/meetings?limit=${limit}&offset=${page * limit}`)
      setMeetings(data.meetings || [])
      setTotal(data.total || 0)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  function handleDelete(id) {
    setModal({ type: 'confirm-delete', id })
  }

  async function confirmDelete() {
    if (!modal?.id) return
    setDeleting(true)
    try {
      await api.del(`/meetings/${modal.id}`)
      setModal({ type: 'success', message: 'Reunião excluída com sucesso!' })
      await loadMeetings()
    } catch (e) {
      setModal({ type: 'error', message: e.message || 'Erro ao excluir' })
    } finally {
      setDeleting(false)
    }
  }

  const totalPages = Math.ceil(total / limit)

  function formatDuration(seconds) {
    if (!seconds) return '—'
    const h = Math.floor(seconds / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    const s = seconds % 60
    if (h > 0) return `${h}h ${String(m).padStart(2, '0')}min`
    if (m > 0) return `${m}min ${String(s).padStart(2, '0')}s`
    return `${s}s`
  }

  function formatDate(dateStr) {
    if (!dateStr) return '—'
    try {
      const d = new Date(dateStr.replace(' ', 'T'))
      return d.toLocaleString('pt-BR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      })
    } catch { return dateStr }
  }

  function formatFileSize(bytes) {
    if (!bytes) return '—'
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  if (loading && meetings.length === 0) {
    return <div className="text-center py-12 text-text-muted">Carregando reuniões...</div>
  }

  if (!loading && meetings.length === 0 && page === 0) {
    return (
      <div className="rpg-card p-12 text-center">
        <Mic size={40} className="mx-auto mb-3 opacity-20 text-text-muted" />
        <p className="text-text-muted">Nenhuma reunião gravada ainda.</p>
        <p className="text-text-muted text-xs mt-1">
          Use o comando <code className="text-gold bg-gold/10 px-1.5 py-0.5 rounded">/reuniao iniciar</code> no Discord para gravar.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Banner de gravação ativa */}
      {activeRecordings.length > 0 && (
        <div className="rpg-card p-4 border-2 border-blood/40 bg-blood/5 animate-pulse-subtle">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-blood/20 border border-blood/40 flex items-center justify-center shrink-0">
              <Radio size={20} className="text-blood-light animate-pulse" />
            </div>
            <div className="min-w-0 flex-1">
              <h4 className="text-blood-light font-bold text-sm flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-blood-light animate-pulse" />
                Gravação em andamento
              </h4>
              <div className="flex flex-wrap gap-x-4 gap-y-1 mt-1">
                {activeRecordings.map(r => (
                  <span key={r.meetingId} className="text-text-secondary text-xs flex items-center gap-1.5">
                    <Mic size={10} className="text-blood-light" />
                    Reunião #{r.meetingId} — {r.participantCount} participante{r.participantCount !== 1 ? 's' : ''}
                    {r.durationSeconds > 0 && <span className="text-text-muted">({formatDuration(r.durationSeconds)})</span>}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Stats resumo */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <StatCard icon={<Mic size={16} />} label="Total" value={total} />
        <StatCard icon={<Clock size={16} />} label="Última" value={meetings[0] ? formatDate(meetings[0].startedAt).split(' ')[0] : '—'} />
        <StatCard
          icon={<Users size={16} />}
          label="Participantes (média)"
          value={meetings.length > 0 ? Math.round(meetings.reduce((s, m) => s + (m.participantCount || 0), 0) / meetings.length) : 0}
        />
        <StatCard
          icon={<FileAudio size={16} />}
          label="Armazenamento"
          value={formatFileSize(meetings.reduce((s, m) => s + (m.fileSizeBytes || 0), 0))}
        />
      </div>

      {/* Lista de reuniões */}
      <div className="space-y-3">
        {meetings.map(m => (
          <MeetingCard
            key={m.id}
            meeting={m}
            isActive={activeIds.has(m.id)}
            formatDuration={formatDuration}
            formatDate={formatDate}
            formatFileSize={formatFileSize}
            onDelete={() => handleDelete(m.id)}
            onNotesUpdated={loadMeetings}
          />
        ))}
      </div>

      {/* Paginação */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-3">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page <= 0}
            className="rpg-button text-xs disabled:opacity-30 flex items-center gap-1"
          >
            <ChevronLeft size={14} /> Anterior
          </button>
          <span className="text-text-secondary text-xs">
            Página {page + 1} de {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="rpg-button text-xs disabled:opacity-30 flex items-center gap-1"
          >
            Próxima <ChevronRight size={14} />
          </button>
        </div>
      )}

      {/* Modal */}
      {modal && (
        <div className="fixed inset-0 bg-black/70 z-50 flex items-center justify-center p-4 animate-backdrop" onClick={() => !deleting && setModal(null)}>
          <div className="rpg-card p-6 max-w-sm w-full animate-scale-in mc-border-top text-center" onClick={e => e.stopPropagation()}>
            {modal.type === 'success' && (
              <>
                <div className="w-14 h-14 rounded-full bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center mx-auto mb-4">
                  <CheckCircle size={28} className="text-emerald-400" />
                </div>
                <p className="text-text-secondary text-sm mb-5">{modal.message}</p>
                <button onClick={() => setModal(null)} className="rpg-button text-sm px-6">OK</button>
              </>
            )}
            {modal.type === 'error' && (
              <>
                <div className="w-14 h-14 rounded-full bg-blood/15 border border-blood/30 flex items-center justify-center mx-auto mb-4">
                  <AlertTriangle size={28} className="text-blood-light" />
                </div>
                <p className="text-text-secondary text-sm mb-5">{modal.message}</p>
                <button onClick={() => setModal(null)} className="rpg-button text-sm px-6">OK</button>
              </>
            )}
            {modal.type === 'confirm-delete' && (
              <>
                <div className="w-14 h-14 rounded-full bg-blood/15 border border-blood/30 flex items-center justify-center mx-auto mb-4">
                  <Trash2 size={28} className="text-blood-light" />
                </div>
                <h3 className="text-text-primary font-bold text-base mb-1">Excluir Reunião</h3>
                <p className="text-text-secondary text-sm mb-5">A gravação será excluída permanentemente. Continuar?</p>
                <div className="flex items-center justify-center gap-3">
                  <button onClick={() => setModal(null)} disabled={deleting}
                    className="px-5 py-2 rounded-lg text-sm font-medium bg-parchment border border-border text-text-secondary hover:bg-parchment-light transition-colors disabled:opacity-50">
                    Cancelar
                  </button>
                  <button onClick={confirmDelete} disabled={deleting}
                    className="px-5 py-2 rounded-lg text-sm font-medium bg-blood/80 hover:bg-blood text-white border border-blood transition-colors disabled:opacity-50">
                    {deleting ? 'Excluindo...' : 'Excluir'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

/* ========== Stat Card ========== */
function StatCard({ icon, label, value }) {
  return (
    <div className="rpg-card p-3 flex items-center gap-3">
      <div className="w-9 h-9 rounded-lg bg-gold/10 border border-gold/20 flex items-center justify-center text-gold shrink-0">
        {icon}
      </div>
      <div className="min-w-0">
        <p className="text-text-primary font-bold text-sm truncate">{value}</p>
        <p className="text-text-secondary text-[10px] truncate">{label}</p>
      </div>
    </div>
  )
}

/* ========== Meeting Card ========== */
function MeetingCard({ meeting, isActive, formatDuration, formatDate, formatFileSize, onDelete, onNotesUpdated }) {
  const api = useApi()
  const [expanded, setExpanded] = useState(false)
  const [editingNotes, setEditingNotes] = useState(false)
  const [notes, setNotes] = useState(meeting.notes || '')
  const [savingNotes, setSavingNotes] = useState(false)

  const hasRecording = meeting.recordingFilename && meeting.recordingFilename.length > 0

  async function saveNotes() {
    setSavingNotes(true)
    try {
      await api.put(`/meetings/${meeting.id}/notes`, { notes })
      setEditingNotes(false)
      onNotesUpdated()
    } catch (e) {
      console.error(e)
    } finally {
      setSavingNotes(false)
    }
  }

  function getRecordingUrl(withToken = false, filename = null) {
    const base = import.meta.env.VITE_API_URL || ''
    const file = filename || meeting.recordingFilename
    const url = `${base}/api/meetings/recordings/${file}`
    if (withToken) {
      const stored = localStorage.getItem('midgard_token')
      return stored ? `${url}?token=${encodeURIComponent(stored)}` : url
    }
    return url
  }

  return (
    <div className="rpg-card overflow-hidden animate-stagger-in">
      {/* Header */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-start sm:items-center justify-between p-4 hover:bg-parchment-light/50 transition-colors text-left"
      >
        <div className="flex items-start gap-3 min-w-0">
          <div className="w-10 h-10 rounded-lg bg-gold/10 border border-gold/25 flex items-center justify-center shrink-0">
            <Mic size={18} className="text-gold" />
          </div>
          <div className="min-w-0">
            <h4 className="text-text-primary font-semibold text-sm leading-tight truncate">
              {meeting.title || `Reunião #${meeting.id}`}
            </h4>
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1">
              <span className="text-text-secondary text-[11px] flex items-center gap-1">
                <Calendar size={10} className="text-gold/60" /> {formatDate(meeting.startedAt)}
              </span>
              <span className="text-text-secondary text-[11px] flex items-center gap-1">
                <Clock size={10} className="text-gold/60" /> {formatDuration(meeting.durationSeconds)}
              </span>
              <span className="text-text-secondary text-[11px] flex items-center gap-1">
                <Users size={10} className="text-gold/60" /> {meeting.participantCount || 0} participante{(meeting.participantCount || 0) !== 1 ? 's' : ''}
              </span>
              {meeting.channelName && (
                <span className="text-text-secondary text-[11px] flex items-center gap-1">
                  <Volume2 size={10} className="text-gold/60" /> {meeting.channelName}
                </span>
              )}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2 shrink-0 ml-2">
          {isActive && (
            <span className="text-[10px] font-bold bg-blood/15 border border-blood/30 text-blood-light rounded px-2 py-0.5 flex items-center gap-1 animate-pulse">
              <span className="w-1.5 h-1.5 rounded-full bg-blood-light" />
              GRAVANDO
            </span>
          )}
          {hasRecording && (
            <span className="text-text-secondary text-[10px] bg-parchment border border-border rounded px-2 py-0.5">
              {formatFileSize(meeting.fileSizeBytes)}
            </span>
          )}
          <div className={`w-2 h-2 rounded-full ${hasRecording ? 'bg-emerald-400' : 'bg-text-muted/30'}`} title={hasRecording ? 'Gravação disponível' : 'Sem gravação'} />
        </div>
      </button>

      {/* Conteúdo expandido */}
      {expanded && (
        <div className="px-4 pb-4 space-y-4 animate-slide-down">
          <div className="rpg-divider" />

          {/* Audio Player */}
          {hasRecording && (
            <div className="space-y-2">
              <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider flex items-center gap-1.5">
                <FileAudio size={12} />
                Gravação
              </label>
              <AudioPlayer url={getRecordingUrl(true)} filename={meeting.recordingFilename} />
            </div>
          )}

          {/* Faixas Individuais */}
          {hasRecording && (
            <TrackSection meetingId={meeting.id} getRecordingUrl={getRecordingUrl} />
          )}

          {!hasRecording && (
            <div className="bg-parchment rounded-lg p-3 border border-border text-center">
              <VolumeX size={20} className="mx-auto mb-1 text-text-muted/40" />
              <p className="text-text-muted text-xs">Gravação não disponível</p>
            </div>
          )}

          {/* Participantes */}
          {meeting.participants && (
            <div>
              <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider flex items-center gap-1.5 mb-1.5">
                <Users size={12} />
                Participantes Detectados
              </label>
              <div className="flex flex-wrap gap-1.5">
                {(() => {
                  try {
                    const parsed = typeof meeting.participants === 'string' ? JSON.parse(meeting.participants) : meeting.participants
                    if (Array.isArray(parsed)) {
                      const speakers = parsed.filter(p => typeof p === 'object' && p.speaker)
                      const audience = parsed.filter(p => typeof p === 'object' && !p.speaker)
                      return (
                        <>
                          {speakers.map((p, i) => (
                            <span key={`s-${i}`} className="px-2 py-1 bg-gold/10 border border-gold/25 rounded-lg text-[11px] text-text-secondary flex items-center gap-1" title="Orador">
                              <Mic size={10} className="text-gold" />
                              {p.name || p.username}
                            </span>
                          ))}
                          {audience.map((p, i) => (
                            <span key={`a-${i}`} className="px-2 py-1 bg-parchment border border-border rounded-lg text-[11px] text-text-muted flex items-center gap-1" title="Plateia">
                              <Headphones size={10} className="text-text-muted/60" />
                              {p.name || p.username}
                            </span>
                          ))}
                          {parsed.filter(p => typeof p !== 'object').map((p, i) => (
                            <span key={`x-${i}`} className="px-2 py-1 bg-parchment border border-border rounded-lg text-[11px] text-text-secondary">{p}</span>
                          ))}
                        </>
                      )
                    }
                    return <span className="text-text-muted text-xs">—</span>
                  } catch {
                    return <span className="text-text-muted text-xs">{meeting.participants}</span>
                  }
                })()}
              </div>
            </div>
          )}

          {/* Notas / Observações */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-text-secondary text-xs font-semibold uppercase tracking-wider flex items-center gap-1.5">
                <Edit3 size={12} />
                Observações da Reunião
              </label>
              {!editingNotes && (
                <button onClick={() => setEditingNotes(true)}
                  className="text-[11px] text-gold/70 hover:text-gold transition-colors flex items-center gap-1">
                  <Edit3 size={10} /> Editar
                </button>
              )}
            </div>

            {editingNotes ? (
              <div className="space-y-2">
                <textarea
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                  rows={4}
                  maxLength={5000}
                  spellCheck
                  lang="pt-BR"
                  placeholder="Adicione observações, pautas, decisões..."
                  className="w-full bg-parchment border border-border rounded-lg px-3 py-2.5 text-xs text-text-primary
                             placeholder:text-text-muted/50 focus:border-gold/50 focus:outline-none transition-colors resize-none"
                />
                <div className="flex items-center gap-2">
                  <button onClick={saveNotes} disabled={savingNotes}
                    className="rpg-button text-xs flex items-center gap-1 disabled:opacity-50">
                    <Save size={12} /> {savingNotes ? 'Salvando...' : 'Salvar'}
                  </button>
                  <button onClick={() => { setEditingNotes(false); setNotes(meeting.notes || '') }}
                    className="px-3 py-1.5 rounded-lg text-xs text-text-muted border border-border hover:border-border-light transition-colors">
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <div className="bg-parchment rounded-lg p-3 border border-border min-h-[40px]">
                {meeting.notes ? (
                  <p className="text-text-secondary text-xs leading-relaxed whitespace-pre-wrap">{meeting.notes}</p>
                ) : (
                  <p className="text-text-muted/50 text-xs italic">Nenhuma observação adicionada</p>
                )}
              </div>
            )}
          </div>

          {/* Ações */}
          <div className="flex items-center gap-2 pt-1">
            {hasRecording && (
              <a
                href={getRecordingUrl(true)}
                download={meeting.recordingFilename}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border
                           border-border text-text-secondary hover:text-gold hover:border-gold/30 transition-all"
              >
                <Download size={13} /> Download WAV
              </a>
            )}
            <button onClick={onDelete}
              disabled={isActive}
              title={isActive ? 'Não é possível excluir durante a gravação' : 'Excluir reunião'}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border
                         border-border text-text-muted hover:text-blood-light hover:border-blood/30 transition-all ml-auto
                         disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:text-text-muted disabled:hover:border-border">
              <Trash2 size={13} /> Excluir
            </button>
          </div>

          {/* Informações técnicas */}
          <div className="text-[10px] text-text-secondary/70 flex flex-wrap gap-x-4 gap-y-1">
            <span>ID: #{meeting.id}</span>
            {meeting.startedByName && <span>Iniciada por: {meeting.startedByName}</span>}
            {meeting.channelName && <span>Canal: {meeting.channelName}</span>}
            {meeting.endedAt && <span>Encerrada: {formatDate(meeting.endedAt)}</span>}
          </div>
        </div>
      )}
    </div>
  )
}

/* ========== Audio Player Custom ========== */
function AudioPlayer({ url, filename }) {
  const audioRef = useRef(null)
  const progressRef = useRef(null)
  const [playing, setPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(1)
  const [muted, setMuted] = useState(false)
  const [error, setError] = useState(false)
  const [dragging, setDragging] = useState(false)

  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return

    const onTime = () => { if (!dragging) setCurrentTime(audio.currentTime) }
    const onDur = () => setDuration(audio.duration || 0)
    const onEnd = () => setPlaying(false)
    const onErr = () => setError(true)

    audio.addEventListener('timeupdate', onTime)
    audio.addEventListener('loadedmetadata', onDur)
    audio.addEventListener('ended', onEnd)
    audio.addEventListener('error', onErr)

    return () => {
      audio.removeEventListener('timeupdate', onTime)
      audio.removeEventListener('loadedmetadata', onDur)
      audio.removeEventListener('ended', onEnd)
      audio.removeEventListener('error', onErr)
    }
  }, [dragging])

  function togglePlay() {
    if (!audioRef.current) return
    if (playing) {
      audioRef.current.pause()
    } else {
      audioRef.current.play().catch(() => setError(true))
    }
    setPlaying(!playing)
  }

  function getSeekTime(clientX) {
    if (!progressRef.current || !duration) return null
    const rect = progressRef.current.getBoundingClientRect()
    const x = Math.max(0, Math.min(clientX - rect.left, rect.width))
    return (x / rect.width) * duration
  }

  function handlePointerDown(e) {
    if (!audioRef.current || !duration) return
    e.preventDefault()
    setDragging(true)
    progressRef.current.setPointerCapture(e.pointerId)
    const t = getSeekTime(e.clientX)
    if (t !== null) {
      setCurrentTime(t)
      audioRef.current.currentTime = t
    }
  }

  function handlePointerMove(e) {
    if (!dragging) return
    const t = getSeekTime(e.clientX)
    if (t !== null) {
      setCurrentTime(t)
      audioRef.current.currentTime = t
    }
  }

  function handlePointerUp() {
    setDragging(false)
  }

  function toggleMute() {
    if (!audioRef.current) return
    audioRef.current.muted = !muted
    setMuted(!muted)
  }

  function changeVolume(e) {
    const v = parseFloat(e.target.value)
    setVolume(v)
    if (audioRef.current) audioRef.current.volume = v
    if (v === 0) setMuted(true)
    else setMuted(false)
  }

  function fmt(sec) {
    if (!sec || isNaN(sec)) return '0:00'
    const m = Math.floor(sec / 60)
    const s = Math.floor(sec % 60)
    return `${m}:${String(s).padStart(2, '0')}`
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0

  if (error) {
    return (
      <div className="bg-parchment rounded-lg p-3 border border-blood/20 text-center">
        <p className="text-blood-light text-xs">Erro ao carregar gravação</p>
      </div>
    )
  }

  return (
    <div className="bg-parchment rounded-lg p-3 border border-border">
      <audio ref={audioRef} src={url} preload="metadata" />

      <div className="flex items-center gap-3">
        {/* Play/Pause */}
        <button onClick={togglePlay}
          className="w-9 h-9 rounded-full bg-gold/15 border border-gold/30 flex items-center justify-center
                     text-gold hover:bg-gold/25 transition-all shrink-0">
          {playing ? <Pause size={16} /> : <Play size={16} className="ml-0.5" />}
        </button>

        {/* Time + Progress */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-text-secondary text-[11px] font-mono w-9 text-right shrink-0">{fmt(currentTime)}</span>
            <div
              ref={progressRef}
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              className="flex-1 py-2 cursor-pointer group touch-none select-none"
            >
              <div className="h-1.5 bg-border rounded-full relative">
                <div
                  className="h-full bg-gold rounded-full relative"
                  style={{ width: `${progress}%` }}
                >
                  <div className="absolute right-0 top-1/2 -translate-y-1/2 w-3.5 h-3.5 rounded-full bg-gold
                                border-2 border-parchment shadow-sm opacity-0 group-hover:opacity-100 transition-opacity"
                    style={dragging ? { opacity: 1 } : {}} />
                </div>
              </div>
            </div>
            <span className="text-text-secondary text-[11px] font-mono w-9 shrink-0">{fmt(duration)}</span>
          </div>
        </div>

        {/* Volume */}
        <div className="flex items-center gap-1.5 shrink-0">
          <button onClick={toggleMute} className="text-text-muted hover:text-text-secondary transition-colors">
            {muted || volume === 0 ? <VolumeX size={14} /> : <Volume2 size={14} />}
          </button>
          <input
            type="range"
            min="0"
            max="1"
            step="0.05"
            value={muted ? 0 : volume}
            onChange={changeVolume}
            className="w-16 h-1 accent-gold appearance-none rounded-full bg-border cursor-pointer
                       [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-2.5
                       [&::-webkit-slider-thumb]:h-2.5 [&::-webkit-slider-thumb]:rounded-full
                       [&::-webkit-slider-thumb]:bg-gold"
          />
        </div>
      </div>
    </div>
  )
}

/* ========== Track Section (lazy-load) ========== */
function TrackSection({ meetingId, getRecordingUrl }) {
  const api = useApi()
  const [open, setOpen] = useState(false)
  const [tracks, setTracks] = useState(null)
  const [loading, setLoading] = useState(false)

  async function loadTracks() {
    if (tracks) { setOpen(!open); return }
    setLoading(true)
    try {
      const data = await api.get(`/meetings/${meetingId}/tracks`)
      setTracks(data.tracks || [])
      setOpen(true)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <button onClick={loadTracks} disabled={loading}
        className="text-xs flex items-center gap-1.5 text-gold/70 hover:text-gold transition-colors disabled:opacity-50">
        <Users size={12} />
        {loading ? 'Carregando faixas...' : open ? 'Ocultar faixas individuais' : 'Faixas individuais'}
      </button>
      {open && tracks && tracks.length > 0 && (
        <div className="mt-2">
          <MultiTrackPlayer tracks={tracks} getRecordingUrl={getRecordingUrl} />
        </div>
      )}
      {open && tracks && tracks.length === 0 && (
        <p className="text-text-muted text-xs mt-1">Nenhuma faixa individual gravada nesta reunião.</p>
      )}
    </div>
  )
}

/* ========== Multi-Track Player ========== */
function MultiTrackPlayer({ tracks, getRecordingUrl }) {
  const audioRefs = useRef({})
  const progressRef = useRef(null)
  const rafRef = useRef(null)
  const [playing, setPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [mutedTracks, setMutedTracks] = useState({})
  const [soloTrack, setSoloTrack] = useState(null)
  const [speakingNow, setSpeakingNow] = useState({})

  const [dragging, setDragging] = useState(false)

  // Parse speaking segments
  const segmentsMap = useMemo(() => {
    const map = {}
    for (const t of tracks) {
      try {
        map[t.userId] = t.speakingSegments ? JSON.parse(t.speakingSegments) : []
      } catch { map[t.userId] = [] }
    }
    return map
  }, [tracks])

  // Update speaking indicators via rAF
  useEffect(() => {
    function tick() {
      const first = Object.values(audioRefs.current)[0]
      if (first) {
        const time = first.currentTime * 1000
        setCurrentTime(first.currentTime)
        const speaking = {}
        for (const t of tracks) {
          const segs = segmentsMap[t.userId] || []
          speaking[t.userId] = segs.some(([s, e]) => time >= s && time <= e + 100)
        }
        setSpeakingNow(speaking)
      }
      rafRef.current = requestAnimationFrame(tick)
    }
    rafRef.current = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(rafRef.current)
  }, [tracks, segmentsMap])

  // Detect max duration once audio loads
  function handleLoadedMetadata(trackId) {
    const audio = audioRefs.current[trackId]
    if (audio && audio.duration > duration) {
      setDuration(audio.duration)
    }
  }

  function togglePlay() {
    const audios = Object.values(audioRefs.current)
    if (playing) {
      audios.forEach(a => a.pause())
    } else {
      audios.forEach(a => a.play().catch(() => {}))
    }
    setPlaying(!playing)
  }

  function getSeekTime(clientX) {
    if (!progressRef.current || !duration) return null
    const rect = progressRef.current.getBoundingClientRect()
    const x = Math.max(0, Math.min(clientX - rect.left, rect.width))
    return (x / rect.width) * duration
  }

  function handlePointerDown(e) {
    if (!duration) return
    e.preventDefault()
    setDragging(true)
    progressRef.current.setPointerCapture(e.pointerId)
    const t = getSeekTime(e.clientX)
    if (t !== null) {
      Object.values(audioRefs.current).forEach(a => { a.currentTime = t })
    }
  }

  function handlePointerMove(e) {
    if (!dragging) return
    const t = getSeekTime(e.clientX)
    if (t !== null) {
      Object.values(audioRefs.current).forEach(a => { a.currentTime = t })
    }
  }

  function handlePointerUp() {
    setDragging(false)
  }

  function toggleMute(userId) {
    setMutedTracks(prev => {
      const next = { ...prev, [userId]: !prev[userId] }
      if (audioRefs.current[userId]) audioRefs.current[userId].muted = next[userId]
      return next
    })
  }

  function toggleSolo(userId) {
    const newSolo = soloTrack === userId ? null : userId
    setSoloTrack(newSolo)
    for (const t of tracks) {
      const audio = audioRefs.current[t.userId]
      if (!audio) continue
      if (newSolo) {
        audio.muted = t.userId !== newSolo
      } else {
        audio.muted = !!mutedTracks[t.userId]
      }
    }
  }

  function handleEnded() {
    setPlaying(false)
  }

  function fmt(sec) {
    if (!sec || isNaN(sec)) return '0:00'
    const m = Math.floor(sec / 60)
    const s = Math.floor(sec % 60)
    return `${m}:${String(s).padStart(2, '0')}`
  }

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0

  return (
    <div className="bg-parchment rounded-lg border border-border overflow-hidden">
      {/* Controle de transporte */}
      <div className="p-3 flex items-center gap-3 border-b border-border">
        <button onClick={togglePlay}
          className="w-8 h-8 rounded-full bg-gold/15 border border-gold/30 flex items-center justify-center
                     text-gold hover:bg-gold/25 transition-all shrink-0">
          {playing ? <Pause size={14} /> : <Play size={14} className="ml-0.5" />}
        </button>
        <span className="text-text-secondary text-[11px] font-mono shrink-0">{fmt(currentTime)}</span>
        <div ref={progressRef}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          className="flex-1 py-2 cursor-pointer group touch-none select-none">
          <div className="h-1.5 bg-border rounded-full relative">
            <div className="h-full bg-gold rounded-full relative" style={{ width: `${progress}%` }}>
              <div className="absolute right-0 top-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-gold
                            border-2 border-parchment shadow-sm opacity-0 group-hover:opacity-100 transition-opacity"
                style={dragging ? { opacity: 1 } : {}} />
            </div>
          </div>
        </div>
        <span className="text-text-secondary text-[11px] font-mono shrink-0">{fmt(duration)}</span>
      </div>

      {/* Faixas individuais */}
      <div className="divide-y divide-border">
        {tracks.map(t => {
          const isMuted = soloTrack ? t.userId !== soloTrack : !!mutedTracks[t.userId]
          const isSolo = soloTrack === t.userId
          const isSpeaking = speakingNow[t.userId]

          return (
            <div key={t.userId} className="flex items-center gap-2 px-3 py-2">
              {/* Hidden audio element */}
              <audio
                ref={el => { if (el) audioRefs.current[t.userId] = el }}
                src={getRecordingUrl(true, t.filename)}
                preload="metadata"
                onLoadedMetadata={() => handleLoadedMetadata(t.userId)}
                onEnded={handleEnded}
              />

              {/* Speaking indicator */}
              <div className={`w-2 h-2 rounded-full shrink-0 transition-all duration-150
                ${isSpeaking && !isMuted ? 'bg-emerald-400 shadow-[0_0_6px_rgba(52,211,153,0.6)]' : 'bg-border'}`}
              />

              {/* User name */}
              <span className={`text-xs min-w-0 truncate flex-1 transition-colors
                ${isMuted ? 'text-text-muted/40 line-through' : isSpeaking ? 'text-emerald-400 font-semibold' : 'text-text-secondary'}`}>
                {t.userName}
              </span>

              {/* Mute btn */}
              <button onClick={() => toggleMute(t.userId)} title={mutedTracks[t.userId] ? 'Desmutar' : 'Mutar'}
                className={`p-1 rounded transition-colors ${mutedTracks[t.userId] ? 'text-blood-light' : 'text-text-muted hover:text-text-secondary'}`}>
                {mutedTracks[t.userId] ? <VolumeX size={13} /> : <Volume2 size={13} />}
              </button>

              {/* Solo btn */}
              <button onClick={() => toggleSolo(t.userId)} title={isSolo ? 'Desativar solo' : 'Solo'}
                className={`px-1.5 py-0.5 rounded text-[10px] font-bold border transition-colors
                  ${isSolo ? 'bg-gold/20 border-gold/40 text-gold' : 'border-border text-text-muted hover:text-gold hover:border-gold/30'}`}>
                S
              </button>
            </div>
          )
        })}
      </div>
    </div>
  )
}
