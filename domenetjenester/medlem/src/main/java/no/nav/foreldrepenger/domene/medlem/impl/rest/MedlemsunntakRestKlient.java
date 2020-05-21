package no.nav.foreldrepenger.domene.medlem.impl.rest;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class MedlemsunntakRestKlient {

    private static final String ENDPOINT_KEY = "medl2.rs.url";
    private static final String DEFAULT_URI = "https://app.adeo.no/medl2/api/v1/medlemskapsunntak";
    private static final String HEADER_CALL_ID = "Nav-Call-Id";
    private static final String HEADER_PERSONIDENT = "Nav-Personident";
    // "ARBEIDSFORDELING_RS_URL": "https://app.adeo.no/medl2/api/v1/medlemskapsunntak",

    private OidcRestClient oidcRestClient;
    private URI endpoint;

    public MedlemsunntakRestKlient() {
    }

    @Inject
    public MedlemsunntakRestKlient(OidcRestClient oidcRestClient,
                                   @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient ;
        this.endpoint = endpoint;
    }

    public List<MedlemskapsunntakForGet> finnMedlemsunntak(AktørId aktørId, LocalDate fom, LocalDate tom) throws Exception {
        URIBuilder builder = new URIBuilder(this.endpoint)
            .addParameter("inkluderSporingsinfo", String.valueOf(true))
            .addParameter("fraOgMed", d2s(fom))
            .addParameter("tilOgMed", d2s(tom));
        var match = this.oidcRestClient.get(builder.build(), this.lagHeader(aktørId), MedlemskapsunntakForGet[].class);
        return Arrays.asList(match);
    }

    private Set<Header> lagHeader(AktørId aktørId) {
        return Set.of(new BasicHeader(HEADER_CALL_ID, MDCOperations.getCallId()),
            new BasicHeader(HEADER_PERSONIDENT, aktørId.getId()));
    }

    private static String d2s(LocalDate dato) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(dato);
    }

}
