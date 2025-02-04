package no.nav.foreldrepenger.domene.uttak.saldo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AnnenPartGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenpartUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregningGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;


@ApplicationScoped
public class StønadskontoSaldoTjeneste {

    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;
    private FpUttakRepository fpUttakRepository;

    StønadskontoSaldoTjeneste() {
        //For CDI
    }

    @Inject
    public StønadskontoSaldoTjeneste(UttakRepositoryProvider repositoryProvider, UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste) {
        this.utregnetStønadskontoTjeneste = utregnetStønadskontoTjeneste;
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
    }

    public SaldoUtregning finnSaldoUtregning(UttakInput uttakInput) {
        var perioderSøker = perioderSøker(uttakInput.getBehandlingReferanse().behandlingId());
        return finnSaldoUtregning(uttakInput, mapTilRegelPerioder(perioderSøker));
    }

    public SaldoUtregning finnSaldoUtregning(UttakInput uttakInput, List<FastsattUttakPeriode> perioderSøker) {
        var ref = uttakInput.getBehandlingReferanse();
        var stønadskontoer = stønadskontoer(ref);
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var saldoUtregningGrunnlag = saldoUtregningGrunnlag(perioderSøker, uttakInput, fpGrunnlag.isBerørtBehandling(), stønadskontoer);
        return SaldoUtregningTjeneste.lagUtregning(saldoUtregningGrunnlag);
    }

    private SaldoUtregningGrunnlag saldoUtregningGrunnlag(List<FastsattUttakPeriode> perioderSøker,
                                                          UttakInput uttakInput,
                                                          boolean berørtBehandling,
                                                          Map<StønadskontoType, Integer> stønadskontoer) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var søknadOpprettetTidspunkt = uttakInput.getSøknadOpprettetTidspunkt();
        var sisteSøknadOpprettetTidspunktAnnenpart = fpGrunnlag.getAnnenpart()
            .map(Annenpart::søknadOpprettetTidspunkt)
            .orElse(null);
        if (!stønadskontoer.isEmpty() && !perioderSøker.isEmpty()) {
            var perioderAnnenpart = perioderAnnenpart(fpGrunnlag);
            var kontoer  = KontoerGrunnlagBygger.byggGrunnlag(uttakInput, stønadskontoer).build();
            return SaldoUtregningGrunnlag.forUtregningAvHeleUttaket(perioderSøker,
                berørtBehandling, perioderAnnenpart, kontoer, søknadOpprettetTidspunkt,
                sisteSøknadOpprettetTidspunktAnnenpart, UtsettelseCore2021.kreverSammenhengendeUttakTilOgMed());
        }
        return SaldoUtregningGrunnlag.forUtregningAvHeleUttaket(List.of(), berørtBehandling,
            List.of(), new Kontoer.Builder().build(), søknadOpprettetTidspunkt, sisteSøknadOpprettetTidspunktAnnenpart,
            UtsettelseCore2021.kreverSammenhengendeUttakTilOgMed());
    }

    public boolean erNegativSaldoPåNoenKonto(UttakInput uttakInput) {
        var saldoUtregning = finnSaldoUtregning(uttakInput);
        return saldoUtregning.negativSaldoPåNoenKonto();
    }

    public boolean erOriginalNegativSaldoPåNoenKontoForsiktig(UttakInput uttakInput) {
        var perioderSøker = mapTilRegelPerioder(perioderSøker(uttakInput.getBehandlingReferanse().getOriginalBehandlingId().orElseThrow()));
        var ref = uttakInput.getBehandlingReferanse();
        var stønadskontoer = stønadskontoer(ref);
        var saldoUtregningGrunnlag = saldoUtregningGrunnlag(perioderSøker, uttakInput, false, stønadskontoer);
        var saldoUtregning = SaldoUtregningTjeneste.lagUtregning(saldoUtregningGrunnlag);
        return saldoUtregning.negativSaldoPåNoenKontoByttParterKonservativ();
    }

    private List<FastsattUttakPeriode> mapTilRegelPerioder(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream().map(StønadskontoSaldoTjeneste::map).toList();
    }

    private List<AnnenpartUttakPeriode> perioderAnnenpart(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var opt = annenPartUttak(foreldrepengerGrunnlag);
        return opt.map(uttakResultatEntitet -> uttakResultatEntitet.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .map(AnnenPartGrunnlagBygger::map)
            .toList())
            .orElseGet(List::of);
    }

    private Optional<UttakResultatEntitet> annenPartUttak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getAnnenpart()
            .map(Annenpart::gjeldendeVedtakBehandlingId)
            .flatMap(fpUttakRepository::hentUttakResultatHvisEksisterer);
    }

    private List<UttakResultatPeriodeEntitet> perioderSøker(Long behandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder).orElse(List.of());
    }

    private Map<StønadskontoType, Integer> stønadskontoer(BehandlingReferanse ref) {
        return utregnetStønadskontoTjeneste.gjeldendeKontoutregning(ref);
    }

    public int finnStønadRest(SaldoUtregning saldoUtregning) {
        return Stream.of(Stønadskontotype.MØDREKVOTE, Stønadskontotype.FEDREKVOTE, Stønadskontotype.FORELDREPENGER,
            Stønadskontotype.FELLESPERIODE).mapToInt(saldoUtregning::saldo).sum();
    }

    private static FastsattUttakPeriode map(UttakResultatPeriodeEntitet periode) {
        return new FastsattUttakPeriode.Builder()
            .tidsperiode(periode.getFom(), periode.getTom())
            .aktiviteter(mapTilRegelPeriodeAktiviteter(periode.getAktiviteter()))
            .oppholdÅrsak(UttakEnumMapper.map(periode.getOppholdÅrsak()))
            .samtidigUttak(periode.isSamtidigUttak())
            .flerbarnsdager(periode.isFlerbarnsdager())
            .resultatÅrsak(UttakEnumMapper.mapTilFastsattPeriodeÅrsak(periode.getResultatÅrsak()))
            .utsettelse(periode.isUtsettelse())
            .periodeResultatType(UttakEnumMapper.map(periode.getResultatType()))
            .mottattDato(periode.getPeriodeSøknad().map(UttakResultatPeriodeSøknadEntitet::getMottattDato).orElse(null))
            .build();
    }

    private static List<FastsattUttakPeriodeAktivitet> mapTilRegelPeriodeAktiviteter(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        return aktiviteter.stream().map(StønadskontoSaldoTjeneste::map).toList();
    }

    private static FastsattUttakPeriodeAktivitet map(
        UttakResultatPeriodeAktivitetEntitet aktivitet) {
        var trekkdager = UttakEnumMapper.map(aktivitet.getTrekkdager());
        var stønadskontotype = UttakEnumMapper.map(aktivitet.getTrekkonto());
        var aktivitetIdentifikator = UttakEnumMapper.map(aktivitet.getUttakAktivitet());
        return new FastsattUttakPeriodeAktivitet(
            trekkdager, stønadskontotype, aktivitetIdentifikator);
    }
}
