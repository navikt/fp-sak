package no.nav.foreldrepenger.domene.uttak.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
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

public class UttakStillingsprosentTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());

    @Test
    public void medOrgNrOgArbIdEnYrkesaktivitet() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent = BigDecimal.valueOf(77);

        String orgnr = "123";
        var arbId = InternArbeidsforholdRef.nyRef();
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        YrkesaktivitetBuilder yrkesAktivitet = arbeidAktivitet(orgnr, arbId, fom, tom, stillingsprosent);

        List<YrkesaktivitetBuilder> yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, fom))
            .isEqualTo(stillingsprosent);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, tom))
            .isEqualTo(stillingsprosent);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, tom.plusDays(1))).isZero();
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, fom.minusDays(1))).isZero();
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(null, arbId, fom)).isZero();
    }

    @Test
    public void medToArbeidsforholdSammeArbeidsgiverSammePeriode() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent1 = BigDecimal.valueOf(77);
        BigDecimal stillingsprosent2 = BigDecimal.valueOf(50);

        String orgnr = "123";
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        YrkesaktivitetBuilder yrkesAktivitet1 = arbeidAktivitet(orgnr, arbId1, fom, tom, stillingsprosent1);
        YrkesaktivitetBuilder yrkesAktivitet2 = arbeidAktivitet(orgnr, arbId2, fom, tom, stillingsprosent2);
        List<YrkesaktivitetBuilder> yrkesAktivitetBuilder = List.of(yrkesAktivitet1, yrkesAktivitet2);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesAktivitet1.build(), yrkesAktivitet2.build());

        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesAktivitetBuilder, behandling.getAktørId()).build();
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId1, fom))
            .isEqualTo(stillingsprosent1);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId2, fom))
            .isEqualTo(stillingsprosent2);
    }

    @Test
    public void medToArbeidsforholdSammeArbeidsgiverUlikPeriode() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent1 = BigDecimal.valueOf(77);
        BigDecimal stillingsprosent2 = BigDecimal.valueOf(50);

        String orgnr = "123";
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        LocalDate fom1 = LocalDate.now();
        LocalDate tom1 = fom1.plusWeeks(2);
        LocalDate fom2 = tom1.plusDays(1);
        LocalDate tom2 = fom2.plusWeeks(2);
        YrkesaktivitetBuilder yrkesAktivitet1 = arbeidAktivitet(orgnr, arbId1, fom1, tom1, stillingsprosent1);
        YrkesaktivitetBuilder yrkesAktivitet2 = arbeidAktivitet(orgnr, arbId2, fom2, tom2, stillingsprosent2);
        List<YrkesaktivitetBuilder> yrkesaktivitetBuilder = List.of(yrkesAktivitet1, yrkesAktivitet2);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesAktivitet1.build(), yrkesAktivitet2.build());

        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId1, fom1))
            .isEqualTo(stillingsprosent1);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId2, fom2))
            .isEqualTo(stillingsprosent2);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId1, fom2)).isZero();
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId2, fom1)).isZero();
    }

    @Test
    public void skalMatcheSelvOmArbeidsgiverErNullMenYtelseHarArbeidsgiverRef() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent = BigDecimal.valueOf(77);

        String orgnr = "123";
        var arbId = InternArbeidsforholdRef.nyRef();
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        YrkesaktivitetBuilder yrkesAktivitet = arbeidAktivitet(orgnr, arbId, fom, tom, stillingsprosent);

        List<YrkesaktivitetBuilder> yrkesAktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesAktivitetBuilder, behandling.getAktørId()).build();
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, null, fom)).isEqualTo(stillingsprosent);
    }

    @Test
    public void skalTaStillingsprosentFraFørsteAktivitetsavtaleIAnsettelsesperiodenHvisDetManglerAktivitetsavtaleFraStartenAvAnsettelsesperioden() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosentFørsteAktivitetsavtale = BigDecimal.valueOf(55);

        String orgnr = "123";
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        AktivitetsAvtaleBuilder førsteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom.minusDays(3)))
            .medProsentsats(stillingsprosentFørsteAktivitetsavtale);
        AktivitetsAvtaleBuilder sisteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(tom.minusDays(2), tom))
            .medProsentsats(BigDecimal.TEN);

        AktivitetsAvtaleBuilder ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        YrkesaktivitetBuilder yrkesAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(førsteAktivitetsavtale)
            .leggTilAktivitetsAvtale(sisteAktivitetsavtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr));

        List<YrkesaktivitetBuilder> yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, null, fom.plusDays(2)))
            .isEqualTo(stillingsprosentFørsteAktivitetsavtale);
    }

    @Test
    public void skalTaStillingsprosentFraSisteAktivitetsavtaleIAnsettelsesperiodenHvisDetManglerAktivitetsavtaleFraSluttenAvAnsettelsesperioden() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosentSisteAktivitetsavtale = BigDecimal.valueOf(55);

        String orgnr = "123";
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        AktivitetsAvtaleBuilder førsteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, fom.plusDays(3)))
            .medProsentsats(BigDecimal.TEN);
        AktivitetsAvtaleBuilder sisteAktivitetsavtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusDays(4), tom.minusWeeks(1)))
            .medProsentsats(stillingsprosentSisteAktivitetsavtale);

        AktivitetsAvtaleBuilder ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        YrkesaktivitetBuilder yrkesAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(førsteAktivitetsavtale)
            .leggTilAktivitetsAvtale(sisteAktivitetsavtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr));

        List<YrkesaktivitetBuilder> yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, null, tom.minusDays(2)))
            .isEqualTo(stillingsprosentSisteAktivitetsavtale);
    }

    @Test
    public void skalBareSePåAktivitetsavtalerSomLiggerIEnAnsettelsePeriodeSomMatcherDato() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent1 = BigDecimal.valueOf(40);
        BigDecimal stillingsprosent2 = BigDecimal.valueOf(50);

        String orgnr = "123";
        var arbId1 = InternArbeidsforholdRef.nyRef();
        var arbId2 = InternArbeidsforholdRef.nyRef();
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);

        AktivitetsAvtaleBuilder aktivitetAvtale1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent1);
        AktivitetsAvtaleBuilder ansettelsesperiode1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        YrkesaktivitetBuilder yrkesAktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetAvtale1)
            .leggTilAktivitetsAvtale(ansettelsesperiode1)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdId(arbId1);

        AktivitetsAvtaleBuilder aktivitetAvtale2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent2);
        AktivitetsAvtaleBuilder ansettelsesperiode2 = AktivitetsAvtaleBuilder.ny()
            // Gjør at aktivitetsavtale ligger utenfor ansettelsesperioden
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.plusWeeks(1), tom));
        YrkesaktivitetBuilder yrkesAktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetAvtale2)
            .leggTilAktivitetsAvtale(ansettelsesperiode2)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdId(arbId2);

        List<YrkesaktivitetBuilder> yrkesAktivitetBuilder = List.of(yrkesAktivitet1, yrkesAktivitet2);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(yrkesAktivitet1.build(), yrkesAktivitet2.build());

        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesAktivitetBuilder, behandling.getAktørId()).build();
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, null, fom))
            .isEqualTo(stillingsprosent1);
    }

    @Test
    public void medYrkesaktivitetProsentSomErOver100() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent = BigDecimal.valueOf(500);

        String orgnr = "123";
        var arbId = InternArbeidsforholdRef.nyRef();
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        YrkesaktivitetBuilder yrkesAktivitet = arbeidAktivitet(orgnr, arbId, fom, tom, stillingsprosent);

        List<YrkesaktivitetBuilder> yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, fom))
            .isEqualTo(stillingsprosent);
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, tom))
            .isEqualTo(stillingsprosent);
    }

    @Test
    public void medYrkesaktivitetProsentSomErUnder0() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        BigDecimal stillingsprosent = BigDecimal.valueOf(-100);

        String orgnr = "123";
        var arbId = InternArbeidsforholdRef.nyRef();
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusWeeks(2);
        YrkesaktivitetBuilder yrkesAktivitet = arbeidAktivitet(orgnr, arbId, fom, tom, stillingsprosent);

        List<YrkesaktivitetBuilder> yrkesaktivitetBuilder = Collections.singletonList(yrkesAktivitet);
        final InntektArbeidYtelseGrunnlag grunnlag = opprettGrunnlag(yrkesaktivitetBuilder, behandling.getAktørId()).build();
        List<Yrkesaktivitet> yrkesaktiviteter = Collections.singletonList(yrkesAktivitet.build());
        var tjeneste = tjeneste(behandling, grunnlag, yrkesaktiviteter);

        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, fom))
            .isEqualTo(stillingsprosent.abs());
        assertThat(tjeneste.finnStillingsprosentOrdinærtArbeid(orgnr, arbId, tom))
            .isEqualTo(stillingsprosent.abs());
    }

    private UttakYrkesaktiviteter tjeneste(Behandling behandling, InntektArbeidYtelseGrunnlag grunnlag, List<Yrkesaktivitet> yrkesaktiviteter) {
        var ref = BehandlingReferanse.fra(behandling, LocalDate.now());
        var input = new UttakInput(ref, grunnlag, null);
        var bgStatuser = yrkesaktiviteter.stream().map((Yrkesaktivitet y) -> {
            var arbeidsforholdRef = y.getArbeidsforholdRef();
            var arbeidsgiver = y.getArbeidsgiver();
            return new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef);
        }).collect(Collectors.toSet());

        return input.medBeregningsgrunnlagStatuser(bgStatuser).getYrkesaktiviteter();
    }

    private YrkesaktivitetBuilder arbeidAktivitet(String orgnr, InternArbeidsforholdRef arbId, LocalDate fom, LocalDate tom, BigDecimal stillingsprosent) {

        AktivitetsAvtaleBuilder aktivitetAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
            .medProsentsats(stillingsprosent);

        AktivitetsAvtaleBuilder ansettelsesperiode = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(aktivitetAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medArbeidsforholdId(arbId);
    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlag(List<YrkesaktivitetBuilder> yrkesaktivitetList, AktørId aktørId) {
        InntektArbeidYtelseAggregatBuilder aggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = aggregat.getAktørArbeidBuilder(aktørId);
        for (YrkesaktivitetBuilder yrkesaktivitet : yrkesaktivitetList) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet);
        }

        aggregat.leggTilAktørArbeid(aktørArbeidBuilder);

        InntektArbeidYtelseGrunnlagBuilder inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty());
        inntektArbeidYtelseGrunnlagBuilder.medData(aggregat);
        return inntektArbeidYtelseGrunnlagBuilder;
    }
}
