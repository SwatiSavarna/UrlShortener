import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import UrlShortenerCard from './components/UrlShortenerCard';
import AnalyticsDashboard from './components/AnalyticsDashboard';
import ArchitectureModal from './components/ArchitectureModal';
import { checkHealth } from './services/api';

export default function App() {
  const [activeCode, setActiveCode] = useState('');
  const [rateLimit, setRateLimit] = useState({ remaining: 15, limit: 15 });
  const [healthStatus, setHealthStatus] = useState('UNKNOWN');
  const [isArchModalOpen, setIsArchModalOpen] = useState(false);

  useEffect(() => {
    checkHealth().then((res) => {
      setHealthStatus(res.status || 'UNKNOWN');
    });
  }, []);

  const handleUrlCreated = (newUrlData) => {
    setActiveCode(newUrlData.shortCode);
  };

  return (
    <div className="bg-slate-950 text-slate-100 min-h-screen flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      <Navbar
        onOpenArchitecture={() => setIsArchModalOpen(true)}
        healthStatus={healthStatus}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Shortener Hero Card */}
        <UrlShortenerCard
          onUrlCreated={handleUrlCreated}
          rateLimit={rateLimit}
          setRateLimit={setRateLimit}
        />

        {/* Real-Time Analytics Dashboard */}
        <AnalyticsDashboard
          activeCode={activeCode}
          onSearchCode={(code) => setActiveCode(code)}
        />
      </main>

      <footer className="border-t border-slate-800/80 py-6 text-center text-xs text-slate-500">
        <p>
          FlashLink Distributed Microservice &bull; React 18 &bull; Spring Boot 3 &bull; Redis 7 &bull; Base62 Bijective Codec
        </p>
      </footer>

      {/* Scalability Design Modal */}
      <ArchitectureModal
        isOpen={isArchModalOpen}
        onClose={() => setIsArchModalOpen(false)}
      />
    </div>
  );
}
