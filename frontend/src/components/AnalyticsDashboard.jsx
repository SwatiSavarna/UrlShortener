import React, { useState, useEffect, useCallback } from 'react';
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
} from 'chart.js';
import { Doughnut, Bar } from 'react-chartjs-2';
import {
  MousePointerClick,
  Users,
  Flame,
  Clock,
  Search,
  RotateCw,
  Smartphone,
  Globe2,
  Compass,
  Laptop,
} from 'lucide-react';
import { fetchAnalytics } from '../services/api';

ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  BarElement,
  Title
);

export default function AnalyticsDashboard({ activeCode, onSearchCode }) {
  const [searchInput, setSearchInput] = useState(activeCode || '');
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const loadData = useCallback(async (code) => {
    if (!code) return;
    setLoading(true);
    try {
      const { data } = await fetchAnalytics(code);
      setAnalytics(data);
    } catch (err) {
      console.warn('Analytics fetch error:', err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (activeCode) {
      setSearchInput(activeCode);
      loadData(activeCode);
    }
  }, [activeCode, loadData]);

  // Polling for live analytics
  useEffect(() => {
    if (!autoRefresh || !activeCode) return;
    const interval = setInterval(() => {
      loadData(activeCode);
    }, 6000);
    return () => clearInterval(interval);
  }, [autoRefresh, activeCode, loadData]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchInput.trim()) {
      onSearchCode(searchInput.trim());
      loadData(searchInput.trim());
    }
  };

  // Device chart data
  const deviceLabels = Object.keys(analytics?.deviceBreakdown || {});
  const deviceValues = Object.values(analytics?.deviceBreakdown || {});
  const deviceChartData = {
    labels: deviceLabels.length ? deviceLabels : ['No Clicks'],
    datasets: [
      {
        data: deviceValues.length ? deviceValues : [1],
        backgroundColor: ['#6366f1', '#a855f7', '#ec4899', '#3b82f6', '#10b981'],
        borderWidth: 0,
      },
    ],
  };

  // Browser chart data
  const browserLabels = Object.keys(analytics?.browserBreakdown || {});
  const browserValues = Object.values(analytics?.browserBreakdown || {});
  const browserChartData = {
    labels: browserLabels.length ? browserLabels : ['None'],
    datasets: [
      {
        label: 'Clicks',
        data: browserValues.length ? browserValues : [0],
        backgroundColor: '#4f46e5',
        borderRadius: 6,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: { color: '#94a3b8', font: { size: 10 }, boxWidth: 10 },
      },
    },
  };

  const barChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: '#94a3b8', font: { size: 10 } }, grid: { display: false } },
      y: { ticks: { color: '#94a3b8', font: { size: 10 }, stepSize: 1 }, grid: { color: '#334155' } },
    },
  };

  return (
    <div className="space-y-6">
      {/* Header and Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white flex items-center space-x-2">
            <MousePointerClick className="h-5 w-5 text-indigo-400" />
            <span>Real-Time Click Analytics & Demographics</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Non-blocking click streams processed asynchronously through bounded thread pools.
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <form onSubmit={handleSearch} className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-500" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Enter short code..."
              className="bg-slate-900 border border-slate-700 rounded-lg pl-8 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 font-mono w-44 sm:w-52"
            />
          </form>

          <button
            type="button"
            onClick={() => loadData(searchInput)}
            className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-lg border border-slate-700 transition flex items-center space-x-1"
          >
            <RotateCw className={`h-3 w-3 ${loading ? 'animate-spin text-indigo-400' : ''}`} />
            <span>Analyze</span>
          </button>

          <button
            type="button"
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={`px-2.5 py-1.5 text-xs font-semibold rounded-lg border transition ${
              autoRefresh
                ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                : 'bg-slate-800 text-slate-400 border-slate-700'
            }`}
            title="Toggle Live 6-second polling"
          >
            {autoRefresh ? 'Live ON' : 'Paused'}
          </button>
        </div>
      </div>

      {/* Metric Tiles */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="glass-card rounded-xl p-4 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>Total Clicks</span>
            <MousePointerClick className="h-4 w-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-bold text-white mt-2 font-mono">
            {analytics?.totalClicks ?? 0}
          </p>
          <span className="text-[11px] text-emerald-400 flex items-center mt-1">
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5 animate-pulse" />
            Live aggregated
          </span>
        </div>

        <div className="glass-card rounded-xl p-4 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>Unique Visitors</span>
            <Users className="h-4 w-4 text-purple-400" />
          </div>
          <p className="text-2xl font-bold text-white mt-2 font-mono">
            {analytics?.uniqueVisitors ?? 0}
          </p>
          <span className="text-[11px] text-slate-400 mt-1 block">Distinct IP Hashing</span>
        </div>

        <div className="glass-card rounded-xl p-4 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>Cache Status</span>
            <Flame className="h-4 w-4 text-rose-400" />
          </div>
          <p className="text-lg font-bold text-emerald-400 mt-2">Active Hit</p>
          <span className="text-[11px] text-slate-400 mt-1 block">&lt; 2ms latency</span>
        </div>

        <div className="glass-card rounded-xl p-4 border border-slate-800">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium">
            <span>TTL Expiry Status</span>
            <Clock className="h-4 w-4 text-amber-400" />
          </div>
          <p
            className={`text-sm font-bold mt-2 font-mono ${
              analytics?.isExpired ? 'text-rose-400' : 'text-white'
            }`}
          >
            {analytics?.isExpired
              ? 'EXPIRED'
              : analytics?.expiresAt
              ? 'Active (TTL)'
              : 'Permanent'}
          </p>
          <span className="text-[11px] text-slate-400 mt-1 block truncate">
            {analytics?.expiresAt
              ? new Date(analytics.expiresAt).toLocaleTimeString()
              : 'Standard 7d TTL'}
          </span>
        </div>
      </div>

      {/* Visual Analytics Charts */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Device Breakdown */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
            <span>Device Breakdown</span>
            <Smartphone className="h-4 w-4 text-slate-500" />
          </h3>
          <div className="h-48 relative flex items-center justify-center">
            <Doughnut data={deviceChartData} options={chartOptions} />
          </div>
        </div>

        {/* Browser Breakdown */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
            <span>Top Browsers</span>
            <Globe2 className="h-4 w-4 text-slate-500" />
          </h3>
          <div className="h-48 relative flex items-center justify-center">
            <Bar data={browserChartData} options={barChartOptions} />
          </div>
        </div>

        {/* Top Referrers */}
        <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
            <span>Traffic Sources (Referrers)</span>
            <Compass className="h-4 w-4 text-slate-500" />
          </h3>
          <div className="space-y-2 h-48 overflow-y-auto text-xs text-slate-300 pr-1">
            {analytics?.topReferrers && Object.keys(analytics.topReferrers).length > 0 ? (
              Object.entries(analytics.topReferrers).map(([ref, count]) => (
                <div
                  key={ref}
                  className="flex items-center justify-between p-2 rounded-lg bg-slate-900/60 border border-slate-800"
                >
                  <span className="font-medium truncate max-w-[150px]">{ref}</span>
                  <span className="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-mono text-[11px]">
                    {count} clicks
                  </span>
                </div>
              ))
            ) : (
              <p className="text-slate-500 italic text-center py-12">No clicks recorded yet.</p>
            )}
          </div>
        </div>
      </div>

      {/* Click Stream Table */}
      <div className="glass-card rounded-xl p-5 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-white flex items-center space-x-2">
            <Laptop className="h-4 w-4 text-indigo-400" />
            <span>Recent Async Click Stream</span>
          </h3>
          <span className="text-[11px] text-slate-400">
            {analytics?.recentClicks?.length || 0} events tracked
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-900/80 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800">
              <tr>
                <th className="px-4 py-2.5">Time</th>
                <th className="px-4 py-2.5">Device</th>
                <th className="px-4 py-2.5">Browser / OS</th>
                <th className="px-4 py-2.5">Referrer</th>
                <th className="px-4 py-2.5">Country</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {analytics?.recentClicks && analytics.recentClicks.length > 0 ? (
                analytics.recentClicks.map((c, i) => (
                  <tr key={i} className="hover:bg-slate-800/40 transition">
                    <td className="px-4 py-2 text-slate-400">
                      {new Date(c.clickedAt).toLocaleTimeString()}
                    </td>
                    <td className="px-4 py-2 font-semibold text-slate-200">
                      <span className="px-2 py-0.5 rounded bg-slate-800 text-[11px] font-sans">
                        {c.deviceType || 'Desktop'}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-slate-300">
                      {c.browser || 'Unknown'} / {c.operatingSystem || 'Unknown'}
                    </td>
                    <td className="px-4 py-2 text-slate-400">{c.referrer || 'Direct'}</td>
                    <td className="px-4 py-2 text-indigo-300">{c.country || 'Unknown'}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-8 text-center text-slate-500 italic font-sans"
                  >
                    No recent clicks recorded yet. Click "Test Redirect" above!
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
