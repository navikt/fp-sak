package no.nav.foreldrepenger.ytelse.beregning.fp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;
import no.nav.foreldrepenger.ytelse.beregning.adapter.ArbeidsforholdMapper;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapUttakArbeidTypeTilAktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

@ApplicationScoped
public class MapUttakResultatFraVLTilRegel {


    @Inject
    public MapUttakResultatFraVLTilRegel() {
    }

    public UttakResultat mapFra(ForeldrepengerUttak uttakResultat, UttakInput input) {
        List<UttakResultatPeriode> uttakResultatPerioder = uttakResultat.getGjeldendePerioder().stream()
            .map(periode -> {
                List<UttakAktivitet> uttakAktiviteter = periode.getAktiviteter().stream()
                    .map(aktivitet -> mapAktivitet(input, aktivitet, periode.getFom(), periode.isGraderingInnvilget()))
                    .collect(Collectors.toList());
                return new UttakResultatPeriode(periode.getFom(), periode.getTom(), uttakAktiviteter, erOppholdsPeriode(periode));
            })
            .collect(Collectors.toList());

        return new UttakResultat(uttakResultatPerioder);
    }

    private UttakAktivitet mapAktivitet(UttakInput input, ForeldrepengerUttakPeriodeAktivitet uttakResultatPeriodeAktivitet, LocalDate periodeFom, boolean periodeGraderingInvilget) {
        BigDecimal utbetalingsgrad = uttakResultatPeriodeAktivitet.getUtbetalingsgrad();
        BigDecimal stillingsprosent = mapStillingsprosent(input, uttakResultatPeriodeAktivitet, periodeFom);

        ForeldrepengerUttakAktivitet aktivitetEntitet = uttakResultatPeriodeAktivitet.getUttakAktivitet();
        UttakYrkesaktiviteter uttakYrkesaktiviteter = new UttakYrkesaktiviteter(input);
        BigDecimal arbeidstidsprosent = finnArbeidsprosent(uttakResultatPeriodeAktivitet, aktivitetEntitet, uttakYrkesaktiviteter, periodeFom);

        Arbeidsforhold arbeidsforhold = mapArbeidsforhold(uttakResultatPeriodeAktivitet.getUttakAktivitet());
        AktivitetStatus aktivitetStatus = MapUttakArbeidTypeTilAktivitetStatus.map(uttakResultatPeriodeAktivitet.getUttakArbeidType());

        return new UttakAktivitet(stillingsprosent, arbeidstidsprosent, utbetalingsgrad, arbeidsforhold, aktivitetStatus, periodeGraderingInvilget && uttakResultatPeriodeAktivitet.isSøktGraderingForAktivitetIPeriode());
    }

    private BigDecimal finnArbeidsprosent(ForeldrepengerUttakPeriodeAktivitet uttakResultatPeriodeAktivitet,
                                        UttakYrkesaktiviteter uttakYrkesaktiviteter, LocalDate periodeFom) {
        ForeldrepengerUttakAktivitet aktivitet= uttakResultatPeriodeAktivitet.getUttakAktivitet();
        final Optional<Arbeidsgiver> arbeidsgiver = aktivitet.getArbeidsgiver();
        if(arbeidsgiver.isPresent()) {

            final BigDecimal stillingsprosent = uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(),
                aktivitet.getArbeidsforholdRef(), periodeFom);
            final BigDecimal totalStillingsprosent = uttakYrkesaktiviteter.finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(),
                null, periodeFom);

            final BigDecimal arbeidsprosentandel = finnArbeidsprosentandel(stillingsprosent, totalStillingsprosent);

            if (uttakResultatPeriodeAktivitet.isSøktGraderingForAktivitetIPeriode()) {
                return uttakResultatPeriodeAktivitet.getArbeidsprosent().multiply(arbeidsprosentandel).setScale(2);
            } else {
                return stillingsprosent.multiply(arbeidsprosentandel).setScale(2);
            }
        } else if (uttakResultatPeriodeAktivitet.isSøktGraderingForAktivitetIPeriode()) {
            return uttakResultatPeriodeAktivitet.getArbeidsprosent().setScale(2);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal finnArbeidsprosentandel(BigDecimal stillingsprosent, BigDecimal totalStillingsprosent) {
        BigDecimal arbeidsprosentandel = BigDecimal.ONE;
        if (stillingsprosent.compareTo(BigDecimal.ZERO) > 0 && totalStillingsprosent.compareTo(BigDecimal.ZERO) > 0) {
            arbeidsprosentandel = stillingsprosent.divide(totalStillingsprosent).setScale(2, RoundingMode.HALF_UP);
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
        Optional<Arbeidsgiver> arbeidsgiver = uttakAktivitet.getArbeidsgiver();
        return arbeidsgiver.isPresent()?
            input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(arbeidsgiver.get(), iaRef, periodeFom):
            BigDecimal.valueOf(100);
    }

    private boolean erOppholdsPeriode(ForeldrepengerUttakPeriode periode) {
        return !OppholdÅrsak.UDEFINERT.equals(periode.getOppholdÅrsak());
    }
}
