package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.håndterOverlappPleiepenger", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterOverlappPleiepengerTask extends GenerellProsessTask {

    private HåndterOpphørAvYtelser tjeneste;
    private AbakusTjeneste abakusTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;


    @Inject
    public HåndterOverlappPleiepengerTask(HåndterOpphørAvYtelser tjeneste,
                                          AbakusTjeneste abakusTjeneste,
                                          BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.tjeneste = tjeneste;
        this.abakusTjeneste = abakusTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    HåndterOverlappPleiepengerTask() {
        // for CDI proxy
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        // Unngå doble revurderinger ved tette vedtak fra Pleiepenger
        if (erFortsattOverlapp(fagsak)) {
            tjeneste.oppdaterEllerOpprettRevurdering(fagsak, null, BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER, true);
        }
    }

    private boolean erFortsattOverlapp(Fagsak fagsak) {
        var tilkjentSegments = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(b -> tilkjentYtelseRepository.hentUtbetBeregningsresultat(b.getId()))
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(), Boolean.TRUE))
            .toList();
        if (tilkjentSegments.isEmpty()) {
            return false;
        }
        var tidligsteTilkjent = tilkjentSegments.stream().map(LocalDateSegment::getFom).min(Comparator.naturalOrder()).orElseThrow();
        var tilkjentTidslinje = new LocalDateTimeline<>(tilkjentSegments, StandardCombinators::alwaysTrueForMatch).compress();

        var request = AbakusTjeneste.lagRequestForHentVedtakFom(fagsak.getAktørId(), tidligsteTilkjent,
            Set.of(Ytelser.PLEIEPENGER_SYKT_BARN, Ytelser.PLEIEPENGER_NÆRSTÅENDE));
        return abakusTjeneste.hentVedtakForAktørId(request).stream()
            .map(y -> (YtelseV1)y)
            .filter(y -> Kildesystem.K9SAK.equals(y.getKildesystem()))
            .filter(y -> Ytelser.PLEIEPENGER_SYKT_BARN.equals(y.getYtelse()) || Ytelser.PLEIEPENGER_NÆRSTÅENDE.equals(y.getYtelse()))
            .flatMap(y -> y.getAnvist().stream())
            .map(Anvisning::getPeriode)
            .anyMatch(p -> !tilkjentTidslinje.intersection(new LocalDateInterval(p.getFom(), p.getTom())).isEmpty());
    }
}
