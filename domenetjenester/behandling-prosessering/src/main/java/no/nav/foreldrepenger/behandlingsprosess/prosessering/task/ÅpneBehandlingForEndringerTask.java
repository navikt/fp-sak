package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("behandlingskontroll.åpneBehandlingForEndringer")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class ÅpneBehandlingForEndringerTask extends BehandlingProsessTask {

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private BehandlingModellTjeneste behandlingModellTjeneste;

    ÅpneBehandlingForEndringerTask() {
        // for CDI proxy
    }

    @Inject
    public ÅpneBehandlingForEndringerTask(BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                          AksjonspunktkontrollTjeneste aksjonspunktkontrollTjeneste,
                                          ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          BehandlingModellTjeneste behandlingModellTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.aksjonspunktkontrollTjeneste = aksjonspunktkontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingModellTjeneste = behandlingModellTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erSaksbehandlingAvsluttet()) return;
        var startpunkt = StartpunktType.KONTROLLER_ARBEIDSFORHOLD;
        var startSteg = startpunkt.getBehandlingSteg();
        if (!behandlingModellTjeneste.inneholderSteg(behandling.getFagsakYtelseType(), behandling.getType(), startSteg)
            || behandlingModellTjeneste.erStegAFørStegB(behandling.getFagsakYtelseType(), behandling.getType(), behandling.getAktivtBehandlingSteg(), startSteg) ) {
            return;
        }
        arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(behandling.getId());
        reaktiverAksjonspunkter(behandling, lås, startpunkt);
        behandling.setÅpnetForEndring(true);
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, startpunkt.getBehandlingSteg());
        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
    }

    private void reaktiverAksjonspunkter(Behandling behandling, BehandlingLås skriveLås, StartpunktType startpunkt) {
        var reåpnes = behandling.getAksjonspunkter().stream()
            .filter(ap -> !ap.getAksjonspunktDefinisjon().erUtgått())
            .filter(ap -> skalAksjonspunktLøsesIEllerEtterSteg(behandling, startpunkt, ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !AksjonspunktType.AUTOPUNKT.equals(ap.getAksjonspunktDefinisjon().getAksjonspunktType()))
            .toList();
        aksjonspunktkontrollTjeneste.lagreAksjonspunkterReåpnet(behandling, skriveLås, reåpnes);
    }

    private boolean skalAksjonspunktLøsesIEllerEtterSteg(Behandling behandling, StartpunktType startpunkt, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return behandlingModellTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(behandling.getFagsakYtelseType(), behandling.getType(),
            startpunkt.getBehandlingSteg(), aksjonspunktDefinisjon);
    }
}
