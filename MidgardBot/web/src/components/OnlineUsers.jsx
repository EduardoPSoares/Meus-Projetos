import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { Eye } from 'lucide-react'

const PAGE_LABELS = {
  '/': 'Dashboard',
  '/whitelists': 'Whitelists',
  '/moderation': 'Moderação',
  '/tickets': 'Tickets',
  '/staff': 'Staff',
  '/players': 'Jogadores',
  '/reports': 'Relatórios'
}

const POLL_INTERVAL = 15_000 // 15 segundos

export default function OnlineUsers() {
  const { token } = useAuth()
  const [users, setUsers] = useState([])
  const [expanded, setExpanded] = useState(false)

  useEffect(() => {
    if (!token) return

    async function fetchOnline() {
      try {
        const res = await fetch('/api/presence/online', {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (res.ok) {
          const data = await res.json()
          setUsers(data.online || [])
        }
      } catch {
        // Silencioso
      }
    }

    fetchOnline()
    const id = setInterval(fetchOnline, POLL_INTERVAL)
    return () => clearInterval(id)
  }, [token])

  if (users.length === 0) return null

  return (
    <div className="px-3 pb-2">
      <button
        onClick={() => setExpanded(!expanded)}
        className="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-xs text-text-secondary
                   hover:bg-parchment-light transition-colors border border-transparent hover:border-border"
      >
        <Eye size={14} className="text-emerald-light" />
        <span className="font-medium text-emerald-light">{users.length} online</span>
        <div className="ml-auto flex -space-x-1.5">
          {users.slice(0, 4).map(u => (
            <img
              key={u.userId}
              src={u.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png'}
              alt=""
              className="w-5 h-5 rounded-full border border-parchment"
            />
          ))}
          {users.length > 4 && (
            <div className="w-5 h-5 rounded-full bg-parchment-lighter border border-parchment flex items-center justify-center">
              <span className="text-[8px] text-text-muted">+{users.length - 4}</span>
            </div>
          )}
        </div>
      </button>

      {expanded && (
        <div className="mt-1 space-y-0.5 animate-fade-in">
          {users.map(u => (
            <div key={u.userId} className="flex items-center gap-2.5 px-3 py-1.5 rounded-lg hover:bg-parchment-light/50 transition-colors">
              <div className="relative">
                <img
                  src={u.avatarUrl || 'https://cdn.discordapp.com/embed/avatars/0.png'}
                  alt=""
                  className="w-7 h-7 rounded-full border border-border"
                />
                <div className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-light rounded-full border-2 border-parchment" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-text-primary truncate">{u.username}</p>
                <p className="text-[10px] text-text-muted truncate">
                  {PAGE_LABELS[u.currentPage] || u.currentPage || 'Navegando'}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
