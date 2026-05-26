import { useEffect, useRef, useCallback } from 'react'
import { useAuth } from '../context/AuthContext'
import { useLocation } from 'react-router-dom'

const HEARTBEAT_INTERVAL = 30_000 // 30 segundos

/**
 * Hook que envia heartbeats periódicos para o backend,
 * informando que o usuário está ativo no painel.
 */
export function usePresence() {
  const { token, user } = useAuth()
  const location = useLocation()
  const intervalRef = useRef(null)

  const sendHeartbeat = useCallback(async (page) => {
    if (!token) return
    try {
      await fetch('/api/presence/heartbeat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          avatarUrl: user?.avatarUrl || null,
          currentPage: page || '/'
        })
      })
    } catch {
      // Silencioso — heartbeat falhou, será reenviado
    }
  }, [token, user?.avatarUrl])

  useEffect(() => {
    if (!token) return

    // Envia imediatamente ao mudar de página ou ao montar
    sendHeartbeat(location.pathname)

    // Intervalo periódico
    intervalRef.current = setInterval(() => {
      sendHeartbeat(location.pathname)
    }, HEARTBEAT_INTERVAL)

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [token, location.pathname, sendHeartbeat])
}
