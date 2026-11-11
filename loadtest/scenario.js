import { sleep } from 'k6';
import { registerAndLogin, exerciseIncidents } from './common.js';

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
  const token = registerAndLogin(BASE_URL);
  exerciseIncidents(BASE_URL, token);
  sleep(1);
}
