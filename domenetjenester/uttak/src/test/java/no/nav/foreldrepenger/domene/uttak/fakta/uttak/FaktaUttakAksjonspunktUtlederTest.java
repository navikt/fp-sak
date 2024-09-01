package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.konfig.Tid;

class FaktaUttakAksjonspunktUtlederTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final FaktaUttakAksjonspunktUtleder utleder = new FaktaUttakAksjonspunktUtleder(new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));

    @Test
    void ap_hvis_avklart_første_uttaksdag_forskjellig_fra_startdato_fordeling() {
        var avklartDato = LocalDate.now();
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(avklartDato.plusWeeks(2), avklartDato.plusWeeks(10))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(periode), true))
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(avklartDato).build());
        var behandling = scenario.lagre(repositoryProvider);

        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, null);
        var ap = utleder.utledAksjonspunkterFor(input);

        assertThat(ap).hasSize(1);
        assertThat(ap.get(0)).isEqualTo(AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO);
    }

    @Test
    void ap_hvis_ingen_perioder() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(), true));
        var behandling = scenario.lagre(repositoryProvider);

        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, null);
        var ap = utleder.utledAksjonspunkterFor(input);

        assertThat(ap).hasSize(1);
        assertThat(ap.get(0)).isEqualTo(AksjonspunktDefinisjon.FAKTA_UTTAK_INGEN_PERIODER);
    }

    @Test
    void ap_hvis_gradering_ukjent_aktivitet() {
        var gradering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(10))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(gradering), true));
        var behandling = scenario.lagre(repositoryProvider);

        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, null)
            .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, Arbeidsgiver.virksomhet("000000000"), null)));
        var ap = utleder.utledAksjonspunkterFor(input);

        assertThat(ap).hasSize(1);
        assertThat(ap.get(0)).isEqualTo(AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
    }

    @Test
    void ap_hvis_gradering_aktivitet_uten_beregningsgrunnlag() {
        var gradering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(10))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(gradering), true));
        var behandling = scenario.lagre(repositoryProvider);

        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, null)
            .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.FRILANSER, null, null)))
            .medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(true);
        var ap = utleder.utledAksjonspunkterFor(input);

        assertThat(ap).hasSize(1);
        assertThat(ap.get(0)).isEqualTo(AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG);
    }

    @Test
    void ap_hvis_gradering_uten_arbeidsforhold_aareg() {
        var arbeidsgiverIAY = Arbeidsgiver.virksomhet("999999999");
        var arbeidsgiverGradering = Arbeidsgiver.virksomhet("999999998");
        var gradering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(10))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medArbeidsgiver(arbeidsgiverGradering)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .build();


        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(gradering), true));
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingRef = BehandlingReferanse.fra(behandling);

        var aggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = aggregat.getAktørArbeidBuilder(behandlingRef.aktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(lagYrkesAkiviteter(arbeidsgiverIAY));
        aggregat.leggTilAktørArbeid(aktørArbeidBuilder);

        var inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty());
        inntektArbeidYtelseGrunnlagBuilder.medData(aggregat);

        var input = new UttakInput(behandlingRef, null, inntektArbeidYtelseGrunnlagBuilder.build(), null)
            .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, arbeidsgiverIAY, null)))
            .medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(false);
        var ap = utleder.utledAksjonspunkterFor(input);

        assertThat(ap).hasSize(1);
        assertThat(ap.get(0)).isEqualTo(AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
    }

    private Yrkesaktivitet lagYrkesAkiviteter(Arbeidsgiver arbeidsgiver) {

        var fom = LocalDate.of(2015, 8, 1);
        var tom = Tid.TIDENES_ENDE;

        var aa1 = AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var aa1_2 = AktivitetsAvtaleBuilder.ny().medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)).medProsentsats(new BigDecimal(20));

        var ya1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aa1)
            .leggTilAktivitetsAvtale(aa1_2)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef());
        return ya1.build();

    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlag(List<YrkesaktivitetBuilder> yrkesaktivitetList,
                                                               AktørId aktørId,
                                                               ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        var aggregat = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørArbeidBuilder = aggregat.getAktørArbeidBuilder(aktørId);
        for (var yrkesaktivitet : yrkesaktivitetList) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitet);
        }
        aggregat.leggTilAktørArbeid(aktørArbeidBuilder);

        var inntektArbeidYtelseGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(Optional.empty());
        inntektArbeidYtelseGrunnlagBuilder.medInformasjon(arbeidsforholdInformasjon);
        inntektArbeidYtelseGrunnlagBuilder.medData(aggregat);
        return inntektArbeidYtelseGrunnlagBuilder;
    }

}
