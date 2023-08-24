package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

import java.util.Objects;
import java.util.Optional;


@ApplicationScoped
public class BeregnStønadskontoerTjeneste {

    private StønadskontoRegelAdapter stønadskontoRegelAdapter;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;


    @Inject
    public BeregnStønadskontoerTjeneste(UttakRepositoryProvider repositoryProvider,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                        ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.stønadskontoRegelAdapter = new StønadskontoRegelAdapter();
        this.uttakTjeneste = uttakTjeneste;
    }

    BeregnStønadskontoerTjeneste() {
        //For CDI
    }

    public void opprettStønadskontoer(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
        var stønadskontoberegning = beregn(uttakInput, fagsakRelasjon);
        fagsakRelasjonTjeneste.lagre(ref.fagsakId(), fagsakRelasjon, ref.behandlingId(), stønadskontoberegning);
    }

    public void overstyrStønadskontoberegning(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
        var eksisterende = fagsakRelasjon.getGjeldendeStønadskontoberegning().orElseThrow();
        var ny = beregn(uttakInput, fagsakRelasjon);
        if (inneholderEndringer(eksisterende, ny)) {
            fagsakRelasjonTjeneste.overstyrStønadskontoberegning(ref.fagsakId(), ref.behandlingId(), ny);
            oppdaterBehandlingsresultat(ref.behandlingId());
        }
    }

    public Stønadskontoberegning beregn(UttakInput uttakInput, FagsakRelasjon fagsakRelasjon) {
        var ref = uttakInput.getBehandlingReferanse();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.behandlingId());
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(fpGrunnlag);
        return stønadskontoRegelAdapter.beregnKontoer(ref, ytelseFordelingAggregat, fagsakRelasjon, annenpartsGjeldendeUttaksplan, fpGrunnlag);
    }

    public boolean inneholderEndringer(Stønadskontoberegning eksisterende, Stønadskontoberegning ny) {
        for (var eksisterendeStønadskonto : eksisterende.getStønadskontoer()) {
            var likNyStønadskonto = finnKontoIStønadskontoberegning(ny, eksisterendeStønadskonto);
            if (likNyStønadskonto.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Optional<Stønadskonto> finnKontoIStønadskontoberegning(Stønadskontoberegning stønadskontoberegning, Stønadskonto konto) {
        return stønadskontoberegning.getStønadskontoer().stream()
            .filter(stønadskonto -> stønadskonto.getStønadskontoType().equals(konto.getStønadskontoType()))
            .filter(stønadskonto -> Objects.equals(stønadskonto.getMaxDager(), konto.getMaxDager()))
            .findFirst();
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
}
