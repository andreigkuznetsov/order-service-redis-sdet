import { check } from 'k6';
import { createOrder } from '../utils/order-api.js';

export const options = {
    vus: 20,
    duration: '20s',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const clientId = `load-vu-${__VU}-iter-${__ITER}`;
    const res = createOrder(clientId);

    check(res, {
        'status is 201': (r) => r.status === 201,
    });
}