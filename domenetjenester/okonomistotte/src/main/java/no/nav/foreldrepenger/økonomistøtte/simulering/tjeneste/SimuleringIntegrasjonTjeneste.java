package no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste;

import static no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste.tilOppdragKontrollDto;
import static no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste.tilOppdragXml;
import static no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste.SimulerOppdragIntegrasjonTjenesteFeil.startSimuleringFeiletMedFeilmelding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.kontrakter.simulering.request.OppdragskontrollDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimulerOppdragDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;
import no.nav.vedtak.exception.IntegrasjonException;

@ApplicationScoped
public class SimuleringIntegrasjonTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SimuleringIntegrasjonTjeneste.class);
    private static final Environment ENV = Environment.current();

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
            var oppdragskontroll = oppdragskontrollOpt.get();
            startSimuleringOLD(oppdragskontroll.getBehandlingId(), tilOppdragXml(oppdragskontroll));

            if (ENV.isDev()) {
                // Kjører simulering av samme oppdragskontroll, men uten persistering i databasen, logger avvik og er failsafe
                startSimuleringViaFpWsProxyOgSammenlingFailsafe(tilOppdragKontrollDto(oppdragskontroll));
            }

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

    protected void startSimuleringOLD(Long behandlingId, List<String> oppdragListe) {
        var dto = map(behandlingId, oppdragListe);
        try {
            restKlient.startSimulering(dto);
        } catch (IntegrasjonException e) {
            throw startSimuleringFeiletMedFeilmelding(behandlingId, e);
        }
    }

    protected void startSimuleringViaFpWsProxyOgSammenlingFailsafe(OppdragskontrollDto oppdragskontrollDto) {
        try {
            restKlient.startSimuleringFpWsProxy(oppdragskontrollDto);
        } catch (Exception e) {
            LOG.info("Simulering via fp-ws-proxy feilet. Sjekk secure logg til fpoppdrag/fp-ws-proxy");
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
