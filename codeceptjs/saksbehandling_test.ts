const FormData = require('form-data');
const fs = require('fs');
const path = require('path');
const tryTo = codeceptjs.container.plugins('tryTo');
const axios = require('axios').default;
const assert = require('assert');

Feature('Ende-til-ende');

let sleep = (ms) => {
    return new Promise(resolve => setTimeout(resolve, ms));
};

let randomString = () => {
    var string = '';
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (var i = 0; i < 10; i++) {
        string += chars.at(Math.floor(Math.random() * chars.length));
    }
    return string;
};

let buildSbd = (receiver, process, documentType, businessMessage, conversationId?) => {
    let sbd = {
        standardBusinessDocumentHeader: {
            headerVersion: '1.0',
            receiver: [{
                identifier: {
                    authority: 'iso6523-actorid-upis',
                    value: receiver
                }
            }
            ],
            documentIdentification: {
                standard: documentType,
                type: Object.keys(businessMessage)[0],
                typeVersion: '2.0'
            },
            businessScope: {
                scope: [{
                    identifier: process,
                    type: 'ConversationId'
                }]
            }
        },
        ...businessMessage
    };
    if (conversationId) {
        sbd.standardBusinessDocumentHeader.businessScope.scope[0].instanceIdentifier = conversationId;
    }
    return sbd;
};

let buildStatuses = (statuses, message) => {
    return {
        content: statuses.map((status) => {
            return {
                status: status,
                messageId: message.id,
                conversationId: message.conversationId

            };
        }),
        totalElements: statuses.length
    };
};

let waitForMessage = async(I, baseUrl, message) => {
    let peekResponse = null;
    for (let i = 0; i < 60 * 15; i++) { // Prøver i opptil 15 minutt
        await sleep(1000);
        peekResponse = await I.sendGetRequest(baseUrl + '/messages/in/peek?messageId=' + message.id);
        if (peekResponse.status == 200) {
            break;
        }
    }
    I.seeResponseCodeIs(200);
    let popResponse = await sendGetRequestFixed(baseUrl + '/messages/in/pop/' + message.id);
    assert.equal(popResponse.status == 200 || popResponse.status == 204, true);
    await I.sendDeleteRequest(baseUrl + '/messages/in/' + message.id);
    I.seeResponseCodeIs(200);
};

let waitForMessageReceipt = async(I, baseUrl, message) => {
    let peekResponse = null;
    for (let i = 0; i < 60 * 15; i++) { // Prøver i opptil 15 minutt
        await sleep(1000);
        peekResponse = await I.sendGetRequest(baseUrl + '/messages/in/peek?conversationId=' + message.conversationId);
        if (peekResponse.status == 200) {
            break;
        }
    }
    I.seeResponseCodeIs(200);
    let messageId = peekResponse.data.standardBusinessDocumentHeader.documentIdentification.instanceIdentifier;
    let popResponse = await sendGetRequestFixed(baseUrl + '/messages/in/pop/' + messageId);
    assert.equal(popResponse.status == 200 || popResponse.status == 204, true);
    await I.sendDeleteRequest(baseUrl + '/messages/in/' + messageId);
    I.seeResponseCodeIs(200);
};

let sendPutRequestFixed = async(url, data, headers) => {
    // Bruker axios direkte ved opplasting av filer istadenfor I.sendPutRequest(...)
    // Ellers printer CodeceptJS binært innhold
    return axios({
        baseURL: url,
        method: 'PUT',
        data: data,
        headers
    });
};

let sendGetRequestFixed = async(url) => {
    // Bruker axios direkte ved nedlasting av filer istadenfor I.sendGetRequest(...)
    // Ellers printer CodeceptJS binært innhold
    return axios({
        baseURL: url,
        method: 'GET'
    });
};

