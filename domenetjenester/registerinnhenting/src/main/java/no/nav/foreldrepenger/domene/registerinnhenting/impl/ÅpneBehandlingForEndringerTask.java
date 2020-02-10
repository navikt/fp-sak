package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import static no.nav.foreldrepenger.domene.registerinnhenting.impl.ÅpneBehandlingForEndringerTask.TASKTYPE;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class ÅpneBehandlingForEndringerTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "behandlingskontroll.åpneBehandlingForEndringer";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingRepository behandlingRepository;

    ÅpneBehandlingForEndringerTask() {
        // for CDI proxy
    }

    @Inject
    public ÅpneBehandlingForEndringerTask(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                          BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        StartpunktType startpunkt = StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        reaktiverAksjonspunkter(kontekst, behandling, startpunkt);
        behandling.setÅpnetForEndring(true);
        behandlingskontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, startpunkt.getBehandlingSteg());
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
    }

    private void reaktiverAksjonspunkter(BehandlingskontrollKontekst kontekst, Behandling behandling, StartpunktType startpunkt) {
        Set<String> aksjonspunkterFraOgMedStartpunkt = behandlingskontrollTjeneste
            .finnAksjonspunktDefinisjonerFraOgMed(behandling, startpunkt.getBehandlingSteg(), true);

        behandling.getAksjonspunkter().stream()
            .filter(ap -> aksjonspunkterFraOgMedStartpunkt.contains(ap.getAksjonspunktDefinisjon().getKode()))
            .filter(ap -> !AksjonspunktType.AUTOPUNKT.equals(ap.getAksjonspunktDefinisjon().getAksjonspunktType()))
            .forEach(ap -> behandlingskontrollTjeneste.lagreAksjonspunkterReåpnet(kontekst, List.of(ap), Optional.of(Boolean.TRUE)));
    }
}
