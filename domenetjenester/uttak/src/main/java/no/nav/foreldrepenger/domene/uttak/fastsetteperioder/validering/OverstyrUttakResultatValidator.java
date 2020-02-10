package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPerioder;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.FastsattUttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;

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

    public void valider(UttakInput uttakInput, UttakResultatPerioder opprinnelig, UttakResultatPerioder perioder) {
        new PerioderHarFastsattResultatValidering().utfør(perioder);
        new BareSplittetPerioderValidering(opprinnelig).utfør(perioder);
        new EndringerHarBegrunnelseValidering(opprinnelig).utfør(perioder);
        new HarSattUtbetalingsprosentValidering(opprinnelig).utfør(perioder);
        new EndringerBareEtterEndringsdatoValidering().utfør(perioder);
        validerSaldo(perioder, uttakInput);
    }

    private void validerSaldo(UttakResultatPerioder perioder, UttakInput uttakInput) {
        SaldoUtregning saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput, map(perioder.getPerioder()));
        ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        new SaldoValidering(saldoUtregning, harAnnenpart(fpGrunnlag), fpGrunnlag.isTapendeBehandling()).utfør(perioder);
    }

    private boolean harAnnenpart(ForeldrepengerGrunnlag fpGrunnlag) {
        var annenpart = fpGrunnlag.getAnnenpart();
        return annenpart.isPresent();
    }

    private List<FastsattUttakPeriode> map(List<no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPeriode> perioder) {
        return perioder.stream().map(this::map).collect(Collectors.toList());
    }

    private FastsattUttakPeriode map(no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPeriode periode) {
        return new FastsattUttakPeriode.Builder()
            .medOppholdÅrsak(UttakEnumMapper.map(periode.getOppholdÅrsak()))
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medTidsperiode(periode.getTidsperiode().getFomDato(), periode.getTidsperiode().getTomDato())
            .medPeriodeResultatType(UttakEnumMapper.map(periode.getResultatType()))
            .medAktiviteter(mapAktiviteter(periode.getAktiviteter()))
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .build();
    }

    private List<FastsattUttakPeriodeAktivitet> mapAktiviteter(List<UttakResultatPeriodeAktivitet> aktiviteter) {
        return aktiviteter.stream().map(this::map).collect(Collectors.toList());
    }

    private FastsattUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitet aktivitet) {
        return new FastsattUttakPeriodeAktivitet(UttakEnumMapper.map(aktivitet.getTrekkdager()), UttakEnumMapper.map(aktivitet.getTrekkonto()),
            UttakEnumMapper.map(aktivitet.getUttakArbeidType(), aktivitet.getArbeidsgiver(), aktivitet.getArbeidsforholdId()));
    }
}
