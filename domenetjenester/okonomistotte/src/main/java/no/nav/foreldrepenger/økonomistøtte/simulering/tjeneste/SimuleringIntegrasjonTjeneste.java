package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static no.nav.foreldrepenger.økonomistøtte.simulering.klient.OppdragsKontrollDtoMapper.tilDto;

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

    public void startSimulering(Optional<Oppdragskontroll> oppdragskontrollOpt) {
        if (oppdragskontrollOpt.isPresent() && erOppdragskontrollGyldigOgHarOppdragÅSimulere(oppdragskontrollOpt.get())) {
            restKlient.startSimulering(tilDto(oppdragskontrollOpt.get()));
        } else {
            if (oppdragskontrollOpt.isPresent()) {
                LOG.info("Ingen oppdrag å simulere for behandling {}", oppdragskontrollOpt.get().getBehandlingId());
            } else {
                LOG.info("Ingen oppdrag å simulere fordi oppdragskontroll er null");
            }
        }
    }

    private static boolean erOppdragskontrollGyldigOgHarOppdragÅSimulere(Oppdragskontroll oppdragskontroll) {
        Objects.requireNonNull(oppdragskontroll.getBehandlingId());
        Objects.requireNonNull(oppdragskontroll.getOppdrag110Liste());
        return !oppdragskontroll.getOppdrag110Liste().isEmpty();
    }

    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "Utviklerfeil: behandlingId kan ikke være null");
        return restKlient.hentResultat(behandlingId);
    }
}
