package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;


@ApplicationScoped
public class BeregnStønadskontoerTjeneste {

    private StønadskontoRegelAdapter stønadskontoRegelAdapter;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UttakRepository uttakRepository;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;


    @Inject
    public BeregnStønadskontoerTjeneste(UttakRepositoryProvider repositoryProvider, FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.stønadskontoRegelAdapter = new StønadskontoRegelAdapter(repositoryProvider);
    }

    BeregnStønadskontoerTjeneste() {
        //For CDI
    }

    public Stønadskontoberegning beregnStønadskontoer(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var fagsak = finnFagsak(ref);
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(fagsak);
        var stønadskontoberegning = beregn(uttakInput, fagsakRelasjon);
        fagsakRelasjonTjeneste.lagre(fagsak,fagsakRelasjon, ref.getBehandlingId(), stønadskontoberegning);
        return stønadskontoberegning;
    }

    public void overstyrStønadskontoberegning(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.getSaksnummer());
        var eksisterende = fagsakRelasjon.getGjeldendeStønadskontoberegning().orElseThrow();
        var ny = beregn(uttakInput, fagsakRelasjon);
        if (inneholderEndringer(eksisterende, ny)) {
            var fagsak = finnFagsak(ref);
            fagsakRelasjonTjeneste.overstyrStønadskontoberegning(fagsak, ref.getBehandlingId(), ny);
            oppdaterBehandlingsresultat(ref.getBehandlingId());
        }
    }

    private Fagsak finnFagsak(BehandlingReferanse ref) {
        return fagsakRepository.hentSakGittSaksnummer(ref.getSaksnummer()).orElseThrow();
    }

    private boolean inneholderEndringer(Stønadskontoberegning eksisterende, Stønadskontoberegning ny) {
        for (Stønadskonto eksisterendeStønadskonto : eksisterende.getStønadskontoer()) {
            Optional<Stønadskonto> likNyStønadskonto = finnKontoIStønadskontoberegning(ny, eksisterendeStønadskonto);
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
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var oppdaterBehandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medEndretStønadskonto(true).build();
        behandlingsresultatRepository.lagre(behandlingId, oppdaterBehandlingsresultat);
    }

    private Stønadskontoberegning beregn(UttakInput uttakInput, FagsakRelasjon fagsakRelasjon) {
        var ref = uttakInput.getBehandlingReferanse();
        YtelseFordelingAggregat ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        Optional<UttakResultatEntitet> annenpartsGjeldendeUttaksplan = hentAnnenpartsUttak(fpGrunnlag);
        return stønadskontoRegelAdapter.beregnKontoer(ref, ytelseFordelingAggregat, fagsakRelasjon, annenpartsGjeldendeUttaksplan, fpGrunnlag);
    }

    private Optional<UttakResultatEntitet> hentAnnenpartsUttak(ForeldrepengerGrunnlag fpGrunnlag) {
        var annenpart = fpGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return uttakRepository.hentUttakResultatHvisEksisterer(fpGrunnlag.getAnnenpart().get().getGjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }
}
