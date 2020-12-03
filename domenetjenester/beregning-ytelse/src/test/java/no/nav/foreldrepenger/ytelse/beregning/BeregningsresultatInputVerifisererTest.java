package no.nav.foreldrepenger.ytelse.beregning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulus.felles.tid.AbstractIntervall;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;

public class BeregningsresultatInputVerifisererTest {
    private Beregningsgrunnlag.Builder bgBuilder = Beregningsgrunnlag.builder().medSkjæringstidspunkt(LocalDate.of(2020, 1, 1))
            .medAktivitetStatuser(Collections.singletonList(AktivitetStatus.ATFL));
    private List<UttakResultatPeriode> uttakPerioder = new ArrayList<>();

    @Test
    public void skal_teste_at_uttak_andeler_kun_valideres_mot_bg_andeler_i_sin_egen_periode() {
        BeregningsgrunnlagPrArbeidsforhold arbfor1 = lagBGArbeidsforhold("999999999", null, false);
        BeregningsgrunnlagPrArbeidsforhold arbfor2 = lagBGArbeidsforhold("999999998", null, false);
        lagBGPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), arbfor1);
        lagBGPeriode(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 11), arbfor1, arbfor2);
        lagBGPeriode(LocalDate.of(2020, 9, 12), LocalDate.of(2020, 10, 21), arbfor1, arbfor2);
        lagBGPeriode(LocalDate.of(2020, 10, 22), AbstractIntervall.TIDENES_ENDE, arbfor1, arbfor2);
        UttakAktivitet uttakAktivitet = lagUttakAktivitet("999999999", null, false);
        UttakAktivitet uttakAktivitet2 = lagUttakAktivitet("999999998", null, false);
        lagUttakPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 10, 21), uttakAktivitet);
        lagUttakPeriode(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 11), uttakAktivitet2);
        lagUttakPeriode(LocalDate.of(2020, 9, 12), LocalDate.of(2020, 10, 21), uttakAktivitet2);

        UttakResultat uttakResultat = new UttakResultat(uttakPerioder);
        BeregningsresultatRegelmodell input = new BeregningsresultatRegelmodell(bgBuilder.build(), uttakResultat);
        BeregningsresultatInputVerifiserer.verifiserAndelerIUttakLiggerIBeregning(input);
    }

    @Test
    public void skal_ikke_validere_andeler_som_ligger_i_perioder_med_fom_etter_siste_uttaksdag() {
        BeregningsgrunnlagPrArbeidsforhold arbfor1 = lagBGArbeidsforhold("999999999", null, false);
        BeregningsgrunnlagPrArbeidsforhold arbfor2 = lagBGArbeidsforhold("999999998", null, false);
        lagBGPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), arbfor1);
        lagBGPeriode(LocalDate.of(2020, 9, 1), LocalDate.of(9999, 12, 31), arbfor1, arbfor2);
        UttakAktivitet uttakAktivitet = lagUttakAktivitet("999999999", null, false);
        lagUttakPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), uttakAktivitet);

        UttakResultat uttakResultat = new UttakResultat(uttakPerioder);
        BeregningsresultatRegelmodell input = new BeregningsresultatRegelmodell(bgBuilder.build(), uttakResultat);
        BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(input);
    }

    @Test
    public void en_periode_skal_matches_med_uttak() {
        BeregningsgrunnlagPrArbeidsforhold arbfor1 = lagBGArbeidsforhold("999999999", null, false);
        lagBGPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(9999, 12, 31), arbfor1);
        UttakAktivitet uttakAktivitet = lagUttakAktivitet("999999999", null, false);
        lagUttakPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), uttakAktivitet);

        UttakResultat uttakResultat = new UttakResultat(uttakPerioder);
        BeregningsresultatRegelmodell input = new BeregningsresultatRegelmodell(bgBuilder.build(), uttakResultat);
        BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(input);
    }


    private BeregningsgrunnlagPrArbeidsforhold lagBGArbeidsforhold(String orgnr, String referanse, boolean erFrilans) {
        Arbeidsforhold arbeidsforhold = erFrilans ? Arbeidsforhold.frilansArbeidsforhold()
                : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr, referanse);
        return BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(arbeidsforhold).medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .build();
    }

    private void lagBGPeriode(LocalDate fom, LocalDate tom, BeregningsgrunnlagPrArbeidsforhold... arbfor) {
        BeregningsgrunnlagPrStatus.Builder andelBuilder = BeregningsgrunnlagPrStatus.builder().medAktivitetStatus(AktivitetStatus.ATFL);
        List<BeregningsgrunnlagPrArbeidsforhold> bgAndelArbfor = Arrays.asList(arbfor);
        bgAndelArbfor.forEach(andelBuilder::medArbeidsforhold);
        BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder().medPeriode(Periode.of(fom, tom));
        List<BeregningsgrunnlagPrStatus> andelerListe = Arrays.asList(andelBuilder.build());
        andelerListe.forEach(periodeBuilder::medBeregningsgrunnlagPrStatus);
        bgBuilder.medBeregningsgrunnlagPeriode(periodeBuilder.build());
    }

    private void lagUttakPeriode(LocalDate fom, LocalDate tom, UttakAktivitet... andeler) {
        uttakPerioder.add(new UttakResultatPeriode(fom, tom, Arrays.asList(andeler), false));
    }

    private UttakAktivitet lagUttakAktivitet(String orgnr, String referanse, boolean erFrilans) {
        Arbeidsforhold arbeidsforhold = erFrilans ? Arbeidsforhold.frilansArbeidsforhold()
                : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr, referanse);
        return new UttakAktivitet(BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100), arbeidsforhold, AktivitetStatus.ATFL,
                false, BigDecimal.valueOf(100));
    }
}
