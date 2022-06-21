package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("behandlingsprosess.satsregulering")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AutomatiskGrunnbelopReguleringTask extends FagsakProsessTask {

    public static final String MANUELL_KEY = "manuell";

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskGrunnbelopReguleringTask.class);
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlendeEnhetTjeneste enhetTjeneste;
    private BehandlingFlytkontroll flytkontroll;

    AutomatiskGrunnbelopReguleringTask() {
        // for CDI proxy
    }

    @Inject
    public AutomatiskGrunnbelopReguleringTask(BehandlingRepositoryProvider repositoryProvider,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                              BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
            BehandlendeEnhetTjeneste enhetTjeneste,
            BehandlingFlytkontroll flytkontroll) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
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
            var satsFom = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
            if (skjæringstidspunkt.getFørsteUttaksdatoGrunnbeløp().isBefore(satsFom)) {
                LOG.info("GrunnbeløpRegulering stp før ny satsdato saksnummer = {} stp {}", fagsak.getSaksnummer().getVerdi(), skjæringstidspunkt);
            } else {
                LOG.info("GrunnbeløpRegulering behov for regulering saksnummer = {} stp {}", fagsak.getSaksnummer().getVerdi(), skjæringstidspunkt);
            }
        }
        return;
/*
        var skalKøes = flytkontroll.nyRevurderingSkalVente(fagsak);
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.RE_SATS_REGULERING, enhet);

        LOG.info("GrunnbeløpRegulering har opprettet revurdering på fagsak med fagsakId = {}", fagsakId);

        if (skalKøes) {
            flytkontroll.settNyRevurderingPåVent(revurdering);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }
        */

    }
}
