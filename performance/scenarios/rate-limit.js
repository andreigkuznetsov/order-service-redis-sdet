import { check } from 'k6';
import { createOrder } from '../utils/order-api.js';

export const options = {
    vus: 10,
    iterations: 10,
    thresholds: {
        http_req_failed: ['rate<0.6'],
    },
};

const CLIENT_ID = 'rate-limit-test';

export default function () {
    const idempotencyKey = `key-${__VU}-${__ITER}`;

    const res = createOrder(CLIENT_ID, idempotencyKey);

    check(res, {
        'status is 201 or 429': (r) =>
        r.status === 201 || r.status === 429,
    });

    console.log(`VU ${__VU}, status=${res.status}`);
}