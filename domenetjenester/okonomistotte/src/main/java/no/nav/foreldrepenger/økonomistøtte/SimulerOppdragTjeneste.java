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

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.NyOppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ny.toggle.OppdragKjerneimplementasjonToggle;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class SimulerOppdragTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SimulerOppdragTjeneste.class);

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private OppdragskontrollTjeneste esOppdragskontrollTjeneste;
    private OppdragskontrollTjeneste nyOppdragskontrollTjeneste;
    private OppdragInputTjeneste oppdragInputTjeneste;
    private OppdragKjerneimplementasjonToggle toggle;

    SimulerOppdragTjeneste() {
        // for CDI
    }

    @Inject
    public SimulerOppdragTjeneste(@Named("oppdragTjeneste") OppdragskontrollTjeneste oppdragskontrollTjeneste,
                                  @Named("oppdragEngangstønadTjeneste") OppdragskontrollTjeneste esOppdragskontrollTjeneste,
                                  @Named("nyOppdragTjeneste") OppdragskontrollTjeneste nyOppdragskontrollTjeneste,
                                  OppdragInputTjeneste oppdragInputTjeneste,
                                  OppdragKjerneimplementasjonToggle toggle) {
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
        this.esOppdragskontrollTjeneste = esOppdragskontrollTjeneste;
        this.nyOppdragskontrollTjeneste = nyOppdragskontrollTjeneste;
        this.oppdragInputTjeneste = oppdragInputTjeneste;
        this.toggle = toggle;
    }

    /**
     * Generer XMLene som skal sendes over til oppdrag for simulering. Det lages en XML per Oppdrag110.
     * Vi har en Oppdrag110-linje per oppdragsmottaker.
     *
     * @param behandlingId   behandling.id
     * @param fagsakYtelseType
     * @return En liste med XMLer som kan sendes over til oppdrag
     */
    public List<String> simulerOppdrag(Long behandlingId, FagsakYtelseType fagsakYtelseType) {
        log.info("Oppretter simuleringsoppdrag for behandling: {}", behandlingId); //$NON-NLS-1$
        boolean brukNyImplementasjon = toggle.brukNyImpl(behandlingId);

        Optional<Oppdragskontroll> oppdragskontrollOpt;

        if (fagsakYtelseType.equals(FagsakYtelseType.ENGANGSTØNAD)) {
            log.info("Simulerer engangsstønad for behandlingId: {}", behandlingId);
            oppdragskontrollOpt = esOppdragskontrollTjeneste.simulerOppdrag(behandlingId);
        } else {
            if (brukNyImplementasjon) {
                log.info("Gjennomfører simulering for behandling med id={} med ny implementasjon", behandlingId);
                var input = oppdragInputTjeneste.lagInput(behandlingId);
                oppdragskontrollOpt = nyOppdragskontrollTjeneste.simulerOppdrag(input);
            } else {
                oppdragskontrollOpt = oppdragskontrollTjeneste.simulerOppdrag(behandlingId);
            }
        }

        if (oppdragskontrollOpt.isPresent()) {
            ØkonomioppdragMapper mapper = new ØkonomioppdragMapper(oppdragskontrollOpt.get());
            return mapper.generateOppdragXML();
        }
        return Collections.emptyList();
    }
}
