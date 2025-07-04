package no.nav.foreldrepenger.domene.vedtak.observer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class VedtattYtelseForeldrepengerMapperTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    void skal_mappe_ett_arbeidsforhold_med_full_utbetaling_til_bruker() {

        var resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123566324");
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var eksternReferanse = "jifesjsioejf";
        var dagsats = 500;
        var arbeidsforholdReferanse = lagReferanser(arbeidsgiver, arbeidsforholdRef, eksternReferanse);
        BeregningsresultatAndel.builder()
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        var arbeidsforholdReferanser = List.of(arbeidsforholdReferanse);
        var anvisninger = VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapTilkjent(resultat);

        assertThat(anvisninger).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler()).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(eksternReferanse);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skal_mappe_to_arbeidsforhold_med_full_utbetaling_til_arbeidsgiver() {

        var resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        var arbeidsforholdRef1 = InternArbeidsforholdRef.nyRef();
        var eksternReferanse1 = "jifesjsioejf";
        var dagsats1 = 500;
        var arbeidsgiver1 = Arbeidsgiver.virksomhet("123566324");
        var arbeidsforholdReferanse1 = lagReferanser(arbeidsgiver1, arbeidsforholdRef1, eksternReferanse1);
        fullRefusjon(arbeidsgiver1, periode, arbeidsforholdRef1, dagsats1);

        var arbeidsforholdRef2 = InternArbeidsforholdRef.nyRef();
        var eksternReferanse2 = "husefiuse";
        var dagsats2 = 200;
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("463465235");
        var arbeidsforholdReferanse2 = lagReferanser(arbeidsgiver1, arbeidsforholdRef2, eksternReferanse2);
        fullRefusjon(arbeidsgiver2, periode, arbeidsforholdRef2, dagsats2);

        var arbeidsforholdReferanser = List.of(arbeidsforholdReferanse1, arbeidsforholdReferanse2);
        var anvisninger = VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapTilkjent(resultat);

        assertThat(anvisninger).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler()).hasSize(2);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver1.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(eksternReferanse1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats1));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(anvisninger.get(0).getAndeler().get(1).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(1).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver2.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(1).getArbeidsforholdId()).isEqualTo(eksternReferanse2);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(1).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(1).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats2));
        assertThat(anvisninger.get(0).getAndeler().get(1).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    private ArbeidsforholdReferanse lagReferanser(Arbeidsgiver arbeidsgiver1, InternArbeidsforholdRef arbeidsforholdRef2, String eksternReferanse2) {
        return new ArbeidsforholdReferanse(arbeidsgiver1, arbeidsforholdRef2, EksternArbeidsforholdRef.ref(eksternReferanse2));
    }

    @Test
    void skal_mappe_ett_arbeidsforhold_med_full_utbetaling_til_arbeidsgiver() {

        var resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123566324");
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var eksternReferanse = "jifesjsioejf";
        var dagsats = 500;
        var arbeidsforholdReferanse = lagReferanser(arbeidsgiver, arbeidsforholdRef, eksternReferanse);
        fullRefusjon(arbeidsgiver, periode, arbeidsforholdRef, dagsats);

        var arbeidsforholdReferanser = List.of(arbeidsforholdReferanse);
        var anvisninger = VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapTilkjent(resultat);

        assertThat(anvisninger).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler()).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isEqualTo(eksternReferanse);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


    @Test
    void skal_mappe_ett_arbeidsforhold_med_full_utbetaling_til_arbeidsgiver_aggregert() {

        var resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        var arbeidsgiver = Arbeidsgiver.virksomhet("123566324");
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        var arbeidsforholdRef = InternArbeidsforholdRef.nullRef();
        var dagsats = 500;
        fullRefusjon(arbeidsgiver, periode, arbeidsforholdRef, dagsats);

        var arbeidsforholdReferanser = new ArrayList<ArbeidsforholdReferanse>();
        var anvisninger = VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapTilkjent(resultat);

        assertThat(anvisninger).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler()).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().erOrganisasjon()).isTrue();
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsgiverIdent().ident()).isEqualTo(arbeidsgiver.getIdentifikator());
        assertThat(anvisninger.get(0).getAndeler().get(0).getArbeidsforholdId()).isNull();
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.ARBEIDSTAKER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


    @Test
    void skal_mappe_dagpenger() {

        var resultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("regelsporing")
            .build();

        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusDays(10))
            .build(resultat);
        var dagsats = 500;
        BeregningsresultatAndel.builder()
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.DAGPENGER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.DAGPENGER)
            .medAktivitetStatus(AktivitetStatus.DAGPENGER)
            .build(periode);

        var arbeidsforholdReferanser = new ArrayList<ArbeidsforholdReferanse>();
        var anvisninger = VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapTilkjent(resultat);

        assertThat(anvisninger).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler()).hasSize(1);
        assertThat(anvisninger.get(0).getAndeler().get(0).getInntektklasse()).isEqualTo(Inntektklasse.DAGPENGER);
        assertThat(anvisninger.get(0).getAndeler().get(0).getUtbetalingsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(anvisninger.get(0).getAndeler().get(0).getDagsats().getVerdi()).isEqualByComparingTo(BigDecimal.valueOf(dagsats));
        assertThat(anvisninger.get(0).getAndeler().get(0).getRefusjonsgrad().getVerdi()).isEqualByComparingTo(BigDecimal.ZERO);
    }



    private void fullRefusjon(Arbeidsgiver arbeidsgiver, BeregningsresultatPeriode periode, InternArbeidsforholdRef arbeidsforholdRef, int dagsats) {
        BeregningsresultatAndel.builder()
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(false)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);

        BeregningsresultatAndel.builder()
            .medArbeidsforholdRef(arbeidsforholdRef)
            .medArbeidsgiver(arbeidsgiver)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsats(0)
            .medDagsatsFraBg(0)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode);
    }
}
