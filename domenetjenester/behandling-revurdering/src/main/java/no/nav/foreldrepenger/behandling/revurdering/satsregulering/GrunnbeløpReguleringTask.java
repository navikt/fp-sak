package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Dependent
@ProsessTask("behandlingsprosess.gregulering.reguler")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class GrunnbeløpReguleringTask extends FagsakProsessTask {

    public static final String MANUELL_KEY = "manuell";

    private static final Logger LOG = LoggerFactory.getLogger(GrunnbeløpReguleringTask.class);

    private final BehandlingRepository behandlingRepository;
    private final FagsakRepository fagsakRepository;
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private final SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private final BehandlendeEnhetTjeneste enhetTjeneste;
    private final BehandlingFlytkontroll flytkontroll;

    @Inject
    public GrunnbeløpReguleringTask(BehandlingRepositoryProvider repositoryProvider,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                    BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                    BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                    BehandlendeEnhetTjeneste enhetTjeneste,
                                    BehandlingFlytkontroll flytkontroll) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.enhetTjeneste = enhetTjeneste;
        this.flytkontroll = flytkontroll;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // For å sikre at fagsaken hentes opp i cache - ellers dukker den opp via readonly-query og det blir problem.
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        // Implisitt precondition fra utvalget i batches: Ingen ytelsesbehandlinger
        // utenom evt berørt behandling.
        var åpneYtelsesBehandlinger = behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsakId);
        if (åpneYtelsesBehandlinger) {
            LOG.info("GrunnbeløpRegulering finnes allerede åpen revurdering på fagsakId = {}", fagsakId);
            return;
        }
        if (prosessTaskData.getPropertyValue(MANUELL_KEY) == null) {
            var sisteVedtatte = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).orElseThrow();
            var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkterForAvsluttetBehandling(sisteVedtatte.getId());
            var grunnbeløpFraSisteVedtatt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(sisteVedtatte.getId())
                .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
                .map(Beløp::getVerdi).map(BigDecimal::longValue).orElse(0L);
            var skalBrukeGrunnbeløp = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, skjæringstidspunkt.getFørsteUttaksdatoGrunnbeløp()).getVerdi();
            if (grunnbeløpFraSisteVedtatt == skalBrukeGrunnbeløp) {
                LOG.info("GrunnbeløpRegulering har rett G for saksnummer = {} stp {}", fagsak.getSaksnummer().getVerdi(), skjæringstidspunkt.getFørsteUttaksdatoGrunnbeløp());
                return;
            }
        }

        var skalKøes = flytkontroll.nyRevurderingSkalVente(fagsak);
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.RE_SATS_REGULERING, enhet);

        LOG.info("GrunnbeløpRegulering har opprettet revurdering på saksnummer = {}", fagsak.getSaksnummer().getVerdi());

        if (skalKøes) {
            flytkontroll.settNyRevurderingPåVent(revurdering);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }


    }
}
