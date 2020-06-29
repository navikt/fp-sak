package no.nav.foreldrepenger.domene.uttak.saldo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AnnenPartGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenpartUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Konto;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregningGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregningTjeneste;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;

@ApplicationScoped
public class StønadskontoSaldoTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FpUttakRepository fpUttakRepository;

    StønadskontoSaldoTjeneste() {
        //For CDI
    }

    @Inject
    public StønadskontoSaldoTjeneste(UttakRepositoryProvider repositoryProvider) {
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
    }

    public SaldoUtregning finnSaldoUtregning(UttakInput uttakInput) {
        var perioderSøker = perioderSøker(uttakInput.getBehandlingReferanse().getBehandlingId());
        return finnSaldoUtregning(uttakInput, mapTilRegelPerioder(perioderSøker));
    }

    public SaldoUtregning finnSaldoUtregning(UttakInput uttakInput, List<FastsattUttakPeriode> perioderSøker) {
        var ref = uttakInput.getBehandlingReferanse();
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var stønadskontoer = stønadskontoer(ref);
        var saldoUtregningGrunnlag = saldoUtregningGrunnlag(perioderSøker, foreldrepengerGrunnlag, stønadskontoer);
        return SaldoUtregningTjeneste.lagUtregning(saldoUtregningGrunnlag);
    }

    private SaldoUtregningGrunnlag saldoUtregningGrunnlag(List<FastsattUttakPeriode> perioderSøker,
                                                          ForeldrepengerGrunnlag foreldrepengerGrunnlag,
                                                          Optional<Set<Stønadskonto>> stønadskontoer) {
        if (stønadskontoer.isPresent() && perioderSøker.size() > 0) {
            var perioderAnnenpart = perioderAnnenpart(foreldrepengerGrunnlag);
            var kontoer = lagKontoer(stønadskontoer.get());
            return SaldoUtregningGrunnlag.forUtregningAvHeleUttaket(perioderSøker, foreldrepengerGrunnlag.isTapendeBehandling(), perioderAnnenpart, kontoer);
        }
        return SaldoUtregningGrunnlag.forUtregningAvHeleUttaket(List.of(), foreldrepengerGrunnlag.isTapendeBehandling(), List.of(), new Kontoer.Builder().build());
    }

    private Kontoer lagKontoer(Set<Stønadskonto> stønadskontoer) {
        var kontoList = stønadskontoer.stream().map(stønadskonto -> new Konto.Builder()
            .medType(UttakEnumMapper.map(stønadskonto.getStønadskontoType()))
            .medTrekkdager(stønadskonto.getMaxDager()))
            .collect(Collectors.toList());
        return new Kontoer.Builder().medKontoList(kontoList).build();
    }

    public boolean erNegativSaldoPåNoenKonto(UttakInput uttakInput) {
        var saldoUtregning = finnSaldoUtregning(uttakInput);
        return saldoUtregning.negativSaldoPåNoenKonto();
    }

    private List<FastsattUttakPeriode> mapTilRegelPerioder(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream().map(StønadskontoSaldoTjeneste::map).collect(Collectors.toList());
    }

    private List<AnnenpartUttakPeriode> perioderAnnenpart(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        Optional<UttakResultatEntitet> opt = annenPartUttak(foreldrepengerGrunnlag);
        if (opt.isPresent()) {
            return opt.get().getGjeldendePerioder().getPerioder().stream()
                .map(AnnenPartGrunnlagBygger::map)
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private Optional<UttakResultatEntitet> annenPartUttak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var annenpart = foreldrepengerGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return fpUttakRepository.hentUttakResultatHvisEksisterer(annenpart.get().getGjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private List<UttakResultatPeriodeEntitet> perioderSøker(Long behandlingId) {
        Optional<UttakResultatEntitet> opt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        if (opt.isPresent()) {
            return opt.get().getGjeldendePerioder().getPerioder();
        }
        return List.of();
    }

    private Optional<Set<Stønadskonto>> stønadskontoer(BehandlingReferanse ref) {
        Optional<FagsakRelasjon> fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(ref.getSaksnummer());
        if (fagsakRelasjon.isPresent() && fagsakRelasjon.get().getGjeldendeStønadskontoberegning().isPresent()) {
            var stønadskontoer = fagsakRelasjon.get().getGjeldendeStønadskontoberegning().get().getStønadskontoer();
            return Optional.ofNullable(stønadskontoer);
        }

        return Optional.empty();
    }

    public boolean erSluttPåStønadsdager(UttakInput uttakInput) {
        return finnStønadRest(uttakInput) == 0;
    }

    public int finnStønadRest(UttakInput uttakInput) {
        var saldoUtregning = finnSaldoUtregning(uttakInput);
        return Stream.of(Stønadskontotype.MØDREKVOTE, Stønadskontotype.FEDREKVOTE, Stønadskontotype.FORELDREPENGER, Stønadskontotype.FELLESPERIODE)
            .mapToInt(saldoUtregning::saldo).sum();
    }

    private static FastsattUttakPeriode map(UttakResultatPeriodeEntitet periode) {
        return new FastsattUttakPeriode.Builder()
            .medTidsperiode(periode.getFom(), periode.getTom())
            .medAktiviteter(mapTilRegelPeriodeAktiviteter(periode.getAktiviteter()))
            .medOppholdÅrsak(UttakEnumMapper.map(periode.getOppholdÅrsak()))
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .medPeriodeResultatType(UttakEnumMapper.map(periode.getResultatType()))
            .build();
    }

    private static List<FastsattUttakPeriodeAktivitet> mapTilRegelPeriodeAktiviteter(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        return aktiviteter.stream().map(StønadskontoSaldoTjeneste::map).collect(Collectors.toList());
    }

    private static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitetEntitet aktivitet) {
        var trekkdager = UttakEnumMapper.map(aktivitet.getTrekkdager());
        var stønadskontotype = UttakEnumMapper.map(aktivitet.getTrekkonto());
        var aktivitetIdentifikator = UttakEnumMapper.map(aktivitet.getUttakAktivitet());
        return new no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet(trekkdager, stønadskontotype, aktivitetIdentifikator);
    }
}
