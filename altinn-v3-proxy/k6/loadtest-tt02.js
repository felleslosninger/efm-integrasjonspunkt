import http from 'k6/http';
import {check} from 'k6';
import {FormData} from 'https://jslib.k6.io/formdata/0.0.2/index.js';

// To run with locally installed K6 try this :
// > k6 run loadtest-tt02.js
//
// To run using Docker do something like this :
// > docker run --rm -i -v "$(pwd)/altinn-v3-proxy/k6/loadtest-tt02.js:/loadtest-tt02.js:ro" grafana/k6 run /loadtest-tt02.js

// Options for running the test
export const options = {
    duration: '1s',
    vus: 1,
    thresholds: {
        http_req_failed: ['rate<0.01'],     // http errors should be less than 1%
        http_req_duration: ['p(99)<500'],   // 95 percent of response times must be below 500ms
    },
};

// Set up global data used by the testing
export function setup() {

    let proxy = 'https://dpvproxy.apps.kt.digdir.cosng.net';
    let token = 'eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxOUFGOTRFNDQ1MzE0Q0RDMjk1Rjk1MjUzODU4MDU0RjhCQ0FDODYiLCJ4NXQiOiJjWnI1VGtSVEZNM0NsZmxTVTRXQVZQaThySVkiLCJ0eXAiOiJKV1QifQ.eyJzY29wZSI6ImFsdGlubjpicm9rZXIucmVhZCIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE3NjEzMTk2MzgsImlhdCI6MTc2MTMxNzgzOCwiY2xpZW50X2lkIjoiYjU5MGYxNDktZDBiYS00ZmNhLWIzNjctYmNjZDllNDQ0YTAwIiwiY29uc3VtZXIiOnsiYXV0aG9yaXR5IjoiaXNvNjUyMy1hY3RvcmlkLXVwaXMiLCJJRCI6IjAxOTI6MzExNzgwNzM1In0sInVybjphbHRpbm46b3JnTnVtYmVyIjoiMzExNzgwNzM1IiwidXJuOmFsdGlubjphdXRoZW50aWNhdGVtZXRob2QiOiJtYXNraW5wb3J0ZW4iLCJ1cm46YWx0aW5uOmF1dGhsZXZlbCI6MywiaXNzIjoiaHR0cHM6Ly9wbGF0Zm9ybS50dDAyLmFsdGlubi5uby9hdXRoZW50aWNhdGlvbi9hcGkvdjEvb3BlbmlkLyIsImp0aSI6IjNjZDFiOGY4LTc2NDgtNDYwNi1hNTk5LTYwMTdiYTBiYTFiYSIsIm5iZiI6MTc2MTMxNzgzOH0.tu-MxzSWwm40ZOpcD2BUOMNycZeC690Ydhcrjwj_G3wlAgXaUV_54nkoqhSXydj9iJAGF0dgg5_VkF57dAGqQsMmfjjpvHWE3bpRF8BgCbQTN4mjRwhC-VflwTUg5k6QWmyG_qsKQsyLEt46VvRKA0qg6JfwZPEwOOpBs3PnZz9cZQYEvsk_bsaSBiMoDvIpcHTCiE4W2yzI4VLRhJSRTHL2smddGPiLs2MmClU8uYYKbUXYfNyUecBkoSDsrmSGLvAO-Tnuf4WrVtrbPONqJx7_ZR-LtmN5xb4IxZyjP3_G2Mi76L1jYiIMMHHjwd0qwlqmtvBPBHpFRqtL1_oZzQ';

    return {
        dpvProxyUrl: proxy,
        maskinportenToken: token,
    }

}

// This is the function that will be used for each virtual user that runs the test
export default function (data) {
    pingLoadTest(data);
    //fetchCorrespondenceDetails(data);
    // sendNewCorrespondence(data);
}

// Use the "ping" function in the proxy for performance testing
function pingLoadTest(data) {

    const url = data.dpvProxyUrl + '/ping?wait=500&size=2048'; // wait 500 ms before returning 2 MiB of data

    console.log(url)

    const res = http.get(url, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + data.maskinportenToken
        },
    });

    console.log(res.status_text);

    check(res, {
        'sucessfully pinging': (r) => r.status === 200,
    });

}

// Henter detaljer på en kjent correspondence direkte fra Altinn v3 via proxy
function fetchCorrespondenceDetails(data) {

    // https://docs.altinn.studio/en/api/correspondence/spec/#/Correspondence/get_correspondence_api_v1_correspondence__correspondenceId__details

    const correspondenceId = '019980e6-e382-7eab-8c9d-60b9c7ccd116';
    const url = data.dpvProxyUrl + '/altinn-proxy/correspondence/api/v1/correspondence/' + correspondenceId + '/details';

    console.log(url)

    const res = http.get(url, {
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + data.maskinportenToken
        },
    });

    console.log(res.status_text);

    check(res, {
        'sucessfully fetching corresponence details': (r) => r.status === 200,
    });

}

// Sender en ny correspondence direkte til Altinn v3 via proxy
function sendNewCorrespondence(data) {

    // https://docs.altinn.studio/en/api/correspondence/spec/#/Correspondence/post_correspondence_api_v1_correspondence_upload

    const theForm = new FormData();
    theForm.append('Correspondence.ResourceId', 'The Resource Id associated with the correspondence service.');
    theForm.append('Correspondence.SendersReference', 'A reference used by senders and receivers to identify a specific Correspondence using external identification methods.');
    theForm.append('Correspondence.Content.Language', 'Gets or sets the language of the correspondence, specified according to ISO 639-1');
    theForm.append('Correspondence.Content.MessageTitle', 'Gets or sets the correspondence message title. Subject.');
    theForm.append('Correspondence.Content.MessageBody', 'Gets or sets the main body of the correspondence.');
    theForm.append('Correspondence.Notification.NotificationTemplate', 'Enum describing available notification templates.');
    theForm.append('Recipients', 'List of recipients for the correspondence, either as organization(urn:altinn:organization:identifier-no:ORGNR) or national identity number(urn:altinn:person:identifier-no:SSN)');

    const url = data.dpvProxyUrl + '/altinn-proxy/correspondence/api/v1/correspondence/upload';

    console.log(url)

    const res = http.post(url, theForm.body(), {
        headers: {
            'Content-Type': 'multipart/form-data; boundary=' + theForm.boundary,
            'Authorization': 'Bearer ' + data.maskinportenToken
        },
    });

    console.log(res.status_text);

    check(res, {
        'sucessfully POSTed a new message': (r) => r.status === 200,
    });

}
