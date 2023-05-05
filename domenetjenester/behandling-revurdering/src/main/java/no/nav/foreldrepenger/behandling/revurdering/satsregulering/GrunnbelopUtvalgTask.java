package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask("behandlingsprosess.gregulering")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class GrunnbelopUtvalgTask implements ProsessTaskHandler {

    static final String YTELSE_KEY = "manuell";
    static final String MANUELL_KEY = "manuell";

    private static final Logger LOG = LoggerFactory.getLogger(GrunnbelopUtvalgTask.class);
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private BehandlingFlytkontroll flytkontroll;

    GrunnbelopUtvalgTask() {
        // for CDI proxy
    }

    @Inject
    public GrunnbelopUtvalgTask(BehandlingRepositoryProvider repositoryProvider,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                BehandlendeEnhetTjeneste enhetTjeneste,
                                BehandlingFlytkontroll flytkontroll) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.enhetTjeneste = enhetTjeneste;
        this.flytkontroll = flytkontroll;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        // For å sikre at fagsaken hentes opp i cache - ellers dukker den opp via readonly-query og det blir problem.



    }
}
