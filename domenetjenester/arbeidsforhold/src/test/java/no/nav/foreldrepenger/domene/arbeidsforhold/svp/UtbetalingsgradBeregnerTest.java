package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.konfig.Tid;

class UtbetalingsgradBeregnerTest {

    @Test
    void case_der_grad_regnes_ut_med_desimaler() {
        // Arrange
        var jordmorsdato = LocalDate.of(2023, 8, 23);
        var delvisTilrettelegging = LocalDate.of(2023, 8, 23);
        var terminDato = LocalDate.of(2024, 1, 14);
        var test123 = Arbeidsgiver.virksomhet("922839204");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(37.11)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 1, 1), LocalDate.of(9999, 12, 31)));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(89.57));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner
            .beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        // Assert
        assertThat(tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(58.57));
    }

    @Test
    void skal_ikke_ignorere_perioder_i_aareg_som_ligger_mellom_jordmor_og_termindato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 28);
        var delvisTilrettelegging = LocalDate.of(2019, 7, 1);
        var slutteArbeid = LocalDate.of(2019, 8, 27);
        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(40)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        var aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 7, 31)));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        var aktivitetsAvtaleBuilder3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 8, 1), Tid.TIDENES_ENDE));
        aktivitetsAvtaleBuilder3.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale3 = aktivitetsAvtaleBuilder3.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner
                .beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2, aktivitetsAvtale3), svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_for_periodene() {
        // Arrange
        var jordmorsdato = LocalDate.now().minusDays(20);
        var helTilrettelegging = LocalDate.now().minusDays(20);
        var delvisTilrettelegging = LocalDate.now().minusDays(10);
        var slutteArbeid = LocalDate.now().plusDays(10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_når_det_finns_flere_delvise_perioder() {
        // Arrange
        var jordmorsdato = LocalDate.now().minusDays(20);
        var delvisTilrettelegging = LocalDate.now().minusDays(20);
        var delvisTilrettelegging2 = LocalDate.now().minusDays(10);
        var slutteArbeid = LocalDate.now().plusDays(10);
        var helTilrettelegging = LocalDate.now().plusDays(15);

        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(34)))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging2, BigDecimal.valueOf(50)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging2.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging2, slutteArbeid.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, helTilrettelegging.minusDays(1));
        var fjerdePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(66));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(fjerdePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_når_slutte_arbeid_er_oppgitt_i_søknad() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var slutteArbeid = LocalDate.of(2019, 5, 15);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));

    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_når_delvis_til_rettelegging_er_oppgitt_i_søknad() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(35)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(65));

    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_når_delvis_til_rettelegging_er_oppgitt_i_søknad_fra_og_med_jordmorsdato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(35)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(65));
    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_når_slutet_i_arbeid_er_oppgitt_i_søknad_fra_og_med_jordmorsdato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var sluttetIArbeid = LocalDate.of(2019, 5, 10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(ingenTilrettelegging(sluttetIArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(sluttetIArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_beregne_2_perioder_når_hel_tilrettelegging_og_sluttet_i_arbeid_er_oppgitt_i_søknad_og_hel_tilretteligging_er_fra_og_med_jordmorsdato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var sluttetIArbeid = LocalDate.of(2019, 5, 15);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(jordmorsdato))
                .medTilretteleggingFom(ingenTilrettelegging(sluttetIArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, sluttetIArbeid.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(sluttetIArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_beregne_3_perioder_når_delvis_og_hel_tilrettelegging_er_oppgitt_i_søknad() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var sluttetIArbeid = LocalDate.of(2019, 5, 12);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 17);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(ingenTilrettelegging(sluttetIArbeid))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    void skal_beregn_3_perioder_hvis_hel_tilrettelegging_forekommer_noen_dager_etter_jordmorsdato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var helTilrettelegging = LocalDate.of(2019, 5, 12);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, helTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, delvisTilrettelegging.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    void skal_håndtere_at_tilretteleggingen_er_satt_til_å_gjelde_før_jordsmorsdato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var helTilrettelegging = LocalDate.of(2019, 5, 5);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 6);
        var terminDatoMinus3 = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    void skal_håndtere_at_tilretteleggingen_er_satt_til_å_gjelde_etter_termin_minus_3_uker() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var helTilrettelegging = LocalDate.of(2019, 5, 25);
        var terminDatoMinus3 = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3);

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(periode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_håndtere_at_søker_er_frilans() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var helTilrettelegging = LocalDate.of(2019, 5, 12);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
                .medArbeidType(ArbeidType.FRILANSER)
                .medArbeidsgiver(test123)
                .build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregnUtenAAreg(svpTilrettelegging, terminDato);

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, helTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, delvisTilrettelegging.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    void skal_håndtere_at_arbeidsforholdet_slutter_mitt_i_utbetalingsperioden() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var helTilrettelegging = LocalDate.of(2019, 5, 12);
        var delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        var terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        var terminDato = LocalDate.of(2019, 6, 11);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
                .medArbeidType(ArbeidType.FRILANSER)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag.minusDays(5)));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, helTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, delvisTilrettelegging.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    void skal_håndtere_0_i_stillingsprosent() {
        // Arrange
        var jordmorsdato = LocalDate.now().minusDays(20);
        var helTilrettelegging = LocalDate.now().minusDays(20);
        var delvisTilrettelegging = LocalDate.now().minusDays(10);
        var slutteArbeid = LocalDate.now().plusDays(10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_håndtere_at_det_søkes_om_høyere_enn_opprinnelig_stillingsprosent() {
        // Arrange
        var jordmorsdato = LocalDate.now().minusDays(20);
        var helTilrettelegging = LocalDate.now().minusDays(20);
        var delvisTilrettelegging = LocalDate.now().minusDays(10);
        var slutteArbeid = LocalDate.now().plusDays(10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        var aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(40));
        var aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, slutteArbeid.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_håndtere_flere_0_i_stillingsprosent() {
        // Arrange
        var jordmorsdato = LocalDate.now().minusDays(20);
        var helTilrettelegging = LocalDate.now().minusDays(20);
        var delvisTilrettelegging = LocalDate.now().minusDays(10);
        var slutteArbeid = LocalDate.now().plusDays(10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        var aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2),
                svpTilrettelegging, terminDato, Collections.emptyList());
        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void teste_3_aktivitetsavtaler_med_0_stillingsprosent() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 9, 9);
        var slutteArbeid = LocalDate.of(2019, 9, 9);
        var terminDato = LocalDate.of(2020, 4, 18);
        var terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), Tid.TIDENES_ENDE));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        var aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2017, 12, 1), LocalDate.of(2018, 8, 31)));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        var aktivitetsAvtaleBuilder3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 12, 31)));
        aktivitetsAvtaleBuilder3.medProsentsats(BigDecimal.valueOf(0));
        var aktivitetsAvtale3 = aktivitetsAvtaleBuilder3.build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner
                .beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2, aktivitetsAvtale3), svpTilrettelegging, terminDato, Collections.emptyList());
        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_håndtere_2_arbeidsforhold_i_samme_bedrift_der_det_ene_arbeidsforholdet_har_null_i_stillingsprosent() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 12);
        var slutteArbeid = LocalDate.of(2019, 5, 12);
        var terminDato = LocalDate.of(2019, 12, 8);
        var terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktiviteter = List.of(
                // første arbeidsforhold
                lagAktivitetsAvtale(LocalDate.of(2019, 7, 1), LocalDate.of(9999, 12, 31), BigDecimal.valueOf(100)),
                lagAktivitetsAvtale(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30), BigDecimal.valueOf(100)),
                lagAktivitetsAvtale(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31), BigDecimal.valueOf(100)),
                // andre arbeidsforhold
                lagAktivitetsAvtale(LocalDate.of(2019, 1, 1), LocalDate.of(9999, 12, 31), BigDecimal.valueOf(0)));

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(aktiviteter, svpTilrettelegging, terminDato,
                Collections.emptyList());
        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_håndtere_arbeidsforhold_som_starter_etter_jordmordsdato() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 5, 23);
        var delvisTilrettelegging = LocalDate.of(2019, 8, 1);
        var terminDato = LocalDate.of(2019, 12, 9);
        var terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(50)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var arbeidsforholdStart = LocalDate.of(2019, 6, 1);
        var aktiviteter = List.of(
                lagAktivitetsAvtale(arbeidsforholdStart, LocalDate.of(9999, 12, 31), BigDecimal.valueOf(100)));

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(aktiviteter, svpTilrettelegging, terminDato,
                Collections.emptyList());
        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, arbeidsforholdStart.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsforholdStart, delvisTilrettelegging.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat).hasSize(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void skal_teste_at_irrelevante_perioder_ignoreres_og_at_desimaltall_i_stillingsprosent_går_bra() {
        // Arrange
        var jordmorsdato = LocalDate.of(2019, 11, 15);
        var ingenTilrettelegging = LocalDate.of(2019, 11, 15);
        var terminDato = LocalDate.of(2020, 7, 13);
        var terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(ingenTilrettelegging(ingenTilrettelegging))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 8, 1), LocalDate.of(9999, 12, 31));
        var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 4, 1), LocalDate.of(2019, 6, 30));
        var periode4 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 7, 31));
        var periode5 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 31));
        var periode6 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 8, 1), LocalDate.of(2019, 1, 31));
        var periode7 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 7, 1), LocalDate.of(2018, 7, 31));

        var aktiviteter = List.of(
                lagAktivitetsAvtale(periode1, BigDecimal.valueOf(0.00)),
                lagAktivitetsAvtale(periode2, BigDecimal.valueOf(0.00)),
                lagAktivitetsAvtale(periode3, BigDecimal.valueOf(0.00)),
                lagAktivitetsAvtale(periode4, BigDecimal.valueOf(0.00)),
                lagAktivitetsAvtale(periode5, BigDecimal.valueOf(0.00)),
                lagAktivitetsAvtale(periode6, BigDecimal.valueOf(0.00)),
                lagAktivitetsAvtale(periode7, BigDecimal.valueOf(0.00)));

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(aktiviteter, svpTilrettelegging, terminDato,
                Collections.emptyList());
        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));

    }

    @Test
    void skal_bruke_overstyrt_utbetalingsgrad_i_delvis_tilrettelegging() {
        // Arrange
        var terminDato = LocalDate.of(2019, 7, 1);
        var jordmorsdato = LocalDate.of(2019, 5, 28);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var overstyrtUtbetalingsgrad = BigDecimal.valueOf(10);
        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(new TilretteleggingFOM.Builder()
                        .medStillingsprosent(BigDecimal.valueOf(60))
                        .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                        .medFomDato(jordmorsdato)
                        .medOverstyrtUtbetalingsgrad(overstyrtUtbetalingsgrad)
                        .build())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, jordmorsdato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();
        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato.minusWeeks(3).minusDays(1));

        // Assert
        assertThat(resultat.get(periode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(overstyrtUtbetalingsgrad);
    }

    /**
     * Privat arbeidsgiver uten stillingsprosent i aareg
     */
    @Test
    void skal_sette_full_utbetalingsgrad_for_ingen_tilrettelegging_når_stillingsprosent_ikke_er_satt() {
        // Arrange
        var terminDato = LocalDate.of(2020, 10, 1);
        var jordmorsdato = LocalDate.of(2020, 2, 3);
        var aktørId = AktørId.dummy();
        var test123 = Arbeidsgiver.person(aktørId);

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(new TilretteleggingFOM.Builder()
                        .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                        .medFomDato(jordmorsdato)
                        .build())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.of(2019, 8, 20)));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();
        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, Collections.emptyList());

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato.minusWeeks(3).minusDays(1));

        // Assert
        assertThat(resultat.get(periode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void skal_beregne_riktig_utbetalingsgrad_for_periode_med_velferdspermisjon() {
        // Arrange
        var jordmorsdato = LocalDate.now().minusDays(20);
        var helTilrettelegging = LocalDate.now().minusDays(20);
        var delvisTilrettelegging = LocalDate.now().minusDays(10);
        var slutteArbeid = LocalDate.now().plusDays(10);
        var terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        var terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        var test123 = Arbeidsgiver.virksomhet("Test123");

        var svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
                .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
                .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
                .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(test123)
                .build();

        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        var aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        var permisjonBuilder = YrkesaktivitetBuilder.nyPermisjonBuilder();
        var velferdspermisjon = permisjonBuilder.medPeriode(jordmorsdato, terminDato)
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.VELFERDSPERMISJON)
                .medProsentsats(BigDecimal.valueOf(20))
                .build();

        // Act
        var tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale),
                svpTilrettelegging, terminDato, List.of(velferdspermisjon));

        var resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
                .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        var førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        var andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        var tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(43.75));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    private AktivitetsAvtale lagAktivitetsAvtale(LocalDate jordmorsdato, LocalDate terminDato, BigDecimal prosentsats) {
        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(prosentsats);
        return aktivitetsAvtaleBuilder.build();
    }

    private AktivitetsAvtale lagAktivitetsAvtale(DatoIntervallEntitet periode, BigDecimal prosentsats) {
        var aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(periode);
        aktivitetsAvtaleBuilder.medProsentsats(prosentsats);
        return aktivitetsAvtaleBuilder.build();
    }

    private TilretteleggingFOM ingenTilrettelegging(LocalDate slutteArbeid) {
        return new TilretteleggingFOM.Builder().medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING).medFomDato(slutteArbeid).build();
    }

    private TilretteleggingFOM delvisTilrettelegging(LocalDate delvisTilrettelegging, BigDecimal stillingsprosent) {
        return new TilretteleggingFOM.Builder().medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(delvisTilrettelegging)
                .medStillingsprosent(stillingsprosent).build();
    }

    private TilretteleggingFOM helTilrettelegging(LocalDate helTilrettelegging) {
        return new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.HEL_TILRETTELEGGING)
                .medFomDato(helTilrettelegging)
                .build();
    }
}
