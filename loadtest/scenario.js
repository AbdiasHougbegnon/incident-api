import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const email = `loadtest-${__VU}-${__ITER}-${Date.now()}@example.com`;
  const password = 'MotDePasseLoadTest123!';
  const headers = { 'Content-Type': 'application/json' };

  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ email, password }),
    { headers }
  );
  check(registerRes, { 'register -> 201': (r) => r.status === 201 });

  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers }
  );
  check(loginRes, { 'login -> 200': (r) => r.status === 200 });

  const token = loginRes.json('accessToken');
  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  const createRes = http.post(
    `${BASE_URL}/api/incidents`,
    JSON.stringify({
      title: 'Incident de test de charge',
      description: 'Genere par le script k6.',
      status: 'OPEN',
      priority: 'LOW',
    }),
    { headers: authHeaders }
  );
  check(createRes, { 'create incident -> 201': (r) => r.status === 201 });

  const incidentId = createRes.json('id');

  const listRes = http.get(
    `${BASE_URL}/api/incidents?page=0&size=10`,
    { headers: authHeaders }
  );
  check(listRes, { 'list incidents -> 200': (r) => r.status === 200 });

  const getRes = http.get(
    `${BASE_URL}/api/incidents/${incidentId}`,
    { headers: authHeaders }
  );
  check(getRes, { 'get incident -> 200': (r) => r.status === 200 });

  sleep(1);
}
