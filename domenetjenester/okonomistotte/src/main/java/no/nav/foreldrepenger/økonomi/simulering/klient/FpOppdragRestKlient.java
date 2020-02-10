package no.nav.foreldrepenger.økonomi.simulering.klient;

import java.net.URI;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimulerOppdragDto;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class FpOppdragRestKlient {

    private static final String FPOPPDRAG_START_SIMULERING = "/simulering/start";
    private static final String FPOPPDRAG_HENT_RESULTAT = "/simulering/resultat";


    private OidcRestClient restClient;
    private URI uriStartSimulering;
    private URI uriHentResultat;

    public FpOppdragRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FpOppdragRestKlient(OidcRestClient restClient) {
        this.restClient = restClient;
        String fpoppdragBaseUrl = FpoppdragFelles.getFpoppdragBaseUrl();
        uriStartSimulering = URI.create(fpoppdragBaseUrl + FPOPPDRAG_START_SIMULERING);
        uriHentResultat = URI.create(fpoppdragBaseUrl + FPOPPDRAG_HENT_RESULTAT);
    }

    /**
     * Starter en simulering for gitt behandling med oppdrag XMLer.
     * @param request med behandlingId og liste med oppdrag-XMLer
     */
    public void startSimulering(SimulerOppdragDto request) {
        restClient.post(uriStartSimulering, request);
    }

    /**
     * Henter simuleringresultat for behandling hvis det finnes.
     * @param behandlingId
     * @return Optional med SimuleringResultatDto kan være tom
     */
    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        return restClient.postReturnsOptional(uriHentResultat, new BehandlingIdDto(behandlingId), SimuleringResultatDto.class);
    }

}
