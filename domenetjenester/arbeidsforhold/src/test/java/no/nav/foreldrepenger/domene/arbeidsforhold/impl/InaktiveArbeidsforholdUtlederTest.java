package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistAndel;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvistAndelBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.konfig.Tid;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InaktiveArbeidsforholdUtlederTest {
    private static final LocalDate STP = LocalDate.of(2021,10,1);
    private static final AktørId AKTØR = new AktørId("0000000000000");
    private InntektArbeidYtelseAggregatBuilder data = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = data.getAktørArbeidBuilder(AKTØR);
    private InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder ytelseBuilder = data.getAktørYtelseBuilder(AKTØR);
    private InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder inntektBuilder = data.getAktørInntektBuilder(AKTØR);
    private List<Inntektsmelding> inntektsmeldinger = new ArrayList<>();


    @Test
    public void er_aktiv_når_iay_mangler() {
        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), null);

        // Assert
        assertThat(erInaktivt).isFalse();
    }

    @Test
    public void er_aktiv_når_det_er_nyoppstartet() {
        // Arrange
        lagArbeid(arbeidsgiver("999999999"), STP.minusMonths(2), Tid.TIDENES_ENDE);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isFalse();
    }

    @Test
    public void er_inaktiv_når_det_er_gammel_uten_inntekt() {
        // Arrange
        lagArbeid(arbeidsgiver("999999999"), STP.minusYears(2), Tid.TIDENES_ENDE);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isTrue();
    }

    @Test
    public void er_aktiv_når_det_nylig_er_betalt_inntekt() {
        // Arrange
        lagInntekt(arbeidsgiver("999999999"), STP.minusYears(2), 30);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isFalse();
    }

    @Test
    public void er_inaktivt_når_det_er_lenge_siden_forrige_inntekt_selv_om_det_finnes_i_aareg() {
        // Arrange
        lagInntekt(arbeidsgiver("999999999"), STP.minusYears(2), 12);
        lagArbeid(arbeidsgiver("999999999"), STP.minusYears(2), Tid.TIDENES_ENDE);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isTrue();
    }

    @Test
    public void er_aktiv_når_det_er_arbeid_uten_inntekt_men_med_ytelse() {
        // Arrange
        lagArbeid(arbeidsgiver("999999999"), STP.minusYears(2), Tid.TIDENES_ENDE);
        lagYtelse(arbeidsgiver("999999999"), STP.minusMonths(2), STP.minusMonths(1), RelatertYtelseType.FORELDREPENGER);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isFalse();
    }

    @Test
    public void er_inaktiv_når_det_er_arbeid_uten_inntekt_med_ytelse_som_ikke_er_relevant_for_vurderingen() {
        // Arrange
        lagArbeid(arbeidsgiver("999999999"), STP.minusYears(2), Tid.TIDENES_ENDE);
        lagYtelse(arbeidsgiver("999999999"), STP.minusMonths(2), STP.minusMonths(1), RelatertYtelseType.DAGPENGER);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isTrue();
    }

    @Test
    public void bruker_ytelse_anvist_når_ingen_anviste_andeler() {
        // Arrange
        lagArbeid(arbeidsgiver("999999999"), STP.minusYears(2), Tid.TIDENES_ENDE);
        lagYtelse(null, STP.minusMonths(2), STP.minusMonths(1), RelatertYtelseType.SYKEPENGER);

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isFalse();
    }

    @Test
    public void er_aktivt_når_det_har_kommet_inntektsmelding_fra_arbeidsgiver() {
        // Arrange
        lagIM(arbeidsgiver("999999999"));

        // Act
        var erInaktivt = utled(arbeidsgiver("999999999"), byggIAY());

        // Assert
        assertThat(erInaktivt).isFalse();
    }

    private void lagIM(Arbeidsgiver arbeidsgiver) {
        var im = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medBeløp(BigDecimal.valueOf(500)).build();
        inntektsmeldinger.add(im);
    }

    private void lagYtelse(Arbeidsgiver arbeidsgiver, LocalDate fom, LocalDate tom, RelatertYtelseType ytelse) {
        YtelseBuilder builder = ytelseBuilder.getYtelselseBuilderForType(Fagsystem.AAREGISTERET, ytelse, new Saksnummer("12313123"));
        builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var ytelseAnvist = builder.getAnvistBuilder()
            .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medDagsats(BigDecimal.valueOf(500))
            .medUtbetalingsgradProsent(BigDecimal.valueOf(100));
        if (arbeidsgiver != null) {
            var anvistAndel = YtelseAnvistAndelBuilder.ny()
            .medArbeidsgiver(arbeidsgiver)
            .medDagsats(BigDecimal.valueOf(500))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build();
            ytelseAnvist.leggTilYtelseAnvistAndel(anvistAndel);
        }
        builder.medYtelseAnvist(ytelseAnvist.build());
        ytelseBuilder.leggTilYtelse(builder);
    }

    private void lagArbeid(Arbeidsgiver ag, LocalDate fom, LocalDate tom) {
        var yaBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aaBuilder = yaBuilder.getAktivitetsAvtaleBuilder();
        var aa = aaBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yaBuilder.leggTilAktivitetsAvtale(aa)
            .medArbeidsgiver(ag)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        arbeidBuilder.leggTilYrkesaktivitet(yaBuilder);
    }

    private void lagInntekt(Arbeidsgiver ag, LocalDate fom, int måneder) {
        InntektBuilder intBuilder = InntektBuilder.oppdatere(Optional.empty());
        intBuilder.medArbeidsgiver(ag).medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING);
        for (int i = 0; i<måneder; i++) {
            LocalDate start = fom.plusMonths(i);
            InntektspostBuilder postBuilder = intBuilder.getInntektspostBuilder();
            postBuilder.medPeriode(start.withDayOfMonth(1), start.with(TemporalAdjusters.lastDayOfMonth()))
                .medBeløp(BigDecimal.valueOf(100))
                .medInntektspostType(InntektspostType.LØNN);
            intBuilder.leggTilInntektspost(postBuilder);
        }
        inntektBuilder.leggTilInntekt(intBuilder);
    }

    private Arbeidsgiver arbeidsgiver(String orgnr) {
        return Arbeidsgiver.virksomhet(orgnr);
    }

    private boolean utled(Arbeidsgiver ag, InntektArbeidYtelseGrunnlag iay) {
        return InaktiveArbeidsforholdUtleder.erInaktivt(ag, Optional.ofNullable(iay), AKTØR, STP);
    }

    private InntektArbeidYtelseGrunnlag byggIAY() {
        data.leggTilAktørArbeid(arbeidBuilder);
        data.leggTilAktørInntekt(inntektBuilder);
        data.leggTilAktørYtelse(ytelseBuilder);
        return InntektArbeidYtelseGrunnlagBuilder.nytt().medData(data).medInntektsmeldinger(inntektsmeldinger).build();
    }

}
