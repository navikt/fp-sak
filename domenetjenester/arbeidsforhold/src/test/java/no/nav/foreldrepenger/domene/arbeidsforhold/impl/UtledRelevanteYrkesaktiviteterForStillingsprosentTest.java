package no.nav.foreldrepenger.domene.arbeidsforhold.impl;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
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
        List<Yrkesaktivitet> relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);
        // Assert
        assertThat(relevanteYrkesaktiviteter).isEmpty();
    }

    @Test
    public void skal_returnere_en_liste_med_yrkesaktiviteter_som_overlapper_stp_og_filtrere_uten_resten() {

        // Arrange
        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom1 = SKJÆRINGSTIDSPUNKT;
        AktivitetsAvtaleBuilder aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("1"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        AktivitetsAvtaleBuilder aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
            .medProsentsats(BigDecimal.valueOf(25));
        AktivitetsAvtaleBuilder ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("2"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .build();

        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        AktivitetsAvtaleBuilder aktivitetsavtale3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
            .medProsentsats(BigDecimal.valueOf(50));
        AktivitetsAvtaleBuilder ansettelsesperiode3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));
        Yrkesaktivitet yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("3"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale3)
            .leggTilAktivitetsAvtale(ansettelsesperiode3)
            .build();

        LocalDate fom4 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom4 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        AktivitetsAvtaleBuilder aktivitetsavtale4 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4))
            .medProsentsats(BigDecimal.valueOf(50));
        AktivitetsAvtaleBuilder ansettelsesperiode4 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4));
        Yrkesaktivitet yrkesaktivitet4 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("4"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale4)
            .leggTilAktivitetsAvtale(ansettelsesperiode4)
            .build();

        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3, yrkesaktivitet4);

        // Act
        List<Yrkesaktivitet> relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

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
        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom1 = SKJÆRINGSTIDSPUNKT.minusDays(1);
        AktivitetsAvtaleBuilder aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("1"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusYears(1);
        AktivitetsAvtaleBuilder aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
            .medProsentsats(BigDecimal.valueOf(25));
        AktivitetsAvtaleBuilder ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("2"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .build();

        LocalDate fom3 = SKJÆRINGSTIDSPUNKT.plusDays(2);
        LocalDate tom3 = SKJÆRINGSTIDSPUNKT.plusYears(2);
        AktivitetsAvtaleBuilder aktivitetsavtale3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3))
            .medProsentsats(BigDecimal.valueOf(50));
        AktivitetsAvtaleBuilder ansettelsesperiode3 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));
        Yrkesaktivitet yrkesaktivitet3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(Arbeidsgiver.virksomhet("3"))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale3)
            .leggTilAktivitetsAvtale(ansettelsesperiode3)
            .build();

        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesaktivitet1, yrkesaktivitet2, yrkesaktivitet3);

        // Act
        List<Yrkesaktivitet> relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter), yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(relevanteYrkesaktiviteter).hasSize(2);
        assertThat(relevanteYrkesaktiviteter).doesNotContain(yrkesaktivitet1);
        assertThat(relevanteYrkesaktiviteter).contains(yrkesaktivitet2);
        assertThat(relevanteYrkesaktiviteter).contains(yrkesaktivitet3);

    }

    @Test
    public void skal_returnere_en_liste_med_relevante_yrkesaktiviteter_som_har_en_overstyrt_periode_eller_overlapper_stp() {

        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagMocked();

        LocalDate fom1 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom1 = Tid.TIDENES_ENDE;
        Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref1 = InternArbeidsforholdRef.nyRef();
        AktivitetsAvtaleBuilder aktivitetsavtale1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode1 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));
        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver1)
            .medArbeidsforholdId(ref1)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .build();

        LocalDate fom2 = SKJÆRINGSTIDSPUNKT.minusYears(2);
        LocalDate tom2 = SKJÆRINGSTIDSPUNKT.plusDays(1);
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref2 = InternArbeidsforholdRef.nyRef();
        AktivitetsAvtaleBuilder aktivitetsavtale2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
            .medProsentsats(BigDecimal.valueOf(100));
        AktivitetsAvtaleBuilder ansettelsesperiode2 = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidsgiver(arbeidsgiver2)
            .medArbeidsforholdId(ref2)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsavtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .build();

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
            .medAktørId(behandling.getAktørId())
            .leggTilYrkesaktivitet(yrkesaktivitet1)
            .leggTilYrkesaktivitet(yrkesaktivitet2);

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver1, ref1)
            .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
            .leggTilOverstyrtPeriode(fom1, SKJÆRINGSTIDSPUNKT.minusDays(1));
        informasjonBuilder.leggTil(overstyringBuilder);

        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId())).før(SKJÆRINGSTIDSPUNKT);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        // Act
        List<Yrkesaktivitet> relevanteYrkesaktiviteter = UtledRelevanteYrkesaktiviteterForStillingsprosent.utled(filter, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

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
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(aktørArbeidBuilder);
        InntektArbeidYtelseGrunnlagBuilder inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(inntektArbeidYtelseAggregatBuilder);
        arbeidsforholdInformasjonOpt.ifPresent(inntektArbeidYtelseGrunnlagBuilder::medInformasjon);
        return inntektArbeidYtelseGrunnlagBuilder.build();
    }


}
