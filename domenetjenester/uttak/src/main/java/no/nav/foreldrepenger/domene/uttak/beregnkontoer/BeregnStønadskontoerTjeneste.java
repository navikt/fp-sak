package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;


@ApplicationScoped
public class BeregnStønadskontoerTjeneste {

    private StønadskontoRegelAdapter stønadskontoRegelAdapter;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;


    @Inject
    public BeregnStønadskontoerTjeneste(UttakRepositoryProvider repositoryProvider,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                        ForeldrepengerUttakTjeneste uttakTjeneste,
                                        DekningsgradTjeneste dekningsgradTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.stønadskontoRegelAdapter = new StønadskontoRegelAdapter();
        this.uttakTjeneste = uttakTjeneste;
    }

    BeregnStønadskontoerTjeneste() {
        //For CDI
    }

    public void opprettStønadskontoer(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var stønadskontoberegning = beregn(uttakInput, false);
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
        fagsakRelasjonTjeneste.lagre(ref.fagsakId(), fagsakRelasjon, ref.behandlingId(), stønadskontoberegning);
    }

    public void overstyrStønadskontoberegning(UttakInput uttakInput, boolean relativBeregning) {
        var ref = uttakInput.getBehandlingReferanse();
        var eksisterendeBeregning = getGjeldendeBeregning(ref);
        var nyBeregning = beregn(uttakInput, relativBeregning);
        if (inneholderEndringer(eksisterendeBeregning, nyBeregning)) {
            fagsakRelasjonTjeneste.overstyrStønadskontoberegning(ref.fagsakId(), ref.behandlingId(), nyBeregning);
            oppdaterBehandlingsresultat(ref.behandlingId());
        }
    }

    public Stønadskontoberegning beregn(UttakInput uttakInput, boolean relativBeregning) {
        var ref = uttakInput.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(fpGrunnlag);
        var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);
        Map<StønadskontoType, Integer> tidligereBeregning = relativBeregning ? getGjeldendeBeregning(ref).getStønadskontoutregning() : Map.of();
        return stønadskontoRegelAdapter.beregnKontoer(ref, ytelseFordelingAggregat, dekningsgrad, annenpartsGjeldendeUttaksplan, fpGrunnlag, tidligereBeregning);
    }

    public Optional<Stønadskontoberegning> beregnForBehandling(UttakInput uttakInput) {
        // Må hente på nytt siden FR kan ha blitt endret etter input/FpGrunnlag (evt generere ny input i steget)
        var kontoUtregning = fagsakRelasjonTjeneste.finnRelasjonFor(uttakInput.getBehandlingReferanse().saksnummer())
            .getGjeldendeStønadskontoberegning().orElseThrow()
            .getStønadskontoutregning();
        return beregnForBehandling(uttakInput, kontoUtregning);
    }

    public Optional<Stønadskontoberegning> beregnForBehandling(UttakInput uttakInput, Map<StønadskontoType, Integer> tidligereBeregning) {
        var ref = uttakInput.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(fpGrunnlag);
        var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);
        return stønadskontoRegelAdapter.beregnKontoerSjekkDiff(ref, ytelseFordelingAggregat, dekningsgrad, annenpartsGjeldendeUttaksplan, fpGrunnlag, tidligereBeregning);
    }

    public boolean inneholderEndringer(Stønadskontoberegning eksisterende, Stønadskontoberegning ny) {
        var typerEksisterende = eksisterende.getStønadskontoer().stream().map(Stønadskonto::getStønadskontoType).collect(Collectors.toSet());
        var typerNy = ny.getStønadskontoer().stream().map(Stønadskonto::getStønadskontoType).collect(Collectors.toSet());
        if (typerNy.size() == typerEksisterende.size() && typerNy.containsAll(typerEksisterende)) {
            return eksisterende.getStønadskontoer().stream()
                .anyMatch(e -> !harSammeAntallDagerFor(ny, e));
        } else {
            return true;
        }
    }

    private boolean harSammeAntallDagerFor(Stønadskontoberegning stønadskontoberegning, Stønadskonto konto) {
        return stønadskontoberegning.getStønadskontoer().stream()
            .filter(stønadskonto -> stønadskonto.getStønadskontoType().equals(konto.getStønadskontoType()))
            .anyMatch(stønadskonto -> Objects.equals(stønadskonto.getMaxDager(), konto.getMaxDager()));
    }

    private void oppdaterBehandlingsresultat(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var oppdaterBehandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medEndretStønadskonto(true).build();
        behandlingsresultatRepository.lagre(behandlingId, oppdaterBehandlingsresultat);
    }

    private Optional<ForeldrepengerUttak> hentAnnenpartsUttak(ForeldrepengerGrunnlag fpGrunnlag) {
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return uttakTjeneste.hentUttakHvisEksisterer(annenpart.get().gjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private Stønadskontoberegning getGjeldendeBeregning(BehandlingReferanse ref) {
        return fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer()).getGjeldendeStønadskontoberegning().orElseThrow();
    }
}
