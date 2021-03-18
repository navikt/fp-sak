package no.nav.foreldrepenger.økonomistøtte;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SimulerOppdragTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SimulerOppdragTjeneste.class);

    private OppdragskontrollTjeneste nyOppdragskontrollTjeneste;
    private OppdragInputTjeneste oppdragInputTjeneste;

    SimulerOppdragTjeneste() {
        // for CDI
    }

    @Inject
    public SimulerOppdragTjeneste(@Named("nyOppdragTjeneste") OppdragskontrollTjeneste nyOppdragskontrollTjeneste,
                                  OppdragInputTjeneste oppdragInputTjeneste) {
        this.nyOppdragskontrollTjeneste = nyOppdragskontrollTjeneste;
        this.oppdragInputTjeneste = oppdragInputTjeneste;
    }

    /**
     * Generer XMLene som skal sendes over til oppdrag for simulering. Det lages en XML per Oppdrag110.
     * Vi har en Oppdrag110-linje per oppdragsmottaker.
     *
     * @param behandlingId   behandling.id
     * @return En liste med XMLer som kan sendes over til oppdrag
     */
    public List<String> simulerOppdrag(Long behandlingId) {
        LOG.info("Simulerer fp/svp for behandlingId: {}", behandlingId);
        var input = oppdragInputTjeneste.lagSimuleringInput(behandlingId);
        var oppdragskontrollOpt = nyOppdragskontrollTjeneste.simulerOppdrag(input);

        return oppdragskontrollOpt.map(new ØkonomioppdragMapper()::generateOppdragXML).orElse(Collections.emptyList());
    }
}
