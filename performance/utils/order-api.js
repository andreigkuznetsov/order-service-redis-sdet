import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function createOrder(clientId, idempotencyKey = null) {
    const payload = JSON.stringify({
        product: 'iPhone 15',
        quantity: 1,
        price: 999.99
    });

    const headers = {
        'Content-Type': 'application/json',
        'X-Client-Id': clientId
    };

    if (idempotencyKey) {
        headers['Idempotency-Key'] = idempotencyKey;
    }

    return http.post(`${BASE_URL}/api/orders`, payload, { headers });
}