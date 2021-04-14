package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

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
@ProsessTask(FortsettBehandlingTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class FortsettBehandlingTask implements ProsessTaskHandler {

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
            var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            var manuellFortsettelse = Optional.ofNullable(data.getPropertyValue(FortsettBehandlingTaskProperties.MANUELL_FORTSETTELSE))
                    .map(Boolean::valueOf)
                    .orElse(Boolean.FALSE);
            var gjenoppta = data.getPropertyValue(FortsettBehandlingTaskProperties.GJENOPPTA_STEG);

            var stegtype = getBehandlingStegType(gjenoppta);
            if ((gjenoppta != null) || manuellFortsettelse) {
                if (behandling.isBehandlingPåVent()) { // Autopunkt
                    behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
                }
            } else {
                var utført = data.getPropertyValue(FortsettBehandlingTaskProperties.UTFORT_AUTOPUNKT);
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
            if ((gjenoppta != null) && tilstand.isPresent() && tilstand.get().getBehandlingSteg().equals(stegtype)
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
        var stegtype = BehandlingStegType.fraKode(gjenopptaSteg);
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
        return data.getBehandlingId() != null ? Long.valueOf(data.getBehandlingId()) : null;
    }
}
