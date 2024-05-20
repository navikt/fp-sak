package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
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
        if (!eksisterendeBeregning.getStønadskontoutregning().equals(nyBeregning.getStønadskontoutregning())) {
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

    public Optional<Stønadskontoberegning> beregnForBehandling(UttakInput uttakInput, Map<StønadskontoType, Integer> tidligereBeregning) {
        return beregnForBehandling(uttakInput, dekningsgradTjeneste.finnGjeldendeDekningsgrad(uttakInput.getBehandlingReferanse()), tidligereBeregning);
    }

    public Optional<Stønadskontoberegning> beregnForBehandling(UttakInput uttakInput, Dekningsgrad dekningsgrad, Map<StønadskontoType, Integer> tidligereBeregning) {
        var ref = uttakInput.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(fpGrunnlag);
        return stønadskontoRegelAdapter.beregnKontoerSjekkDiff(ref, ytelseFordelingAggregat, dekningsgrad, annenpartsGjeldendeUttaksplan, fpGrunnlag, tidligereBeregning);
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
