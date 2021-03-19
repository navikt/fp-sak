package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import static no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste.SimulerOppdragIntegrasjonTjenesteFeil.startSimuleringFeiletMedFeilmelding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimulerOppdragDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;
import no.nav.vedtak.exception.IntegrasjonException;

@ApplicationScoped
public class SimuleringIntegrasjonTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SimuleringIntegrasjonTjeneste.class);

    private FpOppdragRestKlient restKlient;

    public SimuleringIntegrasjonTjeneste() {
        // CDI
    }

    @Inject
    public SimuleringIntegrasjonTjeneste(FpOppdragRestKlient restKlient) {
        this.restKlient = restKlient;
    }

    public void startSimulering(Long behandlingId, List<String> oppdragListe) {
        Objects.requireNonNull(behandlingId);
        Objects.requireNonNull(oppdragListe);
        if (!oppdragListe.isEmpty()) {
            SimulerOppdragDto dto = map(behandlingId, oppdragListe);
            try {
                restKlient.startSimulering(dto);
            } catch (IntegrasjonException e) {
                throw startSimuleringFeiletMedFeilmelding(behandlingId, e);
            }
        } else {
            LOG.info("Ingen oppdrag å simulere. {}", behandlingId);
        }
    }

    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "Utviklerfeil: behandlingId kan ikke være null");
        return restKlient.hentResultat(behandlingId);
    }

    private SimulerOppdragDto map(Long behandlingId, List<String> oppdragListe) {
        return SimulerOppdragDto.lagDto(behandlingId, oppdragListe);
    }

}
