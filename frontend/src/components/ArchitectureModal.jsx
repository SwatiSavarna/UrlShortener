import React from 'react';
import { X, Server, Zap, Shuffle, Binary, ShieldAlert, Timer } from 'lucide-react';

export default function ArchitectureModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-4xl w-full p-6 sm:p-8 shadow-2xl relative max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          className="absolute right-5 top-5 text-slate-400 hover:text-white transition p-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-800"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="flex items-center space-x-3 mb-6">
          <div className="h-10 w-10 rounded-xl bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
            <Server className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">System Architecture & Scalability Guide</h2>
            <p className="text-xs text-slate-400">Engineering design decisions for extreme concurrency & sub-millisecond throughput.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-slate-300">
          {/* Card 1 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
            <div className="font-bold text-slate-100 flex items-center space-x-2 text-sm">
              <Zap className="h-4 w-4 text-amber-400 shrink-0" />
              <span>1. Read-Heavy Cache-Aside & Negative Caching</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              URL shorteners have a <strong>100:1 to 1000:1 read-to-write asymmetry</strong>. Redirections bypass the database completely via Redis <code className="text-indigo-300">shorturl:&#123;code&#125;</code>, delivering response times in <strong>&lt; 2ms</strong>.
            </p>
            <p className="text-slate-400 leading-relaxed">
              <strong>Negative Caching</strong>: Non-existent codes are cached as sentinels for 60 seconds to eliminate database cache penetration from malicious scrapers.
            </p>
          </div>

          {/* Card 2 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
            <div className="font-bold text-slate-100 flex items-center space-x-2 text-sm">
              <Shuffle className="h-4 w-4 text-emerald-400 shrink-0" />
              <span>2. Zero-Latency Async Analytics</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              Redirecting a user never blocks on IP geo-lookup or DB writes. The redirect thread immediately returns an <strong>HTTP 302</strong>, while publishing an internal event onto Spring's bus.
            </p>
            <p className="text-slate-400 leading-relaxed">
              Handled out-of-band by a dedicated <code className="text-indigo-300">ThreadPoolTaskExecutor</code> with bounded queues (10,000 capacity) and a <code className="text-indigo-300">CallerRunsPolicy</code> for graceful backpressure.
            </p>
          </div>

          {/* Card 3 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
            <div className="font-bold text-slate-100 flex items-center space-x-2 text-sm">
              <Binary className="h-4 w-4 text-purple-400 shrink-0" />
              <span>3. Collision-Free Base62 Bijective Codec</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              Instead of hashing URLs (which creates collision probability and requires costly retry lookups), sequence IDs are bijectively converted into Base62 characters <code className="text-indigo-300">[0-9a-zA-Z]</code>.
            </p>
            <p className="text-slate-400 leading-relaxed">
              A 7-character string supports <strong>62⁷ ≈ 3.52 Trillion</strong> unique URLs without collision risk.
            </p>
          </div>

          {/* Card 4 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2">
            <div className="font-bold text-slate-100 flex items-center space-x-2 text-sm">
              <ShieldAlert className="h-4 w-4 text-rose-400 shrink-0" />
              <span>4. Distributed Token Bucket Rate Limiting</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              Enforces per-client quotas: <strong>15 creates/min</strong>, <strong>120 redirects/min</strong>, and <strong>60 analytics reads/min</strong> with burst resilience.
            </p>
            <p className="text-slate-400 leading-relaxed">
              Emits standard <code className="text-indigo-300">X-RateLimit-*</code> HTTP headers and automatically rejects abuse with <strong>HTTP 429 Too Many Requests</strong>.
            </p>
          </div>

          {/* Card 5 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800 space-y-2 md:col-span-2">
            <div className="font-bold text-slate-100 flex items-center space-x-2 text-sm">
              <Timer className="h-4 w-4 text-cyan-400 shrink-0" />
              <span>5. Custom Expiration & Redis TTL Synchronization</span>
            </div>
            <p className="text-slate-400 leading-relaxed">
              The Redis cache key TTL is dynamically calculated to match the remaining link lifespan: <code className="text-indigo-300">TTL = Duration(now, expiresAt)</code>. Expired requests yield <strong>HTTP 410 Gone</strong>. An active background scheduled job (<code className="text-indigo-300">@Scheduled</code>) batch-purges expired records every 30 minutes to keep B-tree indexes compact.
            </p>
          </div>
        </div>

        <div className="mt-6 pt-4 border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs transition"
          >
            Close Guide
          </button>
        </div>
      </div>
    </div>
  );
}
