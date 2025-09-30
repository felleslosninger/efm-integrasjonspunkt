package no.difi.meldingsutveksling.altinnv3.proxy;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class TokenFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // get the token from cache / maskinporten
        var token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxOUFGOTRFNDQ1MzE0Q0RDMjk1Rjk1MjUzODU4MDU0RjhCQ0FDODYiLCJ4NXQiOiJjWnI1VGtSVEZNM0NsZmxTVTRXQVZQaThySVkiLCJ0eXAiOiJKV1QifQ.eyJzY29wZSI6ImFsdGlubjpjb3JyZXNwb25kZW5jZS5yZWFkIGFsdGlubjpzZXJ2aWNlb3duZXIgYWx0aW5uOmNvcnJlc3BvbmRlbmNlLndyaXRlIiwidG9rZW5fdHlwZSI6IkJlYXJlciIsImV4cCI6MTc1OTE1ODIxNywiaWF0IjoxNzU5MTU2NDE3LCJjbGllbnRfaWQiOiJhNjNjYWM5MS0zMjEwLTRjMzUtYjk2MS01YzdiZjEyMjM0NWMiLCJjb25zdW1lciI6eyJhdXRob3JpdHkiOiJpc282NTIzLWFjdG9yaWQtdXBpcyIsIklEIjoiMDE5Mjo5OTE4MjU4MjcifSwidXJuOmFsdGlubjpvcmciOiJkaWdkaXIiLCJ1cm46YWx0aW5uOm9yZ051bWJlciI6Ijk5MTgyNTgyNyIsInVybjphbHRpbm46YXV0aGVudGljYXRlbWV0aG9kIjoibWFza2lucG9ydGVuIiwidXJuOmFsdGlubjphdXRobGV2ZWwiOjMsImlzcyI6Imh0dHBzOi8vcGxhdGZvcm0udHQwMi5hbHRpbm4ubm8vYXV0aGVudGljYXRpb24vYXBpL3YxL29wZW5pZC8iLCJqdGkiOiIxMGI1MTVhNS0yMTE5LTQ1NGItODI0MS1jZDg5YmQ4ZGQxZTEiLCJuYmYiOjE3NTkxNTY0MTd9.eDcc1FY2jo4xOE5bS17NfIJ8mr_d5Eyq5wZkgCWm4RlGrbFiJYeqsaeBEfvfNShVNoypK1SCHq05F7yX4Jhgb_ErjsYfaf29_JrMBmoGRRMkHV53mxllHdNE3I79MpK2xX4UYU0i45npuFy9OVog0SCLd1bcNwVADB4W994u-eGEfBMUzgrTp3jxF3yRxup_QYBfnzuAop78ZqXlVen9s_S9OXnzgFc0ZhVc4MEYqqKkf5qoC5Ytut5Kl90l-2ByCR0iubA2GeNN_ZAUri8hTXclbGt_7j_zMRP8T8rC2HCHa1gusjcNM-sj35VL4y_-CvOPtpD-AFx1w_TqfTdUow";

        // right now just pass on the original token from the request
        token = exchange.getRequest().getHeaders().getFirst("Authorization").substring(7);

        var request = exchange.getRequest().mutate()
            .header("Authorization", "Bearer " + token)
            .build();

        return chain.filter(exchange.mutate().request(request).build());

    }

}
