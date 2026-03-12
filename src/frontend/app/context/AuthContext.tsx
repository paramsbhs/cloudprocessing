import { createContext, useContext, useState, ReactNode } from 'react';
import { api, AuthResponse } from '../api';

interface AuthState {
  token: string | null;
  email: string | null;
  userId: string | null;
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function persist(data: AuthResponse) {
  localStorage.setItem('token', data.token);
  localStorage.setItem('email', data.email);
  localStorage.setItem('userId', data.userId);
}

function clear() {
  localStorage.removeItem('token');
  localStorage.removeItem('email');
  localStorage.removeItem('userId');
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    token: localStorage.getItem('token'),
    email: localStorage.getItem('email'),
    userId: localStorage.getItem('userId'),
  });

  const hydrate = (data: AuthResponse) => {
    persist(data);
    setState({ token: data.token, email: data.email, userId: data.userId });
  };

  const login = async (email: string, password: string) => {
    const data = await api.auth.login(email, password);
    hydrate(data);
  };

  const register = async (email: string, password: string) => {
    const data = await api.auth.register(email, password);
    hydrate(data);
  };

  const logout = () => {
    clear();
    setState({ token: null, email: null, userId: null });
  };

  return (
    <AuthContext.Provider
      value={{ ...state, login, register, logout, isAuthenticated: !!state.token }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