let sendMessage = async (I, baseUrl, message) => {
    const sbd = await buildSbd(message.receiver, message.process, message.documentType, message.businessMessage, message.conversationId);
    const sbdResponse = await I.sendPostRequest(baseUrl + '/messages/out', sbd);
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson(sbd);
    message.id = sbdResponse.data.standardBusinessDocumentHeader.documentIdentification.instanceIdentifier;
    message.conversationId = sbdResponse.data.standardBusinessDocumentHeader.businessScope.scope
        .find((scope) => scope.type === 'ConversationId').instanceIdentifier;
    for (let i = 0; i < message.files.length; i++) {
        let file = message.files[i];
        let fileContent = fs.readFileSync(path.join(__dirname, '/' + file.name));
        if (file.name === 'arkivmelding.xml') {
            // Unik tittel gjer det enklere å hente ut fra web
            // Unngår KS SvarUt feil: "Forsendelse med samme mottaker, tittel, avsender og filer er forsøkt sendt tidligere"
            message.title = 'Digdir e2e-test ' + randomString();
            fileContent = fileContent.toString().replace('<!-- title -->', message.title);
        }
        let putResponse = await sendPutRequestFixed(baseUrl + '/messages/out/' + message.id, fileContent, {
            'Content-Disposition': 'attachment; name=' + file.title + '; filename=' + file.name,
            'Content-Type': file.contentType
        });
        assert.equal(putResponse.status, 200);
    }
    await I.sendPostRequest(baseUrl + '/messages/out/' + message.id);
    I.seeResponseCodeIs(200);
    /*await sleep(5000);
    await expectStatuses(I, baseUrl, message, ['OPPRETTET', 'SENDT']);*/
};

let expectStatuses = async (I, baseUrl, message, statuses) => {
    await I.sendGetRequest(baseUrl + '/statuses/' + message.id);
    I.seeResponseCodeIs(200);
    I.seeResponseContainsJson(buildStatuses(statuses, message));
};

let waitForStatuses = async (I, baseUrl, message, statuses) => {
    for (let i = 0; i < 60 * 15; i++) { // Prøver i opptil 15 minutt
        await sleep(1000);
        const statusResponse = await I.sendGetRequest(baseUrl + '/statuses/' + message.id);
        if (statusResponse.data.content.length === statuses.length) {
            break;
        }
    }
    await expectStatuses(I, baseUrl, message, statuses);
};

let loginUsingIdporten = async (I, user) => {
    I.click('#MinIDEksternChain');
    I.fillField('#personalIdNumber', user.personalIdNumber);
    I.fillField('#password', user.password);
    I.click('#next');
    I.fillField('#otpCode', user.pinCode);
    I.click('#next');
    tryTo(() => {
       I.click('#continuebtn');
    });
    tryTo(() => {
        I.click('#next');
    });
};

let dpoExamples = new DataTable(['senderBaseUrl', 'receiverBaseUrl', 'message']);
dpoExamples.add([
    'http://localhost:9093/api',
    'https://qa-meldingsutveksling.difi.no/integrasjonspunkt/digdir-leikanger/api',
    {
        sender: '0192:991825827',
        receiver: '0192:987464291',
        process: 'urn:no:difi:profile:arkivmelding:administrasjon:ver1.0',
        documentType: 'urn:no:difi:arkivmelding:xsd::arkivmelding',
        businessMessage: {
            arkivmelding: {}
        },
        files: [{
            name: 'arkivmelding.xml',
            title: 'Arkivmelding',
            contentType: 'application/xml'
        }, {
            name: 'test.pdf',
            title: 'Test',
            contentType: 'application/octet-stream'
        }]
    }
]);
dpoExamples.add([
    'https://qa-meldingsutveksling.difi.no/integrasjonspunkt/digdir-leikanger/api',
    'http://localhost:9093/api',
    {
        sender: '0192:987464291',
        receiver: '0192:991825827',
        process: 'urn:no:difi:profile:arkivmelding:administrasjon:ver1.0',
        documentType: 'urn:no:difi:arkivmelding:xsd::arkivmelding',
        businessMessage: {
            arkivmelding: {}
        },
        files: [{
            name: 'arkivmelding.xml',
            title: 'Arkivmelding',
            contentType: 'application/xml'
        }, {
            name: 'test.pdf',
            title: 'Test',
            contentType: 'application/octet-stream'
        }]
    }
]);

Data(dpoExamples).Scenario('Saksbehandling med eFormidlings meldingstjeneste (DPO)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.receiverBaseUrl, current.message, ['OPPRETTET', 'INNKOMMENDE_MOTTATT']);
    await waitForMessage(I, current.receiverBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'MOTTATT', 'LEVERT']);
    await expectStatuses(I, current.receiverBaseUrl, current.message, ['OPPRETTET', 'INNKOMMENDE_MOTTATT', 'INNKOMMENDE_LEVERT']);
    await sendMessage(I, current.receiverBaseUrl, {
        receiver: current.message.sender,
        process: 'urn:no:difi:profile:arkivmelding:response:ver1.0',
        documentType: 'urn:no:difi:arkivmelding:xsd::arkivmelding_kvittering',
        conversationId: current.message.conversationId,
        businessMessage: {
            arkivmelding_kvittering: {
                receiptType: 'OK',
                relatedToMessageId: current.message.id
            }
        },
        files: []
    });
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'MOTTATT', 'LEVERT', 'LEST']);
    await waitForMessageReceipt(I, current.senderBaseUrl, current.message);
});

