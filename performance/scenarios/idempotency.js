import { check } from 'k6';
import { createOrder } from '../utils/order-api.js';

export const options = {
    vus: 10,
    iterations: 10,
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

const IDEMPOTENCY_KEY = 'same-key-for-all-vus';

export default function () {
    const clientId = `idempotency-k6-vu-${__VU}`;

    const res = createOrder(clientId, IDEMPOTENCY_KEY);

    let body = {};
    try {
        body = res.json();
    } catch (e) {}

    check(res, {
        'status is 200, 201 or 409': (r) =>
        r.status === 200 || r.status === 201 || r.status === 409,

        'successful response has order id': () => {
            if (res.status === 200 || res.status === 201) {
                return !!body.id;
            }
            return true;
        },
    });

    console.log(
        `VU ${__VU}, status=${res.status}, orderId=${body.id || 'N/A'}`
    );
}