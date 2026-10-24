// Load test of the member's main read endpoints (Phase 2.9). Target: p95 <= 500 ms, errors < 1%.
//
//   k6 run -e BASE_URL=https://<host> -e TOKEN=<member access token> scripts/loadtest/read-endpoints.js
//
// Use a test member with realistic data (a few hundred transactions, habits, mood entries), never a real one.
// VUS / DURATION can be raised with -e VUS=50 -e DURATION=5m.
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';
const TOKEN = __ENV.TOKEN;

export const options = {
    vus: Number(__ENV.VUS || 20),
    duration: __ENV.DURATION || '2m',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        'http_req_duration{endpoint:dashboard}': ['p(95)<500'],
        'http_req_duration{endpoint:history}': ['p(95)<500'],
        'http_req_duration{endpoint:budgets}': ['p(95)<500'],
        'http_req_duration{endpoint:insights}': ['p(95)<500'],
        'http_req_duration{endpoint:habits}': ['p(95)<500'],
    },
};

export function setup() {
    if (!TOKEN) {
        throw new Error('Set TOKEN to an access token of a test member');
    }
}

function get(path, endpoint) {
    const response = http.get(`${BASE_URL}${path}`, {
        headers: { Authorization: `Bearer ${TOKEN}` },
        tags: { endpoint },
    });
    check(response, { [`${endpoint} is 200`]: (r) => r.status === 200 });
}

export default function () {
    group('home', () => {
        get('/api/v1/dashboard/summary', 'dashboard');
        get('/api/v1/notifications/unread-count', 'unread');
    });
    group('finance', () => {
        get('/api/v1/transactions?page=0&size=20', 'history');
        get('/api/v1/budgets', 'budgets');
        get('/api/v1/finance/summary', 'summary');
    });
    group('wellbeing', () => {
        get('/api/v1/habits/today', 'habits');
        get('/api/v1/insights/weekly', 'insights');
        get('/api/v1/mood-entries/trend?period=month', 'trend');
    });
    sleep(1);
}
