import { create } from 'zustand'
import { getMe, type LoginResponse } from '../api/member'

type AuthState = {
  user: LoginResponse | null
  isLoading: boolean
  setUser: (user: LoginResponse | null) => void
  loadUser: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isLoading: true,
  setUser: (user) => set({ user }),
  loadUser: async () => {
    try {
      const me = await getMe()
      set({ user: me, isLoading: false })
    } catch {
      set({ user: null, isLoading: false })
    }
  },
}))
