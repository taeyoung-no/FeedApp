import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('feed_list_errors');
const feedListDuration = new Trend('feed_list_duration', true);

export const options = {
  scenarios: {
    feed_list: {
      executor: 'constant-vus',
      vus: 10,
      duration: '60s',
    },
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/posts', {
    tags: { name: 'GET /api/posts' },
  });

  feedListDuration.add(res.timings.duration);

  const ok = check(res, {
    'status is 200': (r) => r.status === 200,
  });

  errorRate.add(!ok);
  sleep(1);
}
