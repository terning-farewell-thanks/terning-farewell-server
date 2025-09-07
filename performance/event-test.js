import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

// const BASE_URL = 'http://localhost:8080';
const BASE_URL = 'https://www.terning-farewell.p-e.kr';


export const options = {
    scenarios: {
        event_application_race: {
            executor: 'per-vu-iterations',
            vus: 1500,
            iterations: 1,
            maxDuration: '1m',
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<1500'],
    },
};

const tokens = new SharedArray('all-user-tokens', function () {
    console.log('[Init] Reading tokens from file: ./tokens.txt');
    try {
        const f = open('./tokens.txt');
        return f.split('\n').filter(Boolean);
    } catch (e) {
        console.error(`Could not open tokens file. Make sure 'tokens.txt' exists in the script's directory. Error: ${e.message}`);
        return [];
    }
});

export default function () {
    if (tokens.length < 1500) {
        console.error('Not enough tokens in tokens.txt. Please generate at least 1500 unique tokens.');
        return;
    }

    const token = tokens[__VU - 1];

    const params = {
        headers: {
            'Authorization': `Bearer ${token}`,
        },
    };

    const applyRes = http.post(`${BASE_URL}/api/event/apply`, null, params);

    check(applyRes, {
        'Event application processed (status 202 or 409)': (r) => r.status === 202 || r.status === 409,
    });
}
