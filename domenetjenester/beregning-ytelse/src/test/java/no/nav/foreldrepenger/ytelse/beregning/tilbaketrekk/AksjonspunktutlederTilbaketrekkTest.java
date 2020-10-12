package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class AksjonspunktutlederTilbaketrekkTest {


    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusMonths(2);
    public static final String ORGNR1 = KUNSTIG_ORG + "1";
    public static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet(ORGNR1);
    public static final String ORGNR2 = KUNSTIG_ORG + "2";
    public static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet(ORGNR2);
    public static final int DAGSATS = 2134;
    public static final long ORIGINAL_BEHANDLING_ID = 83724923L;
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BeregningsresultatRepository beregningsresultatRepository = mock(BeregningsresultatRepository.class);
    private AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private Behandling behandling;
    private BehandlingReferanse behandlingReferanse = mock(BehandlingReferanse.class);

    @Before
    public void setUp() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        when(behandlingReferanse.getBehandlingId()).thenReturn(behandling.getId());
        when(behandlingReferanse.erRevurdering()).thenReturn(true);
        when(behandlingReferanse.getAktørId()).thenReturn(behandling.getAktørId());
        when(behandlingReferanse.getUtledetSkjæringstidspunkt()).thenReturn(SKJÆRINGSTIDSPUNKT);
        when(behandlingReferanse.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
        aksjonspunktutlederTilbaketrekk = new AksjonspunktutlederTilbaketrekk(new BeregningsresultatTidslinjetjeneste(beregningsresultatRepository), inntektArbeidYtelseTjeneste);
    }


    @Test
    public void skal_få_aksjonspunkt_for_arbeidsforhold_som_tilkommer_med_avsluttet_arbeidsforhold_i_ulike_virksomheter() {
        // Arrange
        lagUtbetaltBeregningsresultatMedEnAndelTilBruker(ARBEIDSGIVER1, SKJÆRINGSTIDSPUNKT);
        lagBeregningsresultatForTilkommetArbeidMedRefusjon(ARBEIDSGIVER2, ARBEIDSGIVER1, SKJÆRINGSTIDSPUNKT);
        lagIayForAvsluttetOgTilkommetArbeid();

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(behandlingReferanse));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
    }

    @Test
    public void skal_få_aksjonspunkt_for_arbeidsforhold_med_ansettelsesperioder_som_slutter_før_skjæringstidspunktet() {
        // Arrange
        // Bygg IAY
        InntektArbeidYtelseAggregatBuilder registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("977011833");
        InternArbeidsforholdRef arbeidsforholdId1 = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder ya1 = lagYrkesaktivitet(arbeidsgiver, arbeidsforholdId1, DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.minusMonths(3)));
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("924042648");
        YrkesaktivitetBuilder ya2 = lagYrkesaktivitet(arbeidsgiver2, InternArbeidsforholdRef.nullRef(), DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1)));
        InternArbeidsforholdRef arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        YrkesaktivitetBuilder ya3 = lagYrkesaktivitet(arbeidsgiver, arbeidsforholdId, DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1).minusDays(1)));
        leggTilYrkesaktiviteter(registerBuilder, ya1, ya2, ya3);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), registerBuilder);

        // ALLEREDE UTBETALT
        lagUtbetaltBeregningsresultatMedEnAndelTilBruker(arbeidsgiver, SKJÆRINGSTIDSPUNKT.plusMonths(1));
        // NYTT RESULTAT
        lagBeregningsresultatForTilkommetArbeidMedRefusjon(arbeidsgiver2, arbeidsgiver, SKJÆRINGSTIDSPUNKT.plusMonths(1));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(behandlingReferanse));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
    }

    private void leggTilYrkesaktiviteter(InntektArbeidYtelseAggregatBuilder registerBuilder, YrkesaktivitetBuilder... yas) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = registerBuilder.getAktørArbeidBuilder(behandling.getAktørId());
        for (YrkesaktivitetBuilder ya : yas) {
            aktørArbeidBuilder.leggTilYrkesaktivitet(ya);
        }
        registerBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private YrkesaktivitetBuilder lagYrkesaktivitet(Arbeidsgiver arbeidsgiver4, InternArbeidsforholdRef arbeidsforholdId, DatoIntervallEntitet periode) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(periode))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(BigDecimal.valueOf(100))
                .medPeriode(periode))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsgiver(arbeidsgiver4);
    }

    private void lagIayForAvsluttetOgTilkommetArbeid() {
        InntektArbeidYtelseAggregatBuilder registerBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = registerBuilder.getAktørArbeidBuilder(behandling.getAktørId());
        aktørArbeidBuilder.leggTilYrkesaktivitet(lagYrkesaktivitetForAvsluttetArbeid())
            .leggTilYrkesaktivitet(lagYrkesaktivitetForTilkommetArbeid());
        registerBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(), registerBuilder);
    }

    private YrkesaktivitetBuilder lagYrkesaktivitetForAvsluttetArbeid() {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2), SKJÆRINGSTIDSPUNKT.plusMonths(1))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(ARBEIDSGIVER1);
    }

    private YrkesaktivitetBuilder lagYrkesaktivitetForTilkommetArbeid() {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(ARBEIDSGIVER2);
    }

    private void lagUtbetaltBeregningsresultatMedEnAndelTilBruker(Arbeidsgiver arbeidsgiver, LocalDate beregningsresultatPeriodeFom) {
        BeregningsresultatEntitet build = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(beregningsresultatPeriodeFom, beregningsresultatPeriodeFom.plusMonths(2))
            .build(build);
        BeregningsresultatAndel.builder()
            .medDagsats(DAGSATS)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(DAGSATS)
            .build(periode);
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(build));
    }

    private void lagBeregningsresultatForTilkommetArbeidMedRefusjon(Arbeidsgiver tilkommetArbeid, Arbeidsgiver bortfaltArbeid, LocalDate beregningsresultatPeriodeFom) {
        BeregningsresultatEntitet build = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(beregningsresultatPeriodeFom, beregningsresultatPeriodeFom.plusMonths(2))
            .build(build);
        BeregningsresultatAndel.builder()
            .medDagsats(0)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(bortfaltArbeid)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(0)
            .build(periode);
        BeregningsresultatAndel.builder()
            .medDagsats(DAGSATS)
            .medBrukerErMottaker(false)
            .medArbeidsgiver(tilkommetArbeid)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(DAGSATS)
            .build(periode);
        BeregningsresultatAndel.builder()
            .medDagsats(0)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(tilkommetArbeid)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(0))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(0)
            .build(periode);
        when(beregningsresultatRepository.hentBeregningsresultat(behandling.getId())).thenReturn(Optional.of(build));
    }

}
