package no.nav.foreldrepenger.domene.fp;

import static no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.RepositoryProvider;
import no.nav.foreldrepenger.domene.prosess.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

@CdiDbAwareTest
public class BesteberegningFødendeKvinneTjenesteTest {

    private static final String ORGNR = "973861778";
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 7, 1);
    private final RepositoryProvider repositoryProvider;
    private BehandlingReferanse behandlingReferanse;
    private static final ÅpenDatoIntervallEntitet OPPTJENINGSPERIODE = fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
            SKJÆRINGSTIDSPUNKT.plusYears(10));

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    private final BehandlingRepository behandlingRepository;
    @Mock
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    @Mock
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private Behandling behandling;
    private AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private FagsakRepository fagsakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    public BesteberegningFødendeKvinneTjenesteTest(EntityManager em) {
        repositoryProvider = new RepositoryProvider(em);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = new FagsakRepository(em);
        beregningsresultatRepository = new BeregningsresultatRepository(em);
    }

    @BeforeEach
    public void setUp() {
        var scenario = ScenarioForeldrepenger.nyttScenario();
        behandlingReferanse = BehandlingReferanse.fra(scenario.lagre(repositoryProvider));
        behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingReferanse.getBehandlingId(),
                InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        besteberegningFødendeKvinneTjeneste = new BesteberegningFødendeKvinneTjeneste(familieHendelseRepository,
                opptjeningForBeregningTjeneste, inntektArbeidYtelseTjeneste, beregningsgrunnlagRepository, behandlingRepository,
            beregningsresultatRepository, fagsakRepository);
    }

    @Test
    public void skal_ikkje_gi_fødende_kvinne_for_adopsjon() {
        // Act
        var erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.MORA, FamilieHendelseType.ADOPSJON);

        // Assert
        assertThat(erFødendeKvinne).isFalse();
    }

    @Test
    public void skal_ikkje_gi_fødende_kvinne_for_far_fødsel() {
        // Act
        var erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.FARA, FamilieHendelseType.FØDSEL);

        // Assert
        assertThat(erFødendeKvinne).isFalse();
    }

    @Test
    public void skal_gi_fødende_kvinne_for_mor_fødsel() {
        // Act
        var erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.MORA, FamilieHendelseType.FØDSEL);

        // Assert
        assertThat(erFødendeKvinne).isTrue();
    }

    @Test
    public void skalGiBesteberegningNårDagpengerPåStp() {
        var ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        lagreFamilihendelseFødsel();
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
                new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.plusDays(1)));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
                .thenReturn(Optional.of(opptjeningAktiviteter));

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalGiBesteberegningNårSykepengerMedOvergangFraDagpenger() {
        var ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        lagreFamilihendelseFødsel();
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), OPPTJENINGSPERIODE.getTomDato()), ORGNR);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
            .thenReturn(Optional.of(opptjeningAktiviteter));

        var oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = oppdatere.getAktørYtelseBuilder(behandlingReferanse.getAktørId());
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10)))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(RelatertYtelseType.SYKEPENGER);
        var grunnlagBuilder = ytelseBuilder.getGrunnlagBuilder();
        grunnlagBuilder.medArbeidskategori(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);
        ytelseBuilder.medYtelseGrunnlag(grunnlagBuilder.build());
        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        oppdatere.leggTilAktørYtelse(aktørYtelseBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingReferanse.getBehandlingId(), oppdatere);

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skalIkkeGiBesteberegningIkkeDagpengerPåStp() {
        lagreFamilihendelseFødsel();
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.minusDays(3)));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
                .thenReturn(Optional.of(opptjeningAktiviteter));
        var ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void overstyrtBGSkalIkkeGiAutomatiskBesteberegning() {
        lagreFamilihendelseFødsel();
        var ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.plusDays(1)));
        var overstyrtBG = BeregningsgrunnlagEntitet.ny()
            .medOverstyring(true)
            .medSkjæringstidspunkt(LocalDate.now())
            .build();
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
            .thenReturn(Optional.of(opptjeningAktiviteter));
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(any()))
            .thenReturn(Optional.of(overstyrtBG));

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void fjernetDagpengerISaksbehandling() {
        lagreFamilihendelseFødsel();
        var ref = lagBehandlingReferanseMedStp(behandlingReferanse);
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.plusDays(1)));
        var grunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .build();
        var bgGr = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(grunnlag)
            .medRegisterAktiviteter(lagAggregat(OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetType.DAGPENGER))
            .medSaksbehandletAktiviteter(lagAggregat(OpptjeningAktivitetType.ARBEID))
            .buildUtenIdOgTilstand();
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any()))
            .thenReturn(Optional.of(opptjeningAktiviteter));
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(any()))
            .thenReturn(Optional.of(bgGr));

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref);

        // Assert
        assertThat(resultat).isFalse();
    }

    private BeregningAktivitetAggregatEntitet lagAggregat(OpptjeningAktivitetType... typer) {
        var builder = BeregningAktivitetAggregatEntitet.builder();
        Arrays.asList(typer).forEach(type -> builder.leggTilAktivitet(BeregningAktivitetEntitet.builder()
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(OPPTJENINGSPERIODE.getFomDato(), OPPTJENINGSPERIODE.getTomDato()))
            .medOpptjeningAktivitetType(type)
            .build()));
        return builder.medSkjæringstidspunktOpptjening(OPPTJENINGSPERIODE.getTomDato()).build();
    }


    private void lagreFamilihendelseFødsel() {
        var familieHendelseBuilder = familieHendelseRepository.opprettBuilderFor(behandling)
                .medAntallBarn(1)
                .medFødselsDato(SKJÆRINGSTIDSPUNKT.plusWeeks(3));
        familieHendelseRepository.lagre(behandlingReferanse.getBehandlingId(), familieHendelseBuilder);
    }

    private BehandlingReferanse lagBehandlingReferanseMedStp(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT);
    }

}
