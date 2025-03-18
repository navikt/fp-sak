package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.håndterOverlappPleiepenger", prioritet = 2, maxFailedRuns = 1)
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
            tjeneste.oppdaterEllerOpprettRevurdering(fagsak, null, BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER);
        }
    }

    private boolean erFortsattOverlapp(Fagsak fagsak) {
        var beregningsresultatEntitet = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(b -> tilkjentYtelseRepository.hentUtbetBeregningsresultat(b.getId()));

        var tidligsteTilkjent = beregningsresultatEntitet.map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(List.of())
            .stream()
            .filter(p -> p.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());

        if (tidligsteTilkjent.isEmpty()) {
            return false;
        }

        var ytelserPleiepenger = hentYtelser(fagsak.getAktørId(), tidligsteTilkjent.get());

        return SjekkOverlapp.erOverlappOgMerEnn100Prosent(beregningsresultatEntitet, ytelserPleiepenger);
    }

    private List<YtelseV1> hentYtelser(AktørId aktørId, LocalDate tidligsteTilkjent) {
        var request = AbakusTjeneste.lagRequestForHentVedtakFom(aktørId, tidligsteTilkjent,
            Set.of(Ytelser.PLEIEPENGER_SYKT_BARN, Ytelser.PLEIEPENGER_NÆRSTÅENDE));

        return abakusTjeneste.hentVedtakForAktørId(request)
            .stream()
            .map(y -> (YtelseV1) y)
            .filter(y -> Kildesystem.K9SAK.equals(y.getKildesystem()))
            .filter(y -> Ytelser.PLEIEPENGER_SYKT_BARN.equals(y.getYtelse()) || Ytelser.PLEIEPENGER_NÆRSTÅENDE.equals(y.getYtelse()))
            .toList();
    }
}
