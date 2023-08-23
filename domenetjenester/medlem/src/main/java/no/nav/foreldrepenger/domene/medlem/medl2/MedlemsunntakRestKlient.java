package no.nav.foreldrepenger.domene.medlem.medl2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.felles.integrasjon.rest.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 *             Dokumentasjon https://github.com/navikt/medlemskap-medl/wiki/medl2-%E2%86%92-medlemskap-medl-api
 *             Swagger: ukjent
 */
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "medl2.rs.url", endpointDefault = "http://medlemskap-medl-api.team-rocket/api/v1/medlemskapsunntak",
    scopesProperty = "medl2.scopes", scopesDefault = "api://prod-fss.team-rocket.medlemskap-medl-api/.default")
@ApplicationScoped
public class MedlemsunntakRestKlient implements Medlemskap {

    public static final String PARAM_FRA_OG_MED = "fraOgMed";
    public static final String PARAM_TIL_OG_MED = "tilOgMed";
    public static final String PARAM_STATUSER = "statuser";
    public static final String PARAM_INKLUDER_SPORINGSINFO = "inkluderSporingsinfo";

    // Fra kodeverk PeriodestatusMedl
    public static final String KODE_PERIODESTATUS_GYLD = "GYLD";
    public static final String KODE_PERIODESTATUS_UAVK = "UAVK";

    private final RestClient restKlient;
    private final RestConfig restConfig;

    public MedlemsunntakRestKlient() {
        this.restKlient = RestClient.client();
        this.restConfig = RestConfig.forClient(MedlemsunntakRestKlient.class);
    }

    @Override
    public List<Medlemskapsunntak> finnMedlemsunntak(String aktørId, LocalDate fom, LocalDate tom) throws Exception {
        var uri = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam(PARAM_INKLUDER_SPORINGSINFO, String.valueOf(true))
            .queryParam(PARAM_FRA_OG_MED, d2s(fom))
            .queryParam(PARAM_TIL_OG_MED, d2s(tom))
            .queryParam(PARAM_STATUSER, KODE_PERIODESTATUS_GYLD)
            .queryParam(PARAM_STATUSER, KODE_PERIODESTATUS_UAVK);
        var request = RestRequest.newGET(uri.build(), restConfig)
            .otherCallId(NavHeaders.HEADER_NAV_CALL_ID)
            .header(NavHeaders.HEADER_NAV_PERSONIDENT, aktørId);
        var match = restKlient.send(request, Medlemskapsunntak[].class);
        return Arrays.asList(match);
    }

    private static String d2s(LocalDate dato) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(dato);
    }

}
