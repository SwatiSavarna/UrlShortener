import React from 'react';
import { Zap, ShieldCheck, Database, HeartPulse, Layers } from 'lucide-react';

export default function Navbar({ onOpenArchitecture, healthStatus }) {
  const isHealthy = healthStatus === 'UP';

  return (
    <header className="border-b border-slate-800/80 sticky top-0 z-40 glass-card">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg shadow-indigo-500/25">
            <Zap className="h-5 w-5 text-white" />
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-xl font-extrabold tracking-tight bg-gradient-to-r from-white via-indigo-200 to-indigo-400 bg-clip-text text-transparent">
              FlashLink
            </span>
            <span className="hidden sm:inline-block text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-indigo-500/15 text-indigo-300 border border-indigo-500/30">
              Spring Boot 3 + Redis + React
            </span>
          </div>
        </div>

        <div className="flex items-center space-x-3 sm:space-x-4">
          <div className="hidden md:flex items-center space-x-2 text-xs text-slate-400 bg-slate-900/90 px-3 py-1.5 rounded-lg border border-slate-800">
            <span className="inline-block w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Cache-Aside Active</span>
            <span className="text-slate-600">|</span>
            <span className="text-indigo-400 font-mono font-medium">&lt; 2ms Latency</span>
          </div>

          <button
            onClick={onOpenArchitecture}
            className="flex items-center space-x-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/40 transition"
          >
            <Layers className="h-3.5 w-3.5" />
            <span className="hidden sm:inline">Scalability Design</span>
            <span className="sm:hidden">Architecture</span>
          </button>

          <a
            href="/h2-console"
            target="_blank"
            rel="noreferrer"
            className="text-xs text-slate-400 hover:text-slate-200 flex items-center space-x-1 transition"
            title="Open H2 Database Console"
          >
            <Database className="h-3.5 w-3.5" />
            <span className="hidden lg:inline">H2 Console</span>
          </a>

          <a
            href="/actuator/health"
            target="_blank"
            rel="noreferrer"
            className="text-xs text-slate-400 hover:text-slate-200 flex items-center space-x-1 transition"
            title="Actuator Health Endpoint"
          >
            <HeartPulse className={`h-3.5 w-3.5 ${isHealthy ? 'text-emerald-400' : 'text-rose-400'}`} />
            <span className="hidden lg:inline">{isHealthy ? 'Healthy' : 'Health'}</span>
          </a>
        </div>
      </div>
    </header>
  );
}
