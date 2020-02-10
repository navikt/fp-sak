package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import static no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.RepositoryProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BesteberegningFødendeKvinneTjenesteTest {

    public static final String ORGNR = "973861778";
    private static LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 7, 1);
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private BehandlingReferanse behandlingReferanse;
    private static final ÅpenDatoIntervallEntitet OPPTJENINGSPERIODE = fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusYears(10));

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste = mock(OpptjeningForBeregningTjeneste.class);

    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private Behandling behandling;

    @Before
    public void setUp() {
        ScenarioForeldrepenger scenario = ScenarioForeldrepenger.nyttScenario();
        behandlingReferanse = scenario.lagre(repositoryProvider);
        behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingReferanse.getBehandlingId(), InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        besteberegningFødendeKvinneTjeneste = new BesteberegningFødendeKvinneTjeneste(familieHendelseRepository,
            opptjeningForBeregningTjeneste, inntektArbeidYtelseTjeneste);
    }

    @Test
    public void skal_ikkje_gi_fødende_kvinne_for_adopsjon() {
        // Act
        boolean erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.MORA, FamilieHendelseType.ADOPSJON);

        // Assert
        assertThat(erFødendeKvinne).isFalse();
    }

    @Test
    public void skal_ikkje_gi_fødende_kvinne_for_far_fødsel() {
        // Act
        boolean erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.FARA, FamilieHendelseType.FØDSEL);

        // Assert
        assertThat(erFødendeKvinne).isFalse();
    }

    @Test
    public void skal_gi_fødende_kvinne_for_mor_fødsel() {
        // Act
        boolean erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.MORA, FamilieHendelseType.FØDSEL);

        // Assert
        assertThat(erFødendeKvinne).isTrue();
    }

    @Test
    public void skalGiBesteberegningNårDagpengerIOpptjeningsperioden() {
        BehandlingReferanse ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        lagreFamilihendelseFødsel();
        OpptjeningAktiviteter opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new no.nav.abakus.iaygrunnlag.Periode(OPPTJENINGSPERIODE.getFomDato(), OPPTJENINGSPERIODE.getTomDato()));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
            .thenReturn(Optional.of(opptjeningAktiviteter));

        // Act
        boolean resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalIkkeGiBesteberegningIkkeDagpengerIOpptjeningsperioden() {
        lagreFamilihendelseFødsel();
        OpptjeningAktiviteter opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            new no.nav.abakus.iaygrunnlag.Periode(OPPTJENINGSPERIODE.getFomDato(), OPPTJENINGSPERIODE.getTomDato()), ORGNR);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
            .thenReturn(Optional.of(opptjeningAktiviteter));
        BehandlingReferanse ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        // Act
        boolean resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);

        // Assert
        assertThat(resultat).isFalse();
    }

    private void lagreFamilihendelseFødsel() {
        FamilieHendelseBuilder familieHendelseBuilder = familieHendelseRepository.opprettBuilderFor(behandling)
            .medAntallBarn(1)
            .medFødselsDato(SKJÆRINGSTIDSPUNKT.plusWeeks(3));
        familieHendelseRepository.lagre(behandlingReferanse.getBehandlingId(), familieHendelseBuilder);
    }

    private BehandlingReferanse lagBehandlingReferanseMedStp(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT);
    }

}
