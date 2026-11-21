import http from 'k6/http';

export const options = {
  scenarios: {
    popular: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 1000,
      maxVUs: 1000,
    },
  },
};

export default function () {
  http.get('http://localhost:8080/api/posts/popular');
}
