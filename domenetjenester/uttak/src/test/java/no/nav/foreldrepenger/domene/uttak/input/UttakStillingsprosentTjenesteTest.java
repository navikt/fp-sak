package no.nav.foreldrepenger.domene.uttak.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class UttakStillingsprosentTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    void medOrgNrOgArbIdEnYrkesaktivitet() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent = BigDecimal.valueOf(77);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId = InternArbeidsforholdRef.nyRef();
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var yrkesAktivitet = arbeidAktivitet(arbeidsgiver, arbId, fom, tom, stillingsprosent);

        var yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        var grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, fom)).isEqualTo(stillingsprosent);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, tom)).isEqualTo(stillingsprosent);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, tom.plusDays(1))).isZero();
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, fom.minusDays(1))).isZero();
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(null, arbId, fom)).isZero();
    }

    @Test
    void medToArbeidsforholdSammeArbeidsgiverSammePeriode() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent1 = BigDecimal.valueOf(77);
        var stillingsprosent2 = BigDecimal.valueOf(50);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var yrkesAktivitet1 = arbeidAktivitet(arbeidsgiver, arbId1, fom, tom, stillingsprosent1);
        var yrkesAktivitet2 = arbeidAktivitet(arbeidsgiver, arbId2, fom, tom, stillingsprosent2);
        var yrkesAktivitetBuilder = List.of(yrkesAktivitet1, yrkesAktivitet2);
        var yrkesaktiviteter = List.of(yrkesAktivitet1.build(), yrkesAktivitet2.build());

        var grunnlag = opprettGrunnlag(yrkesAktivitetBuilder, behandling.getAktørId()).build();
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId1, fom))
            .isEqualTo(stillingsprosent1);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId2, fom))
            .isEqualTo(stillingsprosent2);
    }

    @Test
    void medToArbeidsforholdSammeArbeidsgiverUlikPeriode() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent1 = BigDecimal.valueOf(77);
        var stillingsprosent2 = BigDecimal.valueOf(50);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var fom1 = LocalDate.now();
        var tom1 = fom1.plusWeeks(2);
        var fom2 = tom1.plusDays(1);
        var tom2 = fom2.plusWeeks(2);
        var yrkesAktivitet1 = arbeidAktivitet(arbeidsgiver, arbId1, fom1, tom1, stillingsprosent1);
        var yrkesAktivitet2 = arbeidAktivitet(arbeidsgiver, arbId2, fom2, tom2, stillingsprosent2);
        var yrkesaktivitetBuilder = List.of(yrkesAktivitet1, yrkesAktivitet2);
        var yrkesaktiviteter = List.of(yrkesAktivitet1.build(), yrkesAktivitet2.build());

        var grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId1, fom1))
            .isEqualTo(stillingsprosent1);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId2, fom2))
            .isEqualTo(stillingsprosent2);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId1, fom2)).isZero();
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId2, fom1)).isZero();
    }

    @Test
    void skalMatcheSelvOmArbeidsgiverErNullMenYtelseHarArbeidsgiverRef() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent = BigDecimal.valueOf(77);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId = InternArbeidsforholdRef.nyRef();
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var yrkesAktivitet = arbeidAktivitet(arbeidsgiver, arbId, fom, tom, stillingsprosent);

        var yrkesAktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        var grunnlag = opprettGrunnlag(yrkesAktivitetBuilder, behandling.getAktørId()).build();
        var yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, fom)).isEqualTo(stillingsprosent);
    }

    @Test
    void skalTaStillingsprosentFraFørsteAktivitetsavtaleIAnsettelsesperiodenHvisDetManglerAktivitetsavtaleFraStartenAvAnsettelsesperioden() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosentFørsteAktivitetsavtale = BigDecimal.valueOf(55);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var førsteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom.minusDays(3)))
            .medProsentsats(stillingsprosentFørsteAktivitetsavtale);
        var sisteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tom.minusDays(2), tom))
            .medProsentsats(BigDecimal.TEN);

        var ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        var yrkesAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(førsteAktivitetsavtale)
            .leggTilAktivitetsAvtale(sisteAktivitetsavtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);

        var yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        var grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, fom.plusDays(2)))
            .isEqualTo(stillingsprosentFørsteAktivitetsavtale);
    }

    @Test
    void skalTaStillingsprosentFraSisteAktivitetsavtaleIAnsettelsesperiodenHvisDetManglerAktivitetsavtaleFraSluttenAvAnsettelsesperioden() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosentSisteAktivitetsavtale = BigDecimal.valueOf(55);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var førsteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))
            .medProsentsats(BigDecimal.TEN);
        var sisteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(4), tom.minusWeeks(1)))
            .medProsentsats(stillingsprosentSisteAktivitetsavtale);

        var ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        var yrkesAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(førsteAktivitetsavtale)
            .leggTilAktivitetsAvtale(sisteAktivitetsavtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver);

        var yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        var grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, tom.minusDays(2)))
            .isEqualTo(stillingsprosentSisteAktivitetsavtale);
    }

    @Test
    void skalBareSePåAktivitetsavtalerSomLiggerIEnAnsettelsePeriodeSomMatcherDato() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent1 = BigDecimal.valueOf(40);
        var stillingsprosent2 = BigDecimal.valueOf(50);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);

        var aktivitetAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent1);
        var ansettelsesperiode1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var yrkesAktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetAvtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbId1);

        var aktivitetAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent2);
        var ansettelsesperiode2 = AktivitetsAvtaleBuilder.ny()
            // Gjør at aktivitetsavtale ligger utenfor ansettelsesperioden
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom));
        var yrkesAktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetAvtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbId2);

        var yrkesAktivitetBuilder = List.of(yrkesAktivitet1, yrkesAktivitet2);
        var yrkesaktiviteter = List.of(yrkesAktivitet1.build(), yrkesAktivitet2.build());

        var grunnlag = opprettGrunnlag(yrkesAktivitetBuilder, behandling.getAktørId()).build();
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, null, fom))
            .isEqualTo(stillingsprosent1);
    }

    @Test
    void medYrkesaktivitetProsentSomErOver100() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent = BigDecimal.valueOf(500);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId = InternArbeidsforholdRef.nyRef();
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var yrkesAktivitet = arbeidAktivitet(arbeidsgiver, arbId, fom, tom, stillingsprosent);

        var yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        var grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, fom))
            .isEqualTo(stillingsprosent);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, tom))
            .isEqualTo(stillingsprosent);
    }

    @Test
    void medYrkesaktivitetProsentSomErUnder0() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        var stillingsprosent = BigDecimal.valueOf(-100);

        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var arbId = InternArbeidsforholdRef.nyRef();
        var fom = LocalDate.now();
        var tom = fom.plusWeeks(2);
        var yrkesAktivitet = arbeidAktivitet(arbeidsgiver, arbId, fom, tom, stillingsprosent);

        var yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        var grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, fom))
            .isEqualTo(stillingsprosent.abs());
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(arbeidsgiver, arbId, tom))
            .isEqualTo(stillingsprosent.abs());
    }

    private UttakYrkesaktiviteter tjeneste(Behandling behandling, InntektArbeidYtelseGrunnlag grunnlag, List<Yrkesaktivitet> yrkesaktiviteter) {
        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        var input = new UttakInput(ref, stp, grunnlag, null);
        var bgStatuser = yrkesaktiviteter.stream().map((Yrkesaktivitet y) -> {
            var arbeidsforholdRef = y.getArbeidsforholdRef();
            var arbeidsgiver = y.getArbeidsgiver();
            return new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef);
        }).collect(Collectors.toSet());

        return input.medBeregningsgrunnlagStatuser(bgStatuser).getYrkesaktiviteter();
    }

    private YrkesaktivitetBuilder arbeidAktivitet(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbId, LocalDate fom, LocalDate tom, BigDecimal stillingsprosent) {

        var aktivitetAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent);

        var ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(arbId);
    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlag(List<YrkesaktivitetBuilder> yrkesaktivitetList, AktørId aktørId) {
        var aggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = aggregat.getAktørArbeidBuilder(aktørId);
        for (var yrkesaktivitet : yrkesaktivitetList) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet);
        }

        aggregat.leggTilAktørArbeid(aktørArbeidBuilder);

        var inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty());
        inntektArbeidYtelseGrunnlagBuilder.medData(aggregat);
        return inntektArbeidYtelseGrunnlagBuilder;
    }
}
