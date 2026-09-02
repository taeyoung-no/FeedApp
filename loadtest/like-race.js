import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = 'http://localhost:8080';
const USERS = 1000;
const PASSWORD = 'qwer1234';

const likesCreated = new Counter('likes_created');
const likesFailed = new Counter('likes_failed');

export const options = {
  scenarios: {
    like_race: {
      executor: 'per-vu-iterations',
      vus: USERS,
      iterations: 1,
      maxDuration: '3m',
    },
  },
};

export function setup() {
  const list = http.get(`${BASE}/api/posts`);
  if (list.status !== 200) {
    throw new Error(`GET /api/posts failed: ${list.status}`);
  }
  const post = list.json().find((p) => p.title === 'like-race');
  if (!post) {
    throw new Error('제목이 "like-race"인 글이 없음');
  }

  const tokens = [];
  const batchSize = 20;
  for (let start = 1; start <= USERS; start += batchSize) {
    const end = Math.min(start + batchSize - 1, USERS);
    const reqs = [];
    for (let i = start; i <= end; i++) {
      reqs.push([
        'POST',
        `${BASE}/api/members/login`,
        JSON.stringify({ username: `vu${i}`, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
      ]);
    }
    const responses = http.batch(reqs);
    for (const res of responses) {
      if (res.status !== 200) {
        throw new Error(`login failed: ${res.status} ${res.body}`);
      }
      const cookie = res.cookies.accessToken;
      if (!cookie || !cookie[0]) {
        throw new Error('accessToken cookie 없음');
      }
      tokens.push(cookie[0].value);
    }
  }

  return { postId: post.id, tokens };
}

export default function (data) {
  const token = data.tokens[__VU - 1];
  const res = http.post(`${BASE}/api/posts/${data.postId}/likes`, null, {
    headers: { Cookie: `accessToken=${token}` },
    tags: { name: 'POST /likes' },
  });
  const created = res.status === 201;
  check(res, { 'status is 201': () => created });
  if (created) {
    likesCreated.add(1);
  } else {
    likesFailed.add(1);
  }
}
