package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.TidligstMottattOppdaterer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@ProsessTask(value = "migrering.tilbakeforbehandling", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterTidligstMottattForUttakTask extends BehandlingProsessTask {

    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FpUttakRepository fpUttakRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;

    public OppdaterTidligstMottattForUttakTask() {
        // For CDI
    }

    @Inject
    public OppdaterTidligstMottattForUttakTask(BehandlingRepositoryProvider repositoryProvider,
                                               BehandlingProsesseringTjeneste prosesseringTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.prosesseringTjeneste = prosesseringTjeneste;
    }



    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        prosesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, BehandlingStegType.INNGANG_UTTAK);
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        var eksisterendeAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Forventer at det finnes en oppgitt fordeling for behandling " + behandlingId));
        var eksisterendeOppgittFordeling = eksisterendeAggregat.getOppgittFordeling();
        var oppdatertOppgittePerioder = oppdaterTidligstMottattDato(behandling, behandling.getOpprettetDato().toLocalDate(), eksisterendeOppgittFordeling.getPerioder());
        var oppdatertOppgittFordeling = new OppgittFordelingEntitet(oppdatertOppgittePerioder, eksisterendeOppgittFordeling.getErAnnenForelderInformert(), eksisterendeOppgittFordeling.ønskerJustertVedFødsel());
        var builder = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(eksisterendeAggregat))
            .medOppgittFordeling(oppdatertOppgittFordeling)
            .medJustertFordeling(null)
            .medOverstyrtFordeling(null);
        ytelsesFordelingRepository.lagre(behandling.getId(), builder.build());

        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
    }

    public List<OppgittPeriodeEntitet> oppdaterTidligstMottattDato(Behandling behandling, LocalDate mottattDato, List<OppgittPeriodeEntitet> nysøknad) {
        if (nysøknad.isEmpty()) {
            return nysøknad;
        }

        var tidligereFordelinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
            .filter(Behandling::erYtelseBehandling)
            .map(Behandling::getId)
            .filter(b -> !b.equals(behandling.getId()))
            .map(this::fordelingForBehandling)
            .flatMap(Optional::stream)
            .toList();

        // Vedtaksperioder fra forrige uttaksresultat - bruker sammenhengende = true for å få med avslåtte
        var forrigeUttak = behandling.getOriginalBehandlingId()
            .flatMap(fpUttakRepository::hentUttakResultatHvisEksisterer);

        return TidligstMottattOppdaterer.oppdaterTidligstMottattDato(nysøknad, mottattDato, tidligereFordelinger, forrigeUttak);
    }

    private Optional<OppgittFordelingEntitet> fordelingForBehandling(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getGjeldendeFordeling);
    }

}
