import http from 'k6/http';
import { check } from 'k6';

export function registerAndLogin(baseUrl) {
  // Ni __VU ni __ITER ne sont disponibles dans setup() (seulement par iteration
  // de VU) ; Date.now() + Math.random() garantit l'unicite dans les deux contextes.
  const email = `loadtest-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;
  const password = 'MotDePasseLoadTest123!';
  const headers = { 'Content-Type': 'application/json' };

  const registerRes = http.post(
    `${baseUrl}/api/auth/register`,
    JSON.stringify({ email, password }),
    { headers }
  );
  check(registerRes, { 'register -> 201': (r) => r.status === 201 });

  const loginRes = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers }
  );
  check(loginRes, { 'login -> 200': (r) => r.status === 200 });

  return loginRes.json('accessToken');
}

export function exerciseIncidents(baseUrl, token) {
  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${token}`,
  };

  const createRes = http.post(
    `${baseUrl}/api/incidents`,
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
    `${baseUrl}/api/incidents?page=0&size=10`,
    { headers: authHeaders }
  );
  check(listRes, { 'list incidents -> 200': (r) => r.status === 200 });

  const getRes = http.get(
    `${baseUrl}/api/incidents/${incidentId}`,
    { headers: authHeaders }
  );
  check(getRes, { 'get incident -> 200': (r) => r.status === 200 });
}
