package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SimulerOppdragTjeneste {

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;

    private static final Logger log = LoggerFactory.getLogger(SimulerOppdragTjeneste.class);

    SimulerOppdragTjeneste() {
        // for CDI
    }

    @Inject
    public SimulerOppdragTjeneste(OppdragskontrollTjeneste oppdragskontrollTjeneste) {
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
    }

    /**
     * Generer XMLene som skal sendes over til oppdrag for simulering. Det lages en XML per Oppdrag110.
     * Vi har en Oppdrag110-linje per oppdragsmottaker.
     *
     * @param behandlingId behandling.id
     * @param ventendeTaskId TaskId til ventende prosessTask
     * @return En liste med XMLer som kan sendes over til oppdrag
     */
    public List<String> simulerOppdrag(Long behandlingId, Long ventendeTaskId) {
        log.info("Oppretter simuleringsoppdrag for behandling: {}", behandlingId); //$NON-NLS-1$
        Optional<Oppdragskontroll> oppdragskontrollOpt = oppdragskontrollTjeneste.opprettOppdrag(behandlingId, ventendeTaskId);
        if (oppdragskontrollOpt.isPresent()) {
            ØkonomioppdragMapper mapper = new ØkonomioppdragMapper(oppdragskontrollOpt.get());
            return mapper.generateOppdragXML();
        }
        return Collections.emptyList();
    }
}
