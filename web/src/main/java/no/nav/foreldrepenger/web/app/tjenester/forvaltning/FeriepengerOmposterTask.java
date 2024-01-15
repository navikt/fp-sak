package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask("feriepenger.omposter24")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class FeriepengerOmposterTask extends FagsakProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(FeriepengerOmposterTask.class);

    private final BehandlingRepository behandlingRepository;
    private final FagsakRepository fagsakRepository;
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private final BeregningsresultatRepository beregningsresultatRepository;
    private final BehandlendeEnhetTjeneste enhetTjeneste;
    private final BehandlingFlytkontroll flytkontroll;

    @Inject
    public FeriepengerOmposterTask(BehandlingRepositoryProvider repositoryProvider,
                                   BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                   BeregningsresultatRepository beregningsresultatRepository,
                                   BehandlendeEnhetTjeneste enhetTjeneste,
                                   BehandlingFlytkontroll flytkontroll) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
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
            LOG.info("FeriepengeOmpostering finnes allerede åpen revurdering på saksnummer = {}", fagsak.getSaksnummer().getVerdi());
            return;
        }
        var sisteVedtatte = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).orElseThrow();
        var utbetalesFom2024 = beregningsresultatRepository.hentUtbetBeregningsresultat(sisteVedtatte.getId())
            .flatMap(BeregningsresultatEntitet::getBeregningsresultatFeriepenger)
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe).orElse(List.of()).stream()
            .anyMatch(f -> f.getOpptjeningsåret() > 2022 && f.getBeregningsresultatAndel().erBrukerMottaker() && !f.getÅrsbeløp().erNullEllerNulltall());
        if (!utbetalesFom2024) {
            LOG.info("FeriepengeOmpostering ferdig utbetalt før 2024 på saksnummer = {}", fagsak.getSaksnummer().getVerdi());
            return;
        }

        var skalKøes = flytkontroll.nyRevurderingSkalVente(fagsak);
        var enhet = enhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.REBEREGN_FERIEPENGER, enhet);

        LOG.info("FeriepengeOmpostering har opprettet revurdering på saksnummer = {}", fagsak.getSaksnummer().getVerdi());

        if (skalKøes) {
            flytkontroll.settNyRevurderingPåVent(revurdering);
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }


    }
}
