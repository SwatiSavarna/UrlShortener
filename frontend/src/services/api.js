/**
 * API client interacting with Spring Boot URL shortener endpoints.
 * Automatically parses JSON responses and extracts rate limiting headers.
 */

const BASE_URL = ''; // relative URLs allow transparent proxy in Vite and same-origin in production

export async function shortenUrl(payload) {
  const res = await fetch(`${BASE_URL}/api/v1/urls`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const rateLimit = {
    limit: res.headers.get('X-RateLimit-Limit'),
    remaining: res.headers.get('X-RateLimit-Remaining'),
    reset: res.headers.get('X-RateLimit-Reset'),
  };

  const data = await res.json();
  if (!res.ok) {
    const errorMsg = data.message || data.error || `HTTP error ${res.status}`;
    const err = new Error(errorMsg);
    err.status = res.status;
    err.data = data;
    err.rateLimit = rateLimit;
    throw err;
  }

  return { data, rateLimit };
}

export async function fetchAnalytics(shortCode) {
  const res = await fetch(`${BASE_URL}/api/v1/analytics/${encodeURIComponent(shortCode)}`);
  const rateLimit = {
    limit: res.headers.get('X-RateLimit-Limit'),
    remaining: res.headers.get('X-RateLimit-Remaining'),
    reset: res.headers.get('X-RateLimit-Reset'),
  };

  const data = await res.json();
  if (!res.ok) {
    const err = new Error(data.message || data.error || 'Failed to fetch analytics');
    err.status = res.status;
    throw err;
  }

  return { data, rateLimit };
}

export async function fetchOverview() {
  const res = await fetch(`${BASE_URL}/api/v1/analytics/overview`);
  if (!res.ok) {
    throw new Error('Failed to fetch system overview');
  }
  return res.json();
}

export async function checkHealth() {
  try {
    const res = await fetch(`${BASE_URL}/actuator/health`);
    if (!res.ok) return { status: 'DOWN' };
    return res.json();
  } catch {
    return { status: 'OFFLINE' };
  }
}
