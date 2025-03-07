package no.nav.foreldrepenger.domene.medlem.medl2;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.vedtak.felles.integrasjon.rest.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Dokumentasjon https://github.com/navikt/medlemskap-medl/wiki
 * Swagger: https://medlemskap-medl-api.dev.intern.nav.no/swagger-ui/index.html
 */
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "medl2p.rs.url", endpointDefault = "http://medlemskap-medl-api.team-rocket/rest/v1/periode/soek",
    scopesProperty = "medl2.scopes", scopesDefault = "api://prod-fss.team-rocket.medlemskap-medl-api/.default")
@ApplicationScoped
public class MedlemsperioderRestKlient {


    // Fra kodeverk PeriodestatusMedl
    public static final String KODE_PERIODESTATUS_GYLD = "GYLD";
    public static final String KODE_PERIODESTATUS_UAVK = "UAVK";

    private final RestClient restKlient;
    private final RestConfig restConfig;

    public MedlemsperioderRestKlient() {
        this.restKlient = RestClient.client();
        this.restConfig = RestConfig.forClient(MedlemsperioderRestKlient.class);
    }

    public List<Medlemskapsunntak> finnMedlemsunntak(String aktørId, LocalDate fom, LocalDate tom) {
        var medlemDto = new MedlemRequest(aktørId, null, List.of(KODE_PERIODESTATUS_GYLD, KODE_PERIODESTATUS_UAVK), List.of(),
            fom, tom, true);
        var request = RestRequest.newPOSTJson(medlemDto, restConfig.endpoint(), restConfig)
            .otherCallId(NavHeaders.HEADER_NAV_CALL_ID);
        var match = restKlient.send(request, Medlemskapsunntak[].class);
        return Arrays.asList(match);
    }


    public record MedlemRequest(String personident, String type, List<String> statuser, List<String> ekskluderKilder,
                                LocalDate fraOgMed, LocalDate tilOgMed, boolean inkluderSporingsinfo) {
    }

}
