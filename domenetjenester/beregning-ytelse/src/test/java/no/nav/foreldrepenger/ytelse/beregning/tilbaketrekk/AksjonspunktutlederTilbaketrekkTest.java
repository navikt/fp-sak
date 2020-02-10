package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
        lagUtbetaltBeregningsresultatMedEnAndelTilBruker();
        lagBeregningsresultatForTilkommetArbeidMedRefusjon();
        lagIayForAvsluttetOgTilkommetArbeid();

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(behandlingReferanse));

        // Assert
        assertThat(aksjonspunktResultater.size()).isEqualTo(1);
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

    private void lagUtbetaltBeregningsresultatMedEnAndelTilBruker() {
        BeregningsresultatEntitet build = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2))
            .build(build);
        BeregningsresultatAndel.builder()
            .medDagsats(DAGSATS)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(ARBEIDSGIVER1)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(DAGSATS)
            .build(periode);
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(ORIGINAL_BEHANDLING_ID)).thenReturn(Optional.of(build));
    }

    private void lagBeregningsresultatForTilkommetArbeidMedRefusjon() {
        BeregningsresultatEntitet build = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2))
            .build(build);
        BeregningsresultatAndel.builder()
            .medDagsats(0)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(ARBEIDSGIVER1)
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
            .medArbeidsgiver(ARBEIDSGIVER2)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(DAGSATS)
            .build(periode);
        when(beregningsresultatRepository.hentBeregningsresultat(behandling.getId())).thenReturn(Optional.of(build));
    }

}
