package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

public class UtledAnsettelsesperiodeTest {

    private static final LocalDate SKJÆRINGDSTIDSPUNKT = LocalDate.now();

    @Test
    public void skal_finne_korrekt_periode_for_yrkesaktiviteter_med_flere_ansettelsesperioder_hvor_en_overlapper_stp() {

        // Arrange
        LocalDate fom1 = SKJÆRINGDSTIDSPUNKT.minusYears(2);
        LocalDate tom1 = SKJÆRINGDSTIDSPUNKT.minusYears(1).minusDays(1);
        AktivitetsAvtaleBuilder aa1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));

        LocalDate fom2 = SKJÆRINGDSTIDSPUNKT.minusYears(1);
        LocalDate tom2 = SKJÆRINGDSTIDSPUNKT.plusYears(2).minusDays(1);
        AktivitetsAvtaleBuilder aa2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));

        LocalDate fom3 = SKJÆRINGDSTIDSPUNKT.minusYears(1);
        LocalDate tom3 = SKJÆRINGDSTIDSPUNKT.plusYears(2).minusDays(1);
        AktivitetsAvtaleBuilder aa3 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));

        Yrkesaktivitet yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(aa1)
                .leggTilAktivitetsAvtale(aa2)
                .leggTilAktivitetsAvtale(aa3)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();

        // Act
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet),
                yrkesaktivitet, SKJÆRINGDSTIDSPUNKT, false);

        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.minusYears(1));
            assertThat(ap.getTomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.plusYears(2).minusDays(1));
        });

    }

    @Test
    public void skal_finne_korrekt_periode_for_yrkesaktiviteter_med_flere_ansettelsesperioder_hvor_ingen_overlapper_stp() {

        // Arrange
        LocalDate fom1 = SKJÆRINGDSTIDSPUNKT.plusDays(1);
        LocalDate tom1 = SKJÆRINGDSTIDSPUNKT.plusYears(1).minusDays(1);
        AktivitetsAvtaleBuilder aa1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));

        LocalDate fom2 = SKJÆRINGDSTIDSPUNKT.plusYears(1);
        LocalDate tom2 = SKJÆRINGDSTIDSPUNKT.plusYears(2).minusDays(1);
        AktivitetsAvtaleBuilder aa2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));

        LocalDate fom3 = SKJÆRINGDSTIDSPUNKT.plusYears(2);
        LocalDate tom3 = Tid.TIDENES_ENDE;
        AktivitetsAvtaleBuilder aa3 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));

        Yrkesaktivitet yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(aa1)
                .leggTilAktivitetsAvtale(aa2)
                .leggTilAktivitetsAvtale(aa3)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();

        // Act
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet),
                yrkesaktivitet, SKJÆRINGDSTIDSPUNKT, false);

        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.plusDays(1));
            assertThat(ap.getTomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.plusYears(1).minusDays(1));
        });

    }

    @Test
    public void skal_finne_ansettelsesperiode_som_overlapper_stp_med_kun_en_yrkesaktivtet() {
        // Arrange
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE));
        Yrkesaktivitet yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        // Act
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(new YrkesaktivitetFilter(Optional.empty(), yrkesaktivitet),
                yrkesaktivitet, SKJÆRINGDSTIDSPUNKT, false);
        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.minusYears(1));
            assertThat(ap.getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
        });
    }

    @Test
    public void skal_finne_ansettelsesperiode_som_overlapper_stp_og_har_aktivitetsavtale_med_fom_dato_nærmest_stp() {
        // Arrange
        Yrkesaktivitet ya1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(2), SKJÆRINGDSTIDSPUNKT)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        Yrkesaktivitet ya2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(1), Tid.TIDENES_ENDE)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        Yrkesaktivitet ya3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.plusDays(1), SKJÆRINGDSTIDSPUNKT.plusYears(1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        // Act
        var yrkesaktiviteter = List.of(ya1, ya2, ya3);
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter),
                yrkesaktiviteter, SKJÆRINGDSTIDSPUNKT, false);
        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.minusYears(1));
            assertThat(ap.getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
        });
    }

    @Test
    public void skal_finne_ansettelsesperiode_som_for_aktivitetsavtale_som_tilkommer_etter_stp_når_ingen_overlapper() {
        // Arrange
        Yrkesaktivitet ya1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(2), SKJÆRINGDSTIDSPUNKT.minusDays(1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        Yrkesaktivitet ya2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(1), SKJÆRINGDSTIDSPUNKT.minusDays(1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        Yrkesaktivitet ya3 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.plusDays(1), SKJÆRINGDSTIDSPUNKT.plusYears(1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        // Act
        var yrkesaktiviteter = List.of(ya1, ya2, ya3);
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter),
                yrkesaktiviteter, SKJÆRINGDSTIDSPUNKT, false);
        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.plusDays(1));
            assertThat(ap.getTomDato()).isEqualTo(SKJÆRINGDSTIDSPUNKT.plusYears(1));
        });
    }

    @Test
    public void skal_finne_ikke_ansettelsesperiode_når_ingen_aktivitetsavtale_overlapper_eller_tilkommer_etter_stp() {
        // Arrange
        Yrkesaktivitet ya1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(2), SKJÆRINGDSTIDSPUNKT.minusDays(1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        Yrkesaktivitet ya2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGDSTIDSPUNKT.minusYears(1), SKJÆRINGDSTIDSPUNKT.minusDays(1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        // Act
        var yrkesaktiviteter = List.of(ya1, ya2);
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(new YrkesaktivitetFilter(null, yrkesaktiviteter),
                yrkesaktiviteter, SKJÆRINGDSTIDSPUNKT, false);
        // Assert
        assertThat(ansettelsesperiode).isEmpty();
    }

    @Test
    public void skal_finne_overstyrt_ansettelsesperiode_når_perioden_overlapper_stp() {
        // Arrange

        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagMocked();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate overstyrtTom = SKJÆRINGDSTIDSPUNKT.plusDays(1);
        LocalDate aaFom = SKJÆRINGDSTIDSPUNKT.minusYears(1);
        LocalDate aaTom = Tid.TIDENES_ENDE;

        ArbeidsforholdInformasjonBuilder informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
        overstyringBuilder.leggTilOverstyrtPeriode(aaFom, overstyrtTom);
        informasjonBuilder.leggTil(overstyringBuilder);

        AktivitetsAvtaleBuilder aaBuilder = lagAktivitetsAvtaleBuilder(aaFom, aaTom);
        Yrkesaktivitet ya = byggYrkesaktivitet(arbeidsgiver, ref, aaBuilder);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = lagAktørArbeidBuilder(behandling.getAktørId(), List.of(ya));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId()))
                .før(SKJÆRINGDSTIDSPUNKT);
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        // Act
        Optional<DatoIntervallEntitet> ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, SKJÆRINGDSTIDSPUNKT, true);

        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(aaFom);
            assertThat(ap.getTomDato()).isEqualTo(overstyrtTom);
        });

    }

    @Test
    public void skal_finne_overstyrt_ansettelsesperiode_når_perioden_har_tom_dato_før_stp() {
        // Arrange

        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        Behandling behandling = scenario.lagMocked();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("1");
        InternArbeidsforholdRef ref = InternArbeidsforholdRef.nyRef();
        LocalDate overstyrtTom = SKJÆRINGDSTIDSPUNKT.minusYears(1);
        LocalDate aaFom = SKJÆRINGDSTIDSPUNKT.minusYears(1);
        LocalDate aaTom = Tid.TIDENES_ENDE;

        var informasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ref);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
        overstyringBuilder.leggTilOverstyrtPeriode(aaFom, overstyrtTom);
        informasjonBuilder.leggTil(overstyringBuilder);

        var aaBuilder = lagAktivitetsAvtaleBuilder(aaFom, aaTom);
        Yrkesaktivitet ya = byggYrkesaktivitet(arbeidsgiver, ref, aaBuilder);
        var aktørArbeidBuilder = lagAktørArbeidBuilder(behandling.getAktørId(), List.of(ya));
        var grunnlag = lagGrunnlag(aktørArbeidBuilder, Optional.of(informasjonBuilder.build()));

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(behandling.getAktørId()))
                .før(SKJÆRINGDSTIDSPUNKT);
        var yrkesaktiviteter = filter.getYrkesaktiviteter();

        // Act
        var ansettelsesperiode = UtledAnsettelsesperiode.utled(filter, yrkesaktiviteter, SKJÆRINGDSTIDSPUNKT, true);

        // Assert
        assertThat(ansettelsesperiode).hasValueSatisfying(ap -> {
            assertThat(ap.getFomDato()).isEqualTo(aaFom);
            assertThat(ap.getTomDato()).isEqualTo(overstyrtTom);
        });

    }

    private Yrkesaktivitet byggYrkesaktivitet(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref, AktivitetsAvtaleBuilder aaBuilder) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(ref)
                .leggTilAktivitetsAvtale(aaBuilder)
                .build();
    }

    private AktivitetsAvtaleBuilder lagAktivitetsAvtaleBuilder(LocalDate aaFom, LocalDate aaTom) {
        return AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aaFom, aaTom));
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

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder lagAktørArbeidBuilder(AktørId aktørId, List<Yrkesaktivitet> yrkesaktiviteter) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder
                .oppdatere(Optional.empty());
        aktørArbeidBuilder.medAktørId(aktørId);
        yrkesaktiviteter.forEach(aktørArbeidBuilder::leggTilYrkesaktivitet);
        return aktørArbeidBuilder;
    }

}
