import http from 'k6/http';
import {check} from 'k6';
import {FormData} from 'https://jslib.k6.io/formdata/0.0.2/index.js';

// To run with locally installed K6 try this :
// > export TOKEN=xxxxx
// > k6 run loadtest-tt02.js
//
// To run with Docker do something like this :
// > export TOKEN=xxxxx
// > docker run --rm -i -e "TOKEN=$TOKEN" -v "$(pwd)/altinn-v3-proxy/k6/loadtest-tt02.js:/loadtest-tt02.js:ro" grafana/k6 run /loadtest-tt02.js

// read maskinporten token to use from ENV variable
const TOKEN = (__ENV.TOKEN) ? __ENV.TOKEN : 'NoTokenWasSet';

// Options for running the test
export const options = {
    duration: '1s',
    vus: 1,
    TOKEN: TOKEN,
    thresholds: {
        http_req_failed: ['rate<0.01'],     // http errors should be less than 1%
        http_req_duration: ['p(99)<500'],   // 95 percent of response times must be below 500ms
    },
};

// Set up global data used by the testing
export function setup() {

    let proxy = 'https://dpvproxy.apps.kt.digdir.cosng.net';

    return {
        dpvProxyUrl: proxy,
        maskinportenToken: options.TOKEN,
    }

}

// This is the function that will be used for each virtual user that runs the test
export default function (data) {
    pingLoadTest(data);
    //fetchCorrespondenceDetails(data);
    //sendNewCorrespondence(data);
}

// Use the "ping" function in the proxy for performance testing
function pingLoadTest(data) {

    const url = data.dpvProxyUrl + '/ping?wait=500&size=2048'; // wait 500 ms before returning 2 KiB of data

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
        'successfully fetching correspondence details': (r) => r.status === 200,
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
        'successfully POSTed a new message': (r) => r.status === 200,
    });

}
