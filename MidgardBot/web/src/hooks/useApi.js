import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'

const BASE_URL = '/api'

export function useApi() {
  const { token, logout } = useAuth()
  const navigate = useNavigate()

  async function request(path, options = {}) {
    const headers = {
      ...(options.multipart ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    }

    const res = await fetch(`${BASE_URL}${path}`, { ...options, headers })

    if (res.status === 401) {
      logout()
      navigate('/login')
      throw new Error('Sessão expirada')
    }

    if (res.status === 403) {
      logout()
      navigate('/login')
      throw new Error('Acesso negado — você não possui cargo de staff')
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.error || `Erro ${res.status}`)
    }

    // Suportar respostas não-JSON (ex.: PDF blob)
    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/pdf')) {
      return res.blob()
    }

    return res.json()
  }

  return {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
    postForm: (path, formData) => request(path, { method: 'POST', body: formData, multipart: true }),
    put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
    del: (path) => request(path, { method: 'DELETE' }),
    getBlob: (path) => request(path)
  }
}
