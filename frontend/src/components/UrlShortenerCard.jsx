import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { Link2, Sparkles, Copy, Check, ExternalLink, ShieldCheck, Clock, AlertCircle } from 'lucide-react';
import { shortenUrl } from '../services/api';

export default function UrlShortenerCard({ onUrlCreated, rateLimit, setRateLimit }) {
  const [originalUrl, setOriginalUrl] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [ttlDuration, setTtlDuration] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const payload = { originalUrl: originalUrl.trim() };
      if (customAlias.trim()) payload.customAlias = customAlias.trim();
      if (ttlDuration) payload.ttlDuration = ttlDuration;

      const { data, rateLimit: rl } = await shortenUrl(payload);

      if (rl.remaining !== null) {
        setRateLimit(rl);
      }

      setResult(data);
      if (onUrlCreated) {
        onUrlCreated(data);
      }
    } catch (err) {
      if (err.rateLimit && err.rateLimit.remaining !== null) {
        setRateLimit(err.rateLimit);
      }
      setError(err.message || 'Failed to shorten URL');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = () => {
    if (!result?.shortUrl) return;
    navigator.clipboard.writeText(result.shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="glass-card rounded-2xl p-6 sm:p-8 shadow-2xl relative overflow-hidden">
      <div className="absolute -top-24 -right-24 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-3xl mb-6">
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
          High-Throughput Distributed URL Shortener
        </h1>
        <p className="text-slate-400 mt-2 text-sm sm:text-base leading-relaxed">
          Engineered for extreme read/write concurrency with Redis Cache-Aside, asynchronous event-driven click ingestion, token-bucket rate limiting, and custom TTL expiry.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
            Target Destination URL
          </label>
          <div className="relative">
            <Link2 className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-500" />
            <input
              type="url"
              required
              value={originalUrl}
              onChange={(e) => setOriginalUrl(e.target.value)}
              placeholder="https://engineering.atspotify.com/2023/12/how-we-scale-distributed-systems"
              className="w-full bg-slate-900/90 border border-slate-700/80 rounded-xl pl-10 pr-4 py-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
              Custom Vanity Alias <span className="text-slate-500 lowercase font-normal">(optional, 3-30 chars)</span>
            </label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-xs text-slate-500 font-mono">/</span>
              <input
                type="text"
                value={customAlias}
                onChange={(e) => setCustomAlias(e.target.value)}
                pattern="^[a-zA-Z0-9_-]{3,30}$"
                placeholder="tech-talk-2026"
                className="w-full bg-slate-900/90 border border-slate-700/80 rounded-xl pl-7 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1">
              Expiration TTL <span className="text-slate-500 lowercase font-normal">(synchronized with Redis)</span>
            </label>
            <select
              value={ttlDuration}
              onChange={(e) => setTtlDuration(e.target.value)}
              className="w-full bg-slate-900/90 border border-slate-700/80 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            >
              <option value="">Never Expire (Default 7 Days Cache)</option>
              <option value="1m">1 Minute (Live Expiry Demo)</option>
              <option value="5m">5 Minutes</option>
              <option value="1h">1 Hour</option>
              <option value="24h">24 Hours</option>
              <option value="7d">7 Days</option>
              <option value="30d">30 Days</option>
            </select>
          </div>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs flex items-center space-x-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
            <span>{error}</span>
          </div>
        )}

        <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-3 rounded-xl bg-gradient-to-r from-indigo-600 via-purple-600 to-indigo-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold text-sm flex items-center justify-center space-x-2 shadow-lg shadow-indigo-600/30 transition transform active:scale-95 disabled:opacity-50"
          >
            {loading ? (
              <span className="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <Sparkles className="h-4 w-4" />
            )}
            <span>{loading ? 'Shortening...' : 'Shorten Link'}</span>
          </button>

          <div className="text-xs text-slate-400 flex items-center space-x-2 self-center">
            <ShieldCheck className="h-4 w-4 text-emerald-400" />
            <span>
              Token Bucket:{' '}
              <span className="text-white font-mono font-semibold">
                {rateLimit?.remaining !== undefined && rateLimit.remaining !== null ? rateLimit.remaining : 15}
              </span>{' '}
              remaining
            </span>
          </div>
        </div>
      </form>

      {/* Result Card with QR Code */}
      {result && (
        <div className="mt-6 pt-6 border-t border-slate-800/80 animate-fade-in">
          <div className="bg-indigo-950/40 border border-indigo-500/30 rounded-xl p-5 flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="space-y-3 w-full">
              <div className="flex items-center space-x-2">
                <span className="px-2.5 py-0.5 text-[10px] font-bold tracking-wider uppercase rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  Link Ready
                </span>
                {result.expiresAt ? (
                  <span className="px-2.5 py-0.5 text-[10px] font-bold tracking-wider uppercase rounded bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center space-x-1">
                    <Clock className="h-3 w-3 inline" />
                    <span>Expires: {new Date(result.expiresAt).toLocaleTimeString()}</span>
                  </span>
                ) : (
                  <span className="px-2.5 py-0.5 text-[10px] font-bold tracking-wider uppercase rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    Permanent (7d Cache)
                  </span>
                )}
              </div>

              <div className="flex items-center space-x-2">
                <input
                  type="text"
                  readOnly
                  value={result.shortUrl}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-indigo-300 font-mono text-sm focus:outline-none"
                />
                <button
                  type="button"
                  onClick={copyToClipboard}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-lg border border-slate-700 flex items-center space-x-1.5 transition shrink-0"
                >
                  {copied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                  <span>{copied ? 'Copied!' : 'Copy'}</span>
                </button>
                <a
                  href={result.shortUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold rounded-lg flex items-center space-x-1.5 transition shrink-0 whitespace-nowrap"
                >
                  <ExternalLink className="h-3.5 w-3.5" />
                  <span>Test Redirect</span>
                </a>
              </div>

              <p className="text-xs text-slate-400 truncate max-w-lg">
                <span className="text-slate-500">Destination:</span> {result.originalUrl}
              </p>
            </div>

            {/* QR Code */}
            <div className="flex flex-col items-center p-2.5 bg-white rounded-xl shadow-lg shrink-0">
              <QRCodeSVG
                value={result.shortUrl}
                size={84}
                bgColor="#ffffff"
                fgColor="#1e1b4b"
                level="M"
              />
              <span className="text-[10px] text-slate-700 font-semibold mt-1">Scan QR</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
