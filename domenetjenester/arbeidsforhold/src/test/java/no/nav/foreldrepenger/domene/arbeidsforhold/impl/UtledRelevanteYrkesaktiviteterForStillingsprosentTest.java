package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

public class UtledRelevanteYrkesaktiviteterForStillingsprosentTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_returnere_en_tom_liste_når_ingen_yrkesaktiviteter_blir_sendt_inn() {
        // Arrange
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.emptyList();
        // Act
        var relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent
                .utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(relevanteYrkesaktiviteter).isEmpty();
    }

    @Test
    public void skal_returnere_en_liste_med_yrkesaktiviteter_som_overlapper_stp_og_filtrere_uten_resten() {

        // Arrange
        var fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom1 = SKJÆRINGSTIDSPUNKT;
        var aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                .medProsentsats(BigDecimal.valueOf(100));
        var ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("1"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale1)
                .leggTilAktivitetsAvtale(ansettelsesperiode1)
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(25));
        var ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("2"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale2)
                .leggTilAktivitetsAvtale(ansettelsesperiode2)
                .build();

        var fom3 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom3 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var aktivitetsavtale3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
                .medProsentsats(BigDecimal.valueOf(50));
        var ansettelsesperiode3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));
        var yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("3"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale3)
                .leggTilAktivitetsAvtale(ansettelsesperiode3)
                .build();

        var fom4 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom4 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        var aktivitetsavtale4 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4))
                .medProsentsats(BigDecimal.valueOf(50));
        var ansettelsesperiode4 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4));
        var yrkesaktivitet4 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("4"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale4)
                .leggTilAktivitetsAvtale(ansettelsesperiode4)
                .build();

        var yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        var relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent
                .utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(relevanteYrkesaktiviteter).hasSize(2);
        assertThat(relevanteYrkesaktiviteter).contains(yrkesaktivitet1);
        assertThat(relevanteYrkesaktiviteter).contains(yrkesaktivitet2);
        assertThat(relevanteYrkesaktiviteter).doesNotContain(yrkesaktivitet3);
        assertThat(relevanteYrkesaktiviteter).doesNotContain(yrkesaktivitet4);

    }

    @Test
    public void skal_returnere_en_liste_med_yrkesaktiviteter_som_tilkommer_etter_stp_når_ingen_yrkesaktivtet_overlapper_stp() {

        // Arrange
        var fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom1 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        var aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                .medProsentsats(BigDecimal.valueOf(100));
        var ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("1"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale1)
                .leggTilAktivitetsAvtale(ansettelsesperiode1)
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        var aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(25));
        var ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("2"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale2)
                .leggTilAktivitetsAvtale(ansettelsesperiode2)
                .build();

        var fom3 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        var tom3 = SKJÆRINGSTIDSPUNKT.plusYears(2);
        var aktivitetsavtale3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
                .medProsentsats(BigDecimal.valueOf(50));
        var ansettelsesperiode3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));
        var yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(Arbeidsgiver.virksomhet("3"))
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale3)
                .leggTilAktivitetsAvtale(ansettelsesperiode3)
                .build();

        var yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3);

        // Act
        var relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent
                .utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(relevanteYrkesaktiviteter).hasSize(2);
        assertThat(relevanteYrkesaktiviteter).doesNotContain(yrkesaktivitet1);
        assertThat(relevanteYrkesaktiviteter).contains(yrkesaktivitet2);
        assertThat(relevanteYrkesaktiviteter).contains(yrkesaktivitet3);

    }

    @Test
    public void skal_returnere_en_liste_med_relevante_yrkesaktiviteter_som_har_en_overstyrt_periode_eller_overlapper_stp() {

        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        var behandling = scenario.lagMocked();

        var fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom1 = Tid.TIDENES_ENDE;
        var arbeidsgiver1 = Arbeidsgiver.virksomhet("1");
        var ref1 = InternArbeidsforholdRef.nyRef();
        var aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
                .medProsentsats(BigDecimal.valueOf(100));
        var ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver1)
                .medArbeidsforholdId(ref1)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale1)
                .leggTilAktivitetsAvtale(ansettelsesperiode1)
                .build();

        var fom2 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        var tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("1");
        var ref2 = InternArbeidsforholdRef.nyRef();
        var aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
                .medProsentsats(BigDecimal.valueOf(100));
        var ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver2)
                .medArbeidsforholdId(ref2)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsavtale2)
                .leggTilAktivitetsAvtale(ansettelsesperiode2)
                .build();

        var aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
                .oppdatere(Optional.empty())
                .medAktørId(behandling.getAktørId())
                .leggTilYrkesaktivitet(yrkesaktivitet1)
                .leggTilYrkesaktivitet(yrkesaktivitet2);

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver1, ref1)
                .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
                .leggTilOverstyrtPeriode(fom1, SKJÆRINGSTIDSPUNKT.minusDays(1));
        informasjonBuilder.leggTil(overstyringBuilder);

        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId()))
                .før(SKJÆRINGSTIDSPUNKT);
        var yrkesaktiviteter = filter.getYrkesaktiviteter();

        // Act
        var relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent.utled(filter, yrkesaktiviteter,
                SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(relevanteYrkesaktiviteter).hasSize(2);
        assertThat(relevanteYrkesaktiviteter).anySatisfy(ya -> {
            assertThat(filter.getAnsettelsesPerioder(ya).get(0).getPeriode().getFomDato()).isEqualTo(fom1);
            assertThat(filter.getAnsettelsesPerioder(ya).get(0).getPeriode().getTomDato()).isEqualTo(SKJÆRINGSTIDSPUNKT.minusDays(1));
        });
        assertThat(relevanteYrkesaktiviteter).anySatisfy(ya -> {
            assertThat(filter.getAnsettelsesPerioder(ya).get(0).getPeriode().getFomDato()).isEqualTo(fom2);
            assertThat(filter.getAnsettelsesPerioder(ya).get(0).getPeriode().getTomDato()).isEqualTo(tom2);
        });
    }

    private InntektArbeidYtelseGrunnlag lagGrunnlag(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder,
            Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt) {
        var inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), VersjonType.REGISTER)
                .leggTilAktørArbeid(aktørArbeidBuilder);
        var inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.nytt()
                .medData(inntektArbeidYtelseAggregatBuilder);
        arbeidsforholdInformasjonOpt.ifPresent(inntektArbeidYtelseGrunnlagBuilder::medInformasjon);
        return inntektArbeidYtelseGrunnlagBuilder.build();
    }

}
