package no.nav.foreldrepenger.økonomistøtte;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.ny.toggle.OppdragKjerneimplementasjonToggle;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SimulerOppdragTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SimulerOppdragTjeneste.class);

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private OppdragskontrollTjeneste nyOppdragskontrollTjeneste;
    private OppdragKjerneimplementasjonToggle toggle;

    SimulerOppdragTjeneste() {
        // for CDI
    }

    @Inject
    public SimulerOppdragTjeneste(@Named("oppdragTjeneste") OppdragskontrollTjeneste oppdragskontrollTjeneste, @Named("nyOppdragTjeneste") OppdragskontrollTjeneste nyOppdragskontrollTjeneste, OppdragKjerneimplementasjonToggle toggle) {
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
        this.nyOppdragskontrollTjeneste = nyOppdragskontrollTjeneste;
        this.toggle = toggle;
    }

    /**
     * Generer XMLene som skal sendes over til oppdrag for simulering. Det lages en XML per Oppdrag110.
     * Vi har en Oppdrag110-linje per oppdragsmottaker.
     *
     * @param behandlingId   behandling.id
     * @param ventendeTaskId TaskId til ventende prosessTask
     * @return En liste med XMLer som kan sendes over til oppdrag
     */
    public List<String> simulerOppdrag(Long behandlingId, Long ventendeTaskId) {
        log.info("Oppretter simuleringsoppdrag for behandling: {}", behandlingId); //$NON-NLS-1$
        boolean brukNyImplementasjon = toggle.brukNyImpl(behandlingId);
        Optional<Oppdragskontroll> oppdragskontrollOpt;
        if (brukNyImplementasjon) {
            log.info("Gjennomfører simulering for behandling med id={} med ny implementasjon", behandlingId);
            oppdragskontrollOpt = nyOppdragskontrollTjeneste.opprettOppdrag(behandlingId, ventendeTaskId, true);
        } else {
            oppdragskontrollOpt = oppdragskontrollTjeneste.opprettOppdrag(behandlingId, ventendeTaskId);
        }
        if (oppdragskontrollOpt.isPresent()) {
            ØkonomioppdragMapper mapper = new ØkonomioppdragMapper(oppdragskontrollOpt.get());
            return mapper.generateOppdragXML();
        }
        return Collections.emptyList();
    }
}
