package no.nav.foreldrepenger.ytelse.beregning.fp;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
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

    public UttakResultat mapFra(UttakResultatEntitet uttakResultat, UttakInput input) {
        List<UttakResultatPeriode> uttakResultatPerioder = uttakResultat.getGjeldendePerioder().getPerioder().stream()
            .map(periode -> {
                List<UttakAktivitet> uttakAktiviteter = periode.getAktiviteter().stream()
                    .map(aktivitet -> mapAktivitet(input, aktivitet))
                    .collect(Collectors.toList());
                return new UttakResultatPeriode(periode.getFom(), periode.getTom(), uttakAktiviteter, erOppholdsPeriode(periode));
            })
            .collect(Collectors.toList());

        return new UttakResultat(uttakResultatPerioder);
    }

    private UttakAktivitet mapAktivitet(UttakInput input, UttakResultatPeriodeAktivitetEntitet uttakResultatPeriodeAktivitet) {
        BigDecimal utbetalingsgrad = uttakResultatPeriodeAktivitet.getUtbetalingsprosent();
        BigDecimal stillingsprosent = mapStillingsprosent(input, uttakResultatPeriodeAktivitet);
        Arbeidsforhold arbeidsforhold = mapArbeidsforhold(uttakResultatPeriodeAktivitet.getUttakAktivitet());
        AktivitetStatus aktivitetStatus = MapUttakArbeidTypeTilAktivitetStatus.map(uttakResultatPeriodeAktivitet.getUttakArbeidType());

        return new UttakAktivitet(stillingsprosent, utbetalingsgrad, arbeidsforhold, aktivitetStatus, uttakResultatPeriodeAktivitet.isGraderingInnvilget());
    }

    private Arbeidsforhold mapArbeidsforhold(UttakAktivitetEntitet uttakAktivitet) {
        if (!uttakAktivitet.getUttakArbeidType().erArbeidstakerEllerFrilans()) {
            return null;
        }
        return ArbeidsforholdMapper.mapArbeidsforholdFraUttakAktivitet(uttakAktivitet.getArbeidsgiver(),
            uttakAktivitet.getArbeidsforholdRef(), uttakAktivitet.getUttakArbeidType());
    }

    private BigDecimal mapStillingsprosent(UttakInput input, UttakResultatPeriodeAktivitetEntitet uttakAktivitet) {
        BigDecimal stillingsprosent;
        if (UttakArbeidType.FRILANS.equals(uttakAktivitet.getUttakArbeidType())) {
            //Brukes denne ved frilans?
            stillingsprosent = BigDecimal.valueOf(100);
        } else {
            stillingsprosent = finnStillingsprosent(input, uttakAktivitet);
        }
        return stillingsprosent;
    }

    protected BigDecimal finnStillingsprosent(UttakInput input, UttakResultatPeriodeAktivitetEntitet uttakAktivitet) {
        InternArbeidsforholdRef iaRef = InternArbeidsforholdRef.ref(uttakAktivitet.getArbeidsforholdId());
        return input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(uttakAktivitet.getArbeidsgiverIdentifikator(), iaRef, uttakAktivitet.getFom());
    }

    private boolean erOppholdsPeriode(UttakResultatPeriodeEntitet periode) {
        return !OppholdÅrsak.UDEFINERT.equals(periode.getOppholdÅrsak());
    }
}
