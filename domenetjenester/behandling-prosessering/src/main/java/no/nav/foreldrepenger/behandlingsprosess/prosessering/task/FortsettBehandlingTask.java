package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Kjører behandlingskontroll automatisk fra der prosessen står.
 */
@ApplicationScoped
@ProsessTask("behandlingskontroll.fortsettBehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class FortsettBehandlingTask implements ProsessTaskHandler {

    public static final String MANUELL_FORTSETTELSE = "manuellFortsettelse";
    public static final String UTFORT_AUTOPUNKT = "autopunktUtfort";
    public static final String GJENOPPTA_STEG = "gjenopptaSteg";

    private BehandlingRepository behandlingRepository;

    FortsettBehandlingTask() {
        // For CDI proxy
    }

    @Inject
    public FortsettBehandlingTask(BehandlingRepositoryProvider repositoryProvider) {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    public void doTask(ProsessTaskData data) {

        // dynamisk lookup, så slipper vi å validere bean ved oppstart i test av moduler
        // etc. før det faktisk brukes
        var cdi = CDI.current();
        var behandlingskontrollTjeneste = cdi.select(BehandlingskontrollTjeneste.class).get();

        try {
            var behandlingId = getBehandlingId(data);
            var lås = behandlingRepository.taSkriveLås(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
            var manuellFortsettelse = Optional.ofNullable(data.getPropertyValue(MANUELL_FORTSETTELSE))
                    .map(Boolean::valueOf)
                    .orElse(Boolean.FALSE);
            var gjenoppta = data.getPropertyValue(GJENOPPTA_STEG);

            var stegtype = getBehandlingStegType(gjenoppta);
            if (gjenoppta != null || manuellFortsettelse) {
                if (behandling.isBehandlingPåVent()) { // Autopunkt
                    behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
                }
            } else {
                var utført = data.getPropertyValue(UTFORT_AUTOPUNKT);
                if (utført != null) {
                    var aksjonspunkt = AksjonspunktDefinisjon.fraKode(utført);
                    behandlingskontrollTjeneste.settAutopunktTilUtført(behandling, aksjonspunkt, kontekst);
                }
            }
            // Ingen åpne autopunkt her, takk
            validerBehandlingIkkeErSattPåVent(behandling);

            // Sjekke om kan prosesserere, samt feilhåndtering vs savepoint: Ved retry av
            // feilet task som har passert gjenopptak må man fortsette.
            var tilstand = behandling.getBehandlingStegTilstand();
            if (gjenoppta != null && tilstand.isPresent() && tilstand.get().getBehandlingSteg().equals(stegtype)
                && BehandlingStegStatus.VENTER.equals(tilstand.get().getBehandlingStegStatus())) {
                behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, stegtype);
            } else if (!behandling.erAvsluttet()) {
                behandlingskontrollTjeneste.prosesserBehandling(kontekst);
            }

        } finally {
            cdi.destroy(behandlingskontrollTjeneste);
        }
    }

    private BehandlingStegType getBehandlingStegType(String gjenopptaSteg) {
        if (gjenopptaSteg == null) {
            return null;
        }
        var stegtype = BehandlingStegType.fromString(gjenopptaSteg);
        if (stegtype == null) {
            throw new IllegalStateException("Utviklerfeil: ukjent steg " + gjenopptaSteg);
        }
        return stegtype;
    }

    private void validerBehandlingIkkeErSattPåVent(Behandling behandling) {
        if (behandling.isBehandlingPåVent()) {
            throw new IllegalStateException("Utviklerfeil: Ikke tillatt å fortsette behandling på vent");
        }
    }

    private Long getBehandlingId(ProsessTaskData data) {
        return data.getBehandlingIdAsLong();
    }
}
