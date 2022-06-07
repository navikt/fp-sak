package no.nav.foreldrepenger.domene.uttak.saldo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.AnnenPartGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.BehandlingGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.DatoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.SøknadGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FarUttakRundtFødsel;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AnnenpartUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Kontoer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregningGrunnlag;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregningTjeneste;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.LukketPeriode;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;


@ApplicationScoped
public class StønadskontoSaldoTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FpUttakRepository fpUttakRepository;
    private KontoerGrunnlagBygger kontoerGrunnlagBygger;

    StønadskontoSaldoTjeneste() {
        //For CDI
    }

    @Inject
    public StønadskontoSaldoTjeneste(UttakRepositoryProvider repositoryProvider, KontoerGrunnlagBygger kontoerGrunnlagBygger) {
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.kontoerGrunnlagBygger = kontoerGrunnlagBygger;
    }

    public SaldoUtregning finnSaldoUtregning(UttakInput uttakInput) {
        var perioderSøker = perioderSøker(uttakInput.getBehandlingReferanse().behandlingId());
        return finnSaldoUtregning(uttakInput, mapTilRegelPerioder(perioderSøker));
    }

    public SaldoUtregning finnSaldoUtregning(UttakInput uttakInput, List<FastsattUttakPeriode> perioderSøker) {
        var ref = uttakInput.getBehandlingReferanse();
        var stønadskontoer = stønadskontoer(ref);
        var saldoUtregningGrunnlag = saldoUtregningGrunnlag(perioderSøker, uttakInput, stønadskontoer);
        return SaldoUtregningTjeneste.lagUtregning(saldoUtregningGrunnlag);
    }

    private SaldoUtregningGrunnlag saldoUtregningGrunnlag(List<FastsattUttakPeriode> perioderSøker,
                                                          UttakInput uttakInput,
                                                          Optional<Set<Stønadskonto>> stønadskontoer) {
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        var søknadOpprettetTidspunkt = uttakInput.getSøknadOpprettetTidspunkt();
        var sisteSøknadOpprettetTidspunktAnnenpart = fpGrunnlag.getAnnenpart()
            .map(ap -> ap.søknadOpprettetTidspunkt())
            .orElse(null);
        if (stønadskontoer.isPresent() && perioderSøker.size() > 0) {
            var perioderAnnenpart = perioderAnnenpart(fpGrunnlag);
            var kontoer  = kontoerGrunnlagBygger.byggGrunnlag(uttakInput).build();
            Optional<LukketPeriode> periodeFar = Optional.empty();
            // TODO: trengs dette eller kan vi sende inn en tom periode? SaldoValidering?
            if (!uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().utenMinsterett()
                && !BehandlingGrunnlagBygger.søkerErMor(uttakInput.getBehandlingReferanse())) {
                var type = SøknadGrunnlagBygger.type(fpGrunnlag);
                var datoer = DatoerGrunnlagBygger.byggForenkletGrunnlagKunFamiliehendelse(uttakInput).build();
                periodeFar = FarUttakRundtFødsel.utledFarsPeriodeRundtFødsel(datoer, kontoer, type, StandardKonfigurasjon.KONFIGURASJON);
            }
            return SaldoUtregningGrunnlag.forUtregningAvHeleUttaket(perioderSøker,
                fpGrunnlag.isBerørtBehandling(), perioderAnnenpart, kontoer, søknadOpprettetTidspunkt,
                sisteSøknadOpprettetTidspunktAnnenpart, periodeFar);
        }
        return SaldoUtregningGrunnlag.forUtregningAvHeleUttaket(List.of(), fpGrunnlag.isBerørtBehandling(),
            List.of(), new Kontoer.Builder().build(), søknadOpprettetTidspunkt, sisteSøknadOpprettetTidspunktAnnenpart, Optional.empty());
    }

    public boolean erNegativSaldoPåNoenKonto(UttakInput uttakInput) {
        var saldoUtregning = finnSaldoUtregning(uttakInput);
        return saldoUtregning.negativSaldoPåNoenKonto();
    }

    private List<FastsattUttakPeriode> mapTilRegelPerioder(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream().map(StønadskontoSaldoTjeneste::map).collect(Collectors.toList());
    }

    private List<AnnenpartUttakPeriode> perioderAnnenpart(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var opt = annenPartUttak(foreldrepengerGrunnlag);
        return opt.map(uttakResultatEntitet -> uttakResultatEntitet.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .map(AnnenPartGrunnlagBygger::map)
            .collect(Collectors.toList()))
            .orElseGet(List::of);
    }

    private Optional<UttakResultatEntitet> annenPartUttak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var annenpart = foreldrepengerGrunnlag.getAnnenpart();
        if (annenpart.isPresent()) {
            return fpUttakRepository.hentUttakResultatHvisEksisterer(annenpart.get().gjeldendeVedtakBehandlingId());
        }
        return Optional.empty();
    }

    private List<UttakResultatPeriodeEntitet> perioderSøker(Long behandlingId) {
        var opt = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        if (opt.isPresent()) {
            return opt.get().getGjeldendePerioder().getPerioder();
        }
        return List.of();
    }

    private Optional<Set<Stønadskonto>> stønadskontoer(BehandlingReferanse ref) {
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(ref.saksnummer());
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
            .mottattDato(periode.getPeriodeSøknad().map(ps -> ps.getMottattDato()).orElse(null))
            .build();
    }

    private static List<FastsattUttakPeriodeAktivitet> mapTilRegelPeriodeAktiviteter(List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        return aktiviteter.stream().map(StønadskontoSaldoTjeneste::map).collect(Collectors.toList());
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
