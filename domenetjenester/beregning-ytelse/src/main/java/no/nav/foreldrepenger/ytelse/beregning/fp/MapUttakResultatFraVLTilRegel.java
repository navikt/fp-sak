package no.nav.foreldrepenger.ytelse.beregning.fp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.ytelse.beregning.adapter.ArbeidsforholdMapper;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapUttakArbeidTypeTilAktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultatPeriode;

@ApplicationScoped
public class MapUttakResultatFraVLTilRegel {


    @Inject
    public MapUttakResultatFraVLTilRegel() {
    }

    public UttakResultat mapFra(List<ForeldrepengerUttakPeriode> perioder, UttakInput input) {
        var uttakResultatPerioder = perioder.stream()
            .map(periode -> {
                var uttakAktiviteter = periode.getAktiviteter().stream()
                    .map(aktivitet -> mapAktivitet(input, aktivitet, periode.getFom(), periode.isGraderingInnvilget()))
                    .collect(Collectors.toList());
                return new UttakResultatPeriode(periode.getFom(), periode.getTom(), uttakAktiviteter,
                    periode.isOpphold());
            })
            .collect(Collectors.toList());

        return new UttakResultat(uttakResultatPerioder);
    }

    private UttakAktivitet mapAktivitet(UttakInput input, ForeldrepengerUttakPeriodeAktivitet uttakResultatPeriodeAktivitet, LocalDate periodeFom, boolean periodeGraderingInvilget) {
        var utbetalingsgrad = uttakResultatPeriodeAktivitet.getUtbetalingsgrad();
        var stillingsprosent = mapStillingsprosent(input, uttakResultatPeriodeAktivitet, periodeFom);
        var totalStillingsprosent = finnTotalStillingsprosentHosAG(input, uttakResultatPeriodeAktivitet, periodeFom);

        var uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);
        var arbeidstidsprosent = finnArbeidsprosent(uttakResultatPeriodeAktivitet, uttakYrkesaktiviteter, periodeFom);

        var arbeidsforhold = mapArbeidsforhold(uttakResultatPeriodeAktivitet.getUttakAktivitet());
        var aktivitetStatus = MapUttakArbeidTypeTilAktivitetStatus.map(uttakResultatPeriodeAktivitet.getUttakArbeidType());

        var skalGraderes = periodeGraderingInvilget && uttakResultatPeriodeAktivitet.isSøktGraderingForAktivitetIPeriode();

        return UttakAktivitet.ny(aktivitetStatus)
            .medArbeidsforhold(arbeidsforhold)
            .medUtbetalingsgrad(utbetalingsgrad.decimalValue())
            .medStillingsgrad(stillingsprosent, totalStillingsprosent)
            .medGradering(skalGraderes, arbeidstidsprosent);
    }

    private BigDecimal finnTotalStillingsprosentHosAG(UttakInput input, ForeldrepengerUttakPeriodeAktivitet uttakAktivitet, LocalDate periodeFom) {
        var arbeidsgiver = uttakAktivitet.getArbeidsgiver();
        return arbeidsgiver.isPresent()?
            input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(), InternArbeidsforholdRef.nullRef(), periodeFom):
            BigDecimal.valueOf(100);
    }

    private BigDecimal finnArbeidsprosent(ForeldrepengerUttakPeriodeAktivitet uttakResultatPeriodeAktivitet,
                                        UttakYrkesaktiviteter uttakYrkesaktiviteter, LocalDate periodeFom) {
        if (!uttakResultatPeriodeAktivitet.isSøktGraderingForAktivitetIPeriode()) return BigDecimal.ZERO;
        var aktivitet= uttakResultatPeriodeAktivitet.getUttakAktivitet();
        final var arbeidsgiver = aktivitet.getArbeidsgiver();
        if (arbeidsgiver.isPresent()) {
            final var stillingsprosent = uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(),
                aktivitet.getArbeidsforholdRef(), periodeFom);
            final var totalStillingsprosent = uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(),
                null, periodeFom);
            final var arbeidsprosentandel = finnArbeidsprosentandel(stillingsprosent, totalStillingsprosent);
            return uttakResultatPeriodeAktivitet.getArbeidsprosent().multiply(arbeidsprosentandel);
        }
        return uttakResultatPeriodeAktivitet.getArbeidsprosent();
    }

    private BigDecimal finnArbeidsprosentandel(BigDecimal stillingsprosent, BigDecimal totalStillingsprosent) {
        var arbeidsprosentandel = BigDecimal.ONE;
        if (stillingsprosent.compareTo(BigDecimal.ZERO) > 0 && totalStillingsprosent.compareTo(BigDecimal.ZERO) > 0) {
            arbeidsprosentandel = stillingsprosent.divide(totalStillingsprosent,10, RoundingMode.HALF_UP);
        }
        return arbeidsprosentandel;
    }

    private Arbeidsforhold mapArbeidsforhold(ForeldrepengerUttakAktivitet uttakAktivitet) {
        if (!uttakAktivitet.getUttakArbeidType().erArbeidstakerEllerFrilans()) {
            return null;
        }
        return ArbeidsforholdMapper.mapArbeidsforholdFraUttakAktivitet(uttakAktivitet.getArbeidsgiver(),
            uttakAktivitet.getArbeidsforholdRef(), uttakAktivitet.getUttakArbeidType());
    }

    private BigDecimal mapStillingsprosent(UttakInput input, ForeldrepengerUttakPeriodeAktivitet uttakAktivitet, LocalDate periodeFom) {
        BigDecimal stillingsprosent;
        if (UttakArbeidType.FRILANS.equals(uttakAktivitet.getUttakArbeidType())) {
            //Brukes denne ved frilans?
            stillingsprosent = BigDecimal.valueOf(100);
        } else {
            stillingsprosent = finnStillingsprosent(input, uttakAktivitet, periodeFom);
        }
        return stillingsprosent;
    }

    protected BigDecimal finnStillingsprosent(UttakInput input, ForeldrepengerUttakPeriodeAktivitet uttakAktivitet, LocalDate periodeFom) {
        var iaRef = uttakAktivitet.getArbeidsforholdRef();
        var arbeidsgiver = uttakAktivitet.getArbeidsgiver();
        return arbeidsgiver.isPresent()?
            input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(), iaRef, periodeFom):
            BigDecimal.valueOf(100);
    }

}
