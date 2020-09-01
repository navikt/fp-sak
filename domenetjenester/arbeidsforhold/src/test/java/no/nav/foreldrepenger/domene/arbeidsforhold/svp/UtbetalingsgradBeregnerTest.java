package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class UtbetalingsgradBeregnerTest {

    @Test
    public void skal_ikke_ignorere_perioder_i_aareg_som_ligger_mellom_jordmor_og_termindato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 28);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 7, 1);
        LocalDate slutteArbeid = LocalDate.of(2019, 8, 27);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(40)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 7, 31)));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 8, 1), AbstractLocalDateInterval.TIDENES_ENDE));
        aktivitetsAvtaleBuilder3.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale3 = aktivitetsAvtaleBuilder3.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2, aktivitetsAvtale3), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_for_periodene() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.now().minusDays(20);
        LocalDate helTilrettelegging = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging = LocalDate.now().minusDays(10);
        LocalDate slutteArbeid = LocalDate.now().plusDays(10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_når_det_finns_flere_delvise_perioder() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging2 = LocalDate.now().minusDays(10);
        LocalDate slutteArbeid = LocalDate.now().plusDays(10);
        LocalDate helTilrettelegging = LocalDate.now().plusDays(15);

        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(34)))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging2, BigDecimal.valueOf(50)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging2.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging2, slutteArbeid.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, helTilrettelegging.minusDays(1));
        DatoIntervallEntitet fjerdePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(66));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(fjerdePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_når_slutte_arbeid_er_oppgitt_i_søknad() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate slutteArbeid = LocalDate.of(2019, 5, 15);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));

    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_når_delvis_til_rettelegging_er_oppgitt_i_søknad() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(35)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(65));

    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_når_delvis_til_rettelegging_er_oppgitt_i_søknad_fra_og_med_jordmorsdato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(35)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(65));
    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_når_slutet_i_arbeid_er_oppgitt_i_søknad_fra_og_med_jordmorsdato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate sluttetIArbeid = LocalDate.of(2019, 5, 10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(ingenTilrettelegging(sluttetIArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(sluttetIArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_beregne_2_perioder_når_hel_tilrettelegging_og_sluttet_i_arbeid_er_oppgitt_i_søknad_og_hel_tilretteligging_er_fra_og_med_jordmorsdato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate sluttetIArbeid = LocalDate.of(2019, 5, 15);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(jordmorsdato))
            .medTilretteleggingFom(ingenTilrettelegging(sluttetIArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, sluttetIArbeid.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(sluttetIArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(2);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_beregne_3_perioder_når_delvis_og_hel_tilrettelegging_er_oppgitt_i_søknad() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate sluttetIArbeid = LocalDate.of(2019, 5, 12);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 17);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(ingenTilrettelegging(sluttetIArbeid))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(2);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    public void skal_beregn_3_perioder_hvis_hel_tilrettelegging_forekommer_noen_dager_etter_jordmorsdato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate helTilrettelegging = LocalDate.of(2019, 5, 12);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, helTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    public void skal_håndtere_at_tilretteleggingen_er_satt_til_å_gjelde_før_jordsmorsdato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate helTilrettelegging = LocalDate.of(2019, 5, 5);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 6);
        LocalDate terminDatoMinus3 = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3);

        // Assert
        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    public void skal_håndtere_at_tilretteleggingen_er_satt_til_å_gjelde_etter_termin_minus_3_uker() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate helTilrettelegging = LocalDate.of(2019, 5, 25);
        LocalDate terminDatoMinus3 = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3);

        // Assert
        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(periode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_håndtere_at_søker_er_frilans() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate helTilrettelegging = LocalDate.of(2019, 5, 12);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
            .medArbeidType(ArbeidType.FRILANSER)
            .medArbeidsgiver(test123)
            .build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregnUtenAAreg(svpTilrettelegging, terminDato);

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, helTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    public void skal_håndtere_at_arbeidsforholdet_slutter_mitt_i_utbetalingsperioden() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate helTilrettelegging = LocalDate.of(2019, 5, 12);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 5, 15);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.of(2019, 5, 20);
        LocalDate terminDato = LocalDate.of(2019, 6, 11);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(66)))
            .medArbeidType(ArbeidType.FRILANSER)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag.minusDays(5)));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, helTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(helTilrettelegging, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(34));
    }

    @Test
    public void skal_håndtere_0_i_stillingsprosent() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.now().minusDays(20);
        LocalDate helTilrettelegging = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging = LocalDate.now().minusDays(10);
        LocalDate slutteArbeid = LocalDate.now().plusDays(10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_håndtere_at_det_søkes_om_høyere_enn_opprinnelig_stillingsprosent() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.now().minusDays(20);
        LocalDate helTilrettelegging = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging = LocalDate.now().minusDays(10);
        LocalDate slutteArbeid = LocalDate.now().plusDays(10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(40));
        AktivitetsAvtale aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, slutteArbeid.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_håndtere_flere_0_i_stillingsprosent() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.now().minusDays(20);
        LocalDate helTilrettelegging = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging = LocalDate.now().minusDays(10);
        LocalDate slutteArbeid = LocalDate.now().plusDays(10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2), svpTilrettelegging, terminDato, Collections.emptyList());
        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(55));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


    @Test
    public void teste_3_aktivitetsavtaler_med_0_stillingsprosent() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 9, 9);
        LocalDate slutteArbeid = LocalDate.of(2019, 9, 9);
        LocalDate terminDato = LocalDate.of(2020, 4, 18);
        LocalDate terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), AbstractLocalDateInterval.TIDENES_ENDE));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2017, 12, 1), LocalDate.of(2018, 8, 31)));
        aktivitetsAvtaleBuilder2.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale2 = aktivitetsAvtaleBuilder2.build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 12, 31)));
        aktivitetsAvtaleBuilder3.medProsentsats(BigDecimal.valueOf(0));
        AktivitetsAvtale aktivitetsAvtale3 = aktivitetsAvtaleBuilder3.build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale, aktivitetsAvtale2, aktivitetsAvtale3), svpTilrettelegging, terminDato, Collections.emptyList());
        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


    @Test
    public void skal_håndtere_2_arbeidsforhold_i_samme_bedrift_der_det_ene_arbeidsforholdet_har_null_i_stillingsprosent() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 12);
        LocalDate slutteArbeid = LocalDate.of(2019, 5, 12);
        LocalDate terminDato = LocalDate.of(2019, 12, 8);
        LocalDate terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        List<AktivitetsAvtale> aktiviteter = List.of(
            //første arbeidsforhold
            lagAktivitetsAvtale(LocalDate.of(2019, 7, 1), LocalDate.of(9999, 12, 31), BigDecimal.valueOf(100)),
            lagAktivitetsAvtale(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 30), BigDecimal.valueOf(100)),
            lagAktivitetsAvtale(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31), BigDecimal.valueOf(100)),
            //andre arbeidsforhold
            lagAktivitetsAvtale(LocalDate.of(2019, 1, 1), LocalDate.of(9999, 12, 31), BigDecimal.valueOf(0))
        );

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(aktiviteter, svpTilrettelegging, terminDato, Collections.emptyList());
        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_håndtere_arbeidsforhold_som_starter_etter_jordmordsdato() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 23);
        LocalDate delvisTilrettelegging = LocalDate.of(2019, 8, 1);
        LocalDate terminDato = LocalDate.of(2019, 12, 9);
        LocalDate terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(50)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        LocalDate arbeidsforholdStart = LocalDate.of(2019, 6, 1);
        List<AktivitetsAvtale> aktiviteter = List.of(
            lagAktivitetsAvtale(arbeidsforholdStart, LocalDate.of(9999, 12, 31), BigDecimal.valueOf(100))
        );

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(aktiviteter, svpTilrettelegging, terminDato, Collections.emptyList());
        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, arbeidsforholdStart.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsforholdStart, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.size()).isEqualTo(3);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    public void skal_teste_at_irrelevante_perioder_ignoreres_og_at_desimaltall_i_stillingsprosent_går_bra() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.of(2019, 11, 15);
        LocalDate ingenTilrettelegging = LocalDate.of(2019, 11, 15);
        LocalDate terminDato = LocalDate.of(2020, 7, 13);
        LocalDate terminDatoMinus3UkerOg1Dag = terminDato.minusWeeks(3).minusDays(1);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(ingenTilrettelegging(ingenTilrettelegging))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 8, 1), LocalDate.of(9999, 12, 31));
        DatoIntervallEntitet periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 4, 1), LocalDate.of(2019, 6, 30));
        DatoIntervallEntitet periode4 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 7, 1), LocalDate.of(2019, 7, 31));
        DatoIntervallEntitet periode5 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 31));
        DatoIntervallEntitet periode6 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 8, 1), LocalDate.of(2019, 1, 31));
        DatoIntervallEntitet periode7 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 7, 1), LocalDate.of(2018, 7, 31));

        List<AktivitetsAvtale> aktiviteter = List.of(
            lagAktivitetsAvtale(periode1, BigDecimal.valueOf(0.00)),
            lagAktivitetsAvtale(periode2, BigDecimal.valueOf(0.00)),
            lagAktivitetsAvtale(periode3, BigDecimal.valueOf(0.00)),
            lagAktivitetsAvtale(periode4, BigDecimal.valueOf(0.00)),
            lagAktivitetsAvtale(periode5, BigDecimal.valueOf(0.00)),
            lagAktivitetsAvtale(periode6, BigDecimal.valueOf(0.00)),
            lagAktivitetsAvtale(periode7, BigDecimal.valueOf(0.00))
        );

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(aktiviteter, svpTilrettelegging, terminDato, Collections.emptyList());
        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDatoMinus3UkerOg1Dag);

        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));

    }

    @Test
    public void skal_bruke_overstyrt_utbetalingsgrad_i_delvis_tilrettelegging() {
        //Arrange
        LocalDate terminDato = LocalDate.of(2019, 7, 1);
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 28);
        LocalDate delvisTilretteleggingFom = jordmorsdato;
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        var overstyrtUtbetalingsgrad = BigDecimal.valueOf(10);
        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(new TilretteleggingFOM.Builder()
                .medStillingsprosent(BigDecimal.valueOf(60))
                .medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING)
                .medFomDato(delvisTilretteleggingFom)
                .medOverstyrtUtbetalingsgrad(overstyrtUtbetalingsgrad)
                .build())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilretteleggingFom, delvisTilretteleggingFom));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();
        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
            .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilretteleggingFom, terminDato.minusWeeks(3).minusDays(1));

        // Assert
        assertThat(resultat.get(periode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(overstyrtUtbetalingsgrad);
    }

    /**
     * Privat arbeidsgiver uten stillingsprosent i aareg
     */
    @Test
    public void skal_sette_full_utbetalingsgrad_for_ingen_tilrettelegging_når_stillingsprosent_ikke_er_satt() {
        //Arrange
        LocalDate terminDato = LocalDate.of(2020, 10, 1);
        LocalDate jordmorsdato = LocalDate.of(2020, 2, 3);
        LocalDate delvisTilretteleggingFom = jordmorsdato;
        AktørId aktørId = AktørId.dummy();
        Arbeidsgiver test123 = Arbeidsgiver.person(aktørId);

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(new TilretteleggingFOM.Builder()
                .medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING)
                .medFomDato(delvisTilretteleggingFom)
                .build())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.of(2019, 8, 20)));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();
        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, Collections.emptyList());

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream()
            .collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilretteleggingFom, terminDato.minusWeeks(3).minusDays(1));

        // Assert
        assertThat(resultat.get(periode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    public void skal_beregne_riktig_utbetalingsgrad_for_periode_med_velferdspermisjon() {
        //Arrange
        LocalDate jordmorsdato = LocalDate.now().minusDays(20);
        LocalDate helTilrettelegging = LocalDate.now().minusDays(20);
        LocalDate delvisTilrettelegging = LocalDate.now().minusDays(10);
        LocalDate slutteArbeid = LocalDate.now().plusDays(10);
        LocalDate terminDatoMinus3UkerOg1Dag = LocalDate.now().plusDays(20);
        LocalDate terminDato = LocalDate.now().plusDays(21).plusWeeks(3);
        Arbeidsgiver test123 = Arbeidsgiver.virksomhet("Test123");

        SvpTilretteleggingEntitet svpTilrettelegging = new SvpTilretteleggingEntitet.Builder().medBehovForTilretteleggingFom(jordmorsdato)
            .medTilretteleggingFom(helTilrettelegging(helTilrettelegging))
            .medTilretteleggingFom(delvisTilrettelegging(delvisTilrettelegging, BigDecimal.valueOf(45)))
            .medTilretteleggingFom(ingenTilrettelegging(slutteArbeid))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(test123)
            .build();

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtale aktivitetsAvtale = aktivitetsAvtaleBuilder.build();

        PermisjonBuilder permisjonBuilder = YrkesaktivitetBuilder.nyPermisjonBuilder();
        Permisjon velferdspermisjon = permisjonBuilder.medPeriode(jordmorsdato, terminDato)
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.VELFERDSPERMISJON)
            .medProsentsats(BigDecimal.valueOf(20))
            .build();

        // Act
        TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad = UtbetalingsgradBeregner.beregn(List.of(aktivitetsAvtale), svpTilrettelegging, terminDato, List.of(velferdspermisjon));

        Map<DatoIntervallEntitet, List<PeriodeMedUtbetalingsgrad>> resultat = tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad().stream().collect(Collectors.groupingBy(PeriodeMedUtbetalingsgrad::getPeriode));

        DatoIntervallEntitet førstePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, delvisTilrettelegging.minusDays(1));
        DatoIntervallEntitet andrePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(delvisTilrettelegging, slutteArbeid.minusDays(1));
        DatoIntervallEntitet tredjePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(slutteArbeid, terminDatoMinus3UkerOg1Dag);

        // Assert
        assertThat(resultat.get(førstePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(resultat.get(andrePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(44));
        assertThat(resultat.get(tredjePeriode).get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }


    private AktivitetsAvtale lagAktivitetsAvtale(LocalDate jordmorsdato, LocalDate terminDato, BigDecimal prosentsats) {
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(jordmorsdato, terminDato));
        aktivitetsAvtaleBuilder.medProsentsats(prosentsats);
        return aktivitetsAvtaleBuilder.build();
    }

    private AktivitetsAvtale lagAktivitetsAvtale(DatoIntervallEntitet periode, BigDecimal prosentsats) {
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder();
        aktivitetsAvtaleBuilder.medPeriode(periode);
        aktivitetsAvtaleBuilder.medProsentsats(prosentsats);
        return aktivitetsAvtaleBuilder.build();
    }


    private TilretteleggingFOM ingenTilrettelegging(LocalDate slutteArbeid) {
        return new TilretteleggingFOM.Builder().medTilretteleggingType(TilretteleggingType.INGEN_TILRETTELEGGING).medFomDato(slutteArbeid).build();
    }

    private TilretteleggingFOM delvisTilrettelegging(LocalDate delvisTilrettelegging, BigDecimal stillingsprosent) {
        return new TilretteleggingFOM.Builder().medTilretteleggingType(TilretteleggingType.DELVIS_TILRETTELEGGING).medFomDato(delvisTilrettelegging).medStillingsprosent(stillingsprosent).build();
    }

    private TilretteleggingFOM helTilrettelegging(LocalDate helTilrettelegging) {
        return new TilretteleggingFOM.Builder()
            .medTilretteleggingType(TilretteleggingType.HEL_TILRETTELEGGING)
            .medFomDato(helTilrettelegging)
            .build();
    }
}