let dpfExamples = new DataTable(['senderBaseUrl', 'message', 'receiversUser']);
dpfExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '0192:910065254',
        process: 'urn:no:difi:profile:arkivmelding:administrasjon:ver1.0',
        documentType: 'urn:no:difi:arkivmelding:xsd::arkivmelding',
        businessMessage: {
            arkivmelding: {}
        },
        files: [{
            name: 'arkivmelding.xml',
            title: 'Arkivmelding',
            contentType: 'application/xml'
        }, {
            name: 'test.pdf',
            title: 'Test',
            contentType: 'application/octet-stream'
        }]
    },
    {
        personalIdNumber: '16079415093',
        password: 'password01',
        pinCode: '12345'
    }
]);

Data(dpfExamples).Scenario('Saksbehandling med KS SvarUt og SvarInn (DPF)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'LEVERT']);
    await sleep(10000);
    I.amOnPage('https://svarut.fiks.test.ks.no/');
    I.click('Logg inn');
    await loginUsingIdporten(I, current.receiversUser);
    // Retries avoids issues with testing KS SvarUt Angular single page application
    I.retry(5).click('Forsendelser');
    I.retry(5).click('Innkommende');
    I.retry(5).fillField('Forsendelsesid', current.message.id);
    I.retry(5).click(current.message.title);
    I.retry(5).see(current.message.id);
    I.retry(5).click('Sett forsendelse mottatt');
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'LEVERT', 'LEST']);
});

let dpvExamples = new DataTable(['senderBaseUrl', 'message', 'receiversUser']);
dpvExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '0192:810074582',
        process: 'urn:no:difi:profile:arkivmelding:administrasjon:ver1.0',
        documentType: 'urn:no:difi:arkivmelding:xsd::arkivmelding',
        businessMessage: {
            arkivmelding: {}
        },
        files: [{
            name: 'arkivmelding.xml',
            title: 'Arkivmelding',
            contentType: 'application/xml'
        }, {
            name: 'test.pdf',
            title: 'Test',
            contentType: 'application/octet-stream'
        }]
    },
    {
        personalIdNumber: '23066702197',
        password: 'password01',
        pinCode: '12345'
    }
]);

Data(dpvExamples).Scenario('Saksbehandling med Altinn Digital Post (DPV)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'LEVERT']);
    I.amOnPage('https://tt02.altinn.no/');
    I.click('Logg inn');
    await loginUsingIdporten(I, current.receiversUser);
    I.retry(5).click('.a-personSwitcher');
    I.retry(5).click('810 074 582');
    I.fillField('#inbox_search', current.message.title);
    I.pressKey('Enter');
    I.see('1 treff')
    I.click(current.message.title);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'LEVERT', 'LEST']);
});

let dpiEboksExamples = new DataTable(['senderBaseUrl', 'message', 'receiversUser']);
dpiEboksExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '07126700169',
        process: 'urn:no:difi:profile:digitalpost:vedtak:ver1.0',
        documentType: 'urn:no:difi:digitalpost:xsd:digital::digital',
        businessMessage: {
            digital: {
                sikkerhetsnivaa: 3,
                hoveddokument: 'test.pdf',
                tittel: 'Digdir e2e-test',
                spraak: 'nb',
                digitalPostInfo: {
                    virkningsdato: '2022-01-01',
                    aapningskvittering: false
                }
            }
        },
        files: [{
            name: 'test.pdf',
            title: 'Digdir e2e-test ' + randomString(),
            contentType: 'application/octet-stream'
        }]
    },
    {
        personalIdNumber: '07126700169',
        password: 'password01',
        pinCode: '12345'
    }
]);

Data(dpiEboksExamples).Scenario('Vedtak til innbygger med Digital Post til innbyggere (DPI eBoks)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'MOTTATT', 'LEVERT']);
    I.amOnPage('http://demo2-www.e-boks.no/');
    I.click('Logg inn via ID-porten');
    await loginUsingIdporten(I, current.receiversUser);
    I.retry(5).click('Ulest post');
    I.retry(5).seeInSource(current.message.files[0].title);
    I.retry(5).click('div[class="mailTitleText"]');
});

