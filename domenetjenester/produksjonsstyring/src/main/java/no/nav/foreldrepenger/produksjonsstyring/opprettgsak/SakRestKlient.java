package no.nav.foreldrepenger.produksjonsstyring.opprettgsak;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;

import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class SakRestKlient {

    private static final String ENDPOINT_KEY = "sak.rs.url";
    private static final String DEFAULT_URI = "http://sak.default/api/v1/saker";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private OidcRestClient oidcRestClient;
    private URI endpoint;

    public SakRestKlient() {
    }

    @Inject
    public SakRestKlient(OidcRestClient oidcRestClient,
                         @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient ;
        this.endpoint = endpoint;
    }

    public Saksnummer opprettSakUtenSaksnummer(Tema tema, Fagsystem fagsystem, AktørId aktørId) {
        var request = SakJson.getBuilder()
            .medAktoerId(aktørId.getId())
            .medApplikasjon(fagsystem.getOffisiellKode())
            .medTema(tema.getOffisiellKode());
        var sak = oidcRestClient.post(endpoint, request.build(), lagHeader(), SakJson.class);
        return new Saksnummer(String.valueOf(sak.getId()));
    }

    public Saksnummer opprettSakMedSaksnummer(Tema tema, Fagsystem fagsystem, AktørId aktørId, Saksnummer saksnummer) {
        var request = SakJson.getBuilder()
            .medAktoerId(aktørId.getId())
            .medApplikasjon(fagsystem.getOffisiellKode())
            .medFagsakNr(saksnummer.getVerdi())
            .medTema(tema.getOffisiellKode());
        var sak = oidcRestClient.post(endpoint, request.build(), lagHeader(), SakJson.class);
        return new Saksnummer(sak.getFagsakNr());
    }

    public Saksnummer finnArkivSakIdForSaksnummer(Fagsystem fagsystem, Saksnummer saksnummer) throws Exception {
        URIBuilder builder = new URIBuilder(this.endpoint)
            .addParameter("fagsakNr", saksnummer.getVerdi())
            .addParameter("applikasjon", fagsystem.getOffisiellKode());
        var match = this.oidcRestClient.get(builder.build(), this.lagHeader(), SakJson[].class);
        var arkivSakId = Arrays.stream(match).map(SakJson::getId).map(String::valueOf).findFirst().orElse(null);
        return arkivSakId != null ? new Saksnummer(arkivSakId) : null;
    }

    private Set<Header> lagHeader() {
        return Collections.singleton(new BasicHeader(HEADER_CORRELATION_ID, MDCOperations.getCallId()));
    }

}
