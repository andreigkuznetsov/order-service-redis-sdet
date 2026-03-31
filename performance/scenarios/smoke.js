import { check } from 'k6';
import { createOrder } from '../utils/order-api.js';

export const options = {
    vus: 1,
    iterations: 5,
};

export default function () {
    const res = createOrder("smoke-test");

    check(res, {
        'status is 201': (r) => r.status === 201,
    });
}