let dpiDigipostExamples = new DataTable(['senderBaseUrl', 'message', 'receiversUser']);
dpiDigipostExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '06068700602',
        process: 'urn:no:difi:profile:digitalpost:vedtak:ver1.0',
        documentType: 'urn:no:difi:digitalpost:xsd:digital::digital',
        businessMessage: {
            digital: {
                sikkerhetsnivaa: 3,
                hoveddokument: 'test.pdf',
                tittel: 'Digdir e2e-test',
                spraak: 'nb',
                digitalPostInfo: {
                    virkningsdato: '2022-01-01',
                    aapningskvittering: false
                }
            }
        },
        files: [{
            name: 'test.pdf',
            title: 'Digdir e2e-test ' + randomString(),
            contentType: 'application/octet-stream'
        }]
    },
    {
        personalIdNumber: '06068700602',
        password: 'password01',
        pinCode: '12345'
    }
]);

Data(dpiDigipostExamples).Scenario('Vedtak til innbygger med Digital Post til innbyggere (DPI Digipost)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'MOTTATT', 'LEVERT']);
    I.amOnPage('https://www.difitest.digipost.no/app/#/');
    I.click('.eid-login');
    loginUsingIdporten(I, current.receiversUser);
    I.retry(5).click(current.message.files[0].title);
});

let dpiPrintExamples = new DataTable(['senderBaseUrl', 'message']);
dpiPrintExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '04817197073',
        process: 'urn:no:difi:profile:digitalpost:vedtak:ver1.0',
        documentType: 'urn:no:difi:digitalpost:xsd:fysisk::print',
        businessMessage: {
            print: {
                hoveddokument: 'test.pdf',
                mottaker: {
                    navn: 'Test Testesen'
                }
            }
        },
        files: [{
            name: 'test.pdf',
            title: 'Digdir e2e-test ' + randomString(),
            contentType: 'application/octet-stream'
        }]
    }
]);

Data(dpiPrintExamples).Scenario('Vedtak til innbygger med Digital Post til innbyggere (DPI print)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'MOTTATT', 'LEVERT']);
});

let dpiAltinnExamples = new DataTable(['senderBaseUrl', 'message', 'receiversUser']);
dpiAltinnExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '06068800461',
        process: 'urn:no:difi:profile:digitalpost:info:ver1.0',
        documentType: 'urn:no:difi:digitalpost:xsd:digital::digital_dpv',
        businessMessage: {
            digital_dpv: {
                tittel: 'Digdir e2e-test ' + randomString(),
                sammendrag: 'Digdir e2e-test sammendrag',
                innhold: 'Digdir e2e-test innhold'
            }
        },
        files: [{
            name: 'test.pdf',
            title: 'Digdir e2e-test',
            contentType: 'application/octet-stream'
        }]
    },
    {
        personalIdNumber: '06068800461',
        password: 'password01',
        pinCode: '12345'
    }
]);

Data(dpiAltinnExamples).Scenario('Info til innbygger med Altinn DigitalPost (DPV)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'LEVERT']);
    I.retry(5).amOnPage('https://tt02.altinn.no/');
    I.retry(5).click('Logg inn');
    await loginUsingIdporten(I, current.receiversUser);
    I.retry(5).fillField('#inbox_search', current.message.businessMessage.digital_dpv.tittel);
    I.pressKey('Enter');
    I.see('1 treff')
    I.click(current.message.businessMessage.digital_dpv.tittel);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'LEVERT', 'LEST']);
});

let dpeExamples = new DataTable(['senderBaseUrl', 'message']);
dpeExamples.add([
    'http://localhost:9093/api',
    {
        receiver: '0192:991825827',
        process: 'urn:no:difi:profile:einnsyn:journalpost:ver1.0',
        documentType: 'urn:no:difi:einnsyn:xsd::publisering',
        businessMessage: {
            publisering: {
                orgnr: '991825827'
            }
        },
        files: [{
            name: 'test.jsonld',
            title: 'test.jsonld',
            contentType: 'application/json'
        }]
    }
]);

Data(dpeExamples).Scenario('Journalpost til eInnsyn (DPE)', async ({I, current}) => {
    await sendMessage(I, current.senderBaseUrl, current.message);
    await waitForStatuses(I, current.senderBaseUrl, current.message, ['OPPRETTET', 'SENDT', 'MOTTATT', 'LEVERT']);
    await waitForMessageReceipt(I, current.senderBaseUrl, current.message);
});

export {}
