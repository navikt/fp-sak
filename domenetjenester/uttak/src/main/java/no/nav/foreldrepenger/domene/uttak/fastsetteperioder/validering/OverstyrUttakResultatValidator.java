package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;

@ApplicationScoped
public class OverstyrUttakResultatValidator {

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    OverstyrUttakResultatValidator() {
        // CDI
    }

    @Inject
    public OverstyrUttakResultatValidator(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
    }

    public void valider(UttakInput uttakInput,
                        List<ForeldrepengerUttakPeriode> opprinnelig,
                        List<ForeldrepengerUttakPeriode> perioder,
                        LocalDate endringsdato) {
        new PerioderHarFastsattResultatValidering().utfør(perioder);
        new BareSplittetPerioderValidering(opprinnelig).utfør(perioder);
        new EndringerHarBegrunnelseValidering(opprinnelig).utfør(perioder);
        new HarSattUtbetalingsgradValidering(opprinnelig).utfør(perioder);
        new AvslagØverføringValidering().utfør(perioder);
        if (uttakInput.getBehandlingReferanse().erRevurdering()) {
            new EndringerBareEtterEndringsdatoValidering(opprinnelig, endringsdato).utfør(perioder);
        }
        validerSaldo(perioder, uttakInput);
    }

    private void validerSaldo(List<ForeldrepengerUttakPeriode> perioder, UttakInput uttakInput) {
        var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput, map(perioder));
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        new SaldoValidering(saldoUtregning, harAnnenpart(fpGrunnlag), fpGrunnlag.isTapendeBehandling()).utfør(perioder);
    }

    private boolean harAnnenpart(ForeldrepengerGrunnlag fpGrunnlag) {
        var annenpart = fpGrunnlag.getAnnenpart();
        return annenpart.isPresent();
    }

    private List<FastsattUttakPeriode> map(List<ForeldrepengerUttakPeriode> perioder) {
        return perioder.stream().map(this::map).toList();
    }

    private FastsattUttakPeriode map(ForeldrepengerUttakPeriode periode) {
        return new FastsattUttakPeriode.Builder()
            .oppholdÅrsak(UttakEnumMapper.map(periode.getOppholdÅrsak()))
            .samtidigUttak(periode.isSamtidigUttak())
            .tidsperiode(periode.getTidsperiode().getFomDato(), periode.getTidsperiode().getTomDato())
            .periodeResultatType(UttakEnumMapper.map(periode.getResultatType()))
            .aktiviteter(mapAktiviteter(periode.getAktiviteter()))
            .flerbarnsdager(periode.isFlerbarnsdager())
            .mottattDato(periode.getMottattDato())
            .resultatÅrsak(UttakEnumMapper.mapTilFastsattPeriodeÅrsak(periode.getResultatÅrsak()))
            .utsettelse(!UttakUtsettelseType.UDEFINERT.equals(periode.getUtsettelseType()))
            .build();
    }

    private List<FastsattUttakPeriodeAktivitet> mapAktiviteter(List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter) {
        return aktiviteter.stream().map(this::map).toList();
    }

    private FastsattUttakPeriodeAktivitet map(ForeldrepengerUttakPeriodeAktivitet aktivitet) {
        return new FastsattUttakPeriodeAktivitet(UttakEnumMapper.map(aktivitet.getTrekkdager()),
            UttakEnumMapper.map(aktivitet.getTrekkonto()),
            UttakEnumMapper.map(aktivitet.getUttakAktivitet().getUttakArbeidType(),
                aktivitet.getUttakAktivitet().getArbeidsgiver(), aktivitet.getUttakAktivitet().getArbeidsforholdRef()));
    }
}
