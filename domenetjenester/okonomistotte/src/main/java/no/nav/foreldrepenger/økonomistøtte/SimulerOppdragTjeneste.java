package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.kontrakter.simulering.request.OppdragskontrollDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.fpwsproxy.mapper.OppdragsKontrollDtoMapper;

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

    /**
     * Generer XMLene som skal sendes over til oppdrag for simulering fra oppdragskontroll. Det lages en XML per Oppdrag110.
     * Vi har en Oppdrag110-linje per oppdragsmottaker.
     * @return En liste med XMLer som kan sendes over til oppdrag
     */
    public static List<String> tilOppdragXml(@NotNull Oppdragskontroll oppdragskontroll) {
        return new ØkonomioppdragMapper().generateOppdragXML(oppdragskontroll);
    }

    public static OppdragskontrollDto tilOppdragKontrollDto(@NotNull Oppdragskontroll oppdragskontroll) {
        return OppdragsKontrollDtoMapper.tilDto(oppdragskontroll);
    }
}
