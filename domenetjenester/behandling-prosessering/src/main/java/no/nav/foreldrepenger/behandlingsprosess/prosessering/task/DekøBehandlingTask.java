package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Tar en behandling av kø. Gjenopptar dersom den ikke er på vent av andre årsaker (kompletthet, mm)
 */
@ApplicationScoped
@ProsessTask("behandlingskontroll.dekøBehandling")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class DekøBehandlingTask extends BehandlingProsessTask {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    DekøBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public DekøBehandlingTask(BehandlingRepository behandlingRepository, BehandlingLåsRepository låsRepository,
                              BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        super(låsRepository);
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {

        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);

        if (behandling.erKøet()) {
            var køAksjonspunkt = behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING).orElseThrow();
            behandlingskontrollTjeneste.settAutopunktTilUtført(kontekst, behandling, køAksjonspunkt);
        }

        var nå = LocalDateTime.now();
        if (behandling.isBehandlingPåVent()) {
            var skalVentePåGjenopptak = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
                .anyMatch(a -> a.getFristTid() != null && a.getFristTid().isAfter(nå));
            if (skalVentePåGjenopptak) {
                return;
            } else {
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(kontekst, behandling);
            }
        }

        if (erRegisterinnhentingPassert(behandling)) {
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, nå);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }

    }

    private boolean erRegisterinnhentingPassert(Behandling behandling) {
        return behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, BehandlingStegType.INNHENT_REGISTEROPP);
    }

}
