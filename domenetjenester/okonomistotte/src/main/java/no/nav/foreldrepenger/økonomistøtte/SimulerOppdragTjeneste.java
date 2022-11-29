package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

@ApplicationScoped
@ActivateRequestContext
public class SimulerOppdragTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SimulerOppdragTjeneste.class);

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private OppdragInputTjeneste oppdragInputTjeneste;

    SimulerOppdragTjeneste() {
        // for CDI
    }

    @Inject
    public SimulerOppdragTjeneste(OppdragskontrollTjeneste nyOppdragskontrollTjeneste,
                                  OppdragInputTjeneste oppdragInputTjeneste) {
        this.oppdragskontrollTjeneste = nyOppdragskontrollTjeneste;
        this.oppdragInputTjeneste = oppdragInputTjeneste;
    }

    /**
     * Henter ut oppdragskontroll for en gitt behandlingsid
     * @param behandlingId
     * @return
     */
    public Optional<Oppdragskontroll> hentOppdragskontrollForBehandling(Long behandlingId) {
        LOG.info("Simulerer behandlingId: {}", behandlingId);
        var input = oppdragInputTjeneste.lagSimuleringInput(behandlingId);
        return oppdragskontrollTjeneste.simulerOppdrag(input);
    }
}
