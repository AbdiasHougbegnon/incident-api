import { sleep } from 'k6';
import { registerAndLogin, exerciseIncidents } from './common.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// S'authentifie une seule fois (pas par iteration) : en situation reelle,
// un client ne se reconnecte pas a chaque requete, et le rate limiter sur
// /api/auth/** (10 req/min/IP) bloquerait de toute facon un register+login
// par iteration des que la montee en charge depasse quelques VUs.
export function setup() {
  const token = registerAndLogin(BASE_URL);
  return { token };
}

export const options = {
  scenarios: {
    ramping_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 5 },
        { duration: '2m', target: 20 },
        { duration: '2m', target: 20 },
        { duration: '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function (data) {
  exerciseIncidents(BASE_URL, data.token);
  sleep(1);
}
