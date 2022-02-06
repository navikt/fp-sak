package no.nav.foreldrepenger.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
public class StønadsperiodeInnhenterTest extends EntityManagerAwareTest {



    private static final int DAGSATS = 100;
    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId MEDF_AKTØR_ID = AktørId.dummy();

    private static final LocalDate FH_DATO = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(2));
    private static final LocalDate SISTE_DAG_MOR = FH_DATO.plusWeeks(6);
    private static final LocalDate STP_NORMAL = FH_DATO.minusWeeks(3);

    private static final LocalDate FH_DATO_ELDRE = VirkedagUtil.fomVirkedag(FH_DATO.minusYears(2));
    private static final LocalDate FH_DATO_YNGRE = VirkedagUtil.fomVirkedag(FH_DATO.plusWeeks(45));


    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Mock
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    @Mock
    private FamilieHendelseGrunnlagEntitet fhGrunnlagAktuellMock;
    @Mock
    private FamilieHendelseEntitet familieHendelseAktuellMock;
    @Mock
    private FamilieHendelseGrunnlagEntitet fhGrunnlagAnnenMock;
    @Mock
    private FamilieHendelseEntitet familieHendelseAnnenMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private Skjæringstidspunkt skjæringstidspunkt;

    private StønadsperioderInnhenter stønadsperioderInnhenter;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        stønadsperioderInnhenter = new StønadsperioderInnhenter(repositoryProvider, familieHendelseTjeneste, skjæringstidspunktTjeneste);
    }

    /*
     * Cases
     * 1. Mor FP, ingen senere saker, FP for tidligere barn
     * 2. Mor FP, to tette
     * 3. Mor SVP, finnes innvilget FP for samme barn
     * 4. Far FP, er oppgitt som annenpart i vedtatt sak for nyere barn
     * 5. adopsjon?
     */

    @Test
    public void behandlingMorFPIngenNyereSaker() {
        var eldreBehandling = lagBehandlingMor(FH_DATO_ELDRE, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(eldreBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_ELDRE);
        var berResMorBehEldre = lagBeregningsresultat(FH_DATO_ELDRE.minusWeeks(2), FH_DATO_ELDRE.plusWeeks(18), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(eldreBehandling, berResMorBehEldre);
        avsluttBehandling(eldreBehandling);

        var behandling = lagBehandlingMor(FH_DATO, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(any())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        Mockito.lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        Mockito.lenient().when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(STP_NORMAL);
        var berResMorBeh1 = lagBeregningsresultat(STP_NORMAL, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(behandling, berResMorBeh1);

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperioderLoggResultat(behandling);
        assertThat(muligSak).isEmpty();
    }

    @Test
    public void behandlingMorFPToTette() {
        var nyereBehandling = lagBehandlingMor(FH_DATO_YNGRE, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE);
        var berResMorBehNy = lagBeregningsresultat(FH_DATO_YNGRE.plusWeeks(2), FH_DATO_YNGRE.plusWeeks(4), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyereBehandling, berResMorBehNy);
        avsluttBehandling(nyereBehandling);


        var behandling = lagBehandlingMor(FH_DATO, AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(behandling.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        Mockito.lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        Mockito.lenient().when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(STP_NORMAL);
        var berResMorBeh1 = lagBeregningsresultat(STP_NORMAL, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(behandling, berResMorBeh1);

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperioderLoggResultat(behandling);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(nyereBehandling.getFagsak().getSaksnummer());
            assertThat(v.valgtStartdato()).isEqualTo(FH_DATO_YNGRE);
        });
    }

    @Test
    public void behandlingMorSVPHarInnvilgetFPSammeBarn() {

        var avsluttetFPBehMor = lagBehandlingMor(FH_DATO.minusWeeks(1), AKTØR_ID_MOR, null);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(avsluttetFPBehMor.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO.minusWeeks(1));
        var berResMorBeh1 = lagBeregningsresultat(STP_NORMAL, SISTE_DAG_MOR.minusWeeks(1), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        avsluttetFPBehMor.avsluttBehandling();

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyBehSVPOverlapper.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        Mockito.lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        Mockito.lenient().when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.minusWeeks(12));
        var berResMedOverlapp = lagBeregningsresultat(FH_DATO.minusWeeks(12), STP_NORMAL.minusDays(1), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperioderLoggResultat(nyBehSVPOverlapper);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(avsluttetFPBehMor.getFagsak().getSaksnummer());
            assertThat(v.valgtStartdato()).isEqualTo(STP_NORMAL);
        });
    }

    @Test
    public void behandlingFarDerMorHarNySakTette() {
        var nyereBehandling = lagBehandlingMor(FH_DATO_YNGRE, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(nyereBehandling.getId())).thenReturn(Optional.of(fhGrunnlagAnnenMock));
        Mockito.lenient().when(fhGrunnlagAnnenMock.getGjeldendeVersjon()).thenReturn(familieHendelseAnnenMock);
        Mockito.lenient().when(familieHendelseAnnenMock.getSkjæringstidspunkt()).thenReturn(FH_DATO_YNGRE);
        var berResMorBehNy = lagBeregningsresultat(FH_DATO_YNGRE.minusWeeks(3), FH_DATO_YNGRE.plusWeeks(6), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyereBehandling, berResMorBehNy);
        avsluttBehandling(nyereBehandling);


        var behandling = lagBehandlingFar(FH_DATO, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        Mockito.lenient().when(familieHendelseTjeneste.finnAggregat(behandling.getId())).thenReturn(Optional.of(fhGrunnlagAktuellMock));
        Mockito.lenient().when(fhGrunnlagAktuellMock.getGjeldendeVersjon()).thenReturn(familieHendelseAktuellMock);
        Mockito.lenient().when(familieHendelseAktuellMock.getSkjæringstidspunkt()).thenReturn(FH_DATO);
        Mockito.lenient().when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        Mockito.lenient().when(skjæringstidspunkt.getUtledetSkjæringstidspunkt()).thenReturn(FH_DATO.plusWeeks(34));
        var berResFarBeh1 = lagBeregningsresultat(FH_DATO.plusWeeks(34), FH_DATO.plusWeeks(49), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(behandling, berResFarBeh1);

        assertThat(repositoryProvider.getPersonopplysningRepository().fagsakerMedOppgittAnnenPart(MEDF_AKTØR_ID)).isNotEmpty();

        var muligSak = stønadsperioderInnhenter.finnSenereStønadsperioderLoggResultat(behandling);
        assertThat(muligSak).hasValueSatisfying(v -> {
            assertThat(v.saksnummer()).isEqualTo(nyereBehandling.getFagsak().getSaksnummer());
            assertThat(v.valgtStartdato()).isEqualTo(FH_DATO_YNGRE.minusWeeks(3));
        });
    }

    private Behandling lagBehandlingMor(LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
        var scenarioMor = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioMor.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId != null) {
            scenarioMor.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenarioMor.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioMor.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioMor.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenarioMor.lagre(repositoryProvider);
        return behandling;
    }


    private Behandling lagBehandlingFar(LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioFar.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId != null) {
            scenarioFar.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Is Pinne")
                .medType(SøknadAnnenPartType.MOR);
        }
        scenarioFar.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFar.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioFar.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenarioFar.lagre(repositoryProvider);
        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonMor(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(
                scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId != null) {
            scenario.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);

        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonFar(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioFarSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(
                scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId != null) {
            scenario.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling);

        return behandling;
    }

    private Behandling lagBehandlingSVP(AktørId aktørId) {
        var scenarioSVP = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioSVP.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenarioSVP.medDefaultOppgittTilknytning();
        scenarioSVP.medSøknadHendelse().medTerminbekreftelse(scenarioSVP.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGEN MIN")
                .medTermindato(FH_DATO)
                .medUtstedtDato(LocalDate.now().minusDays(3)))
            .medAntallBarn(1);
        scenarioSVP.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioSVP.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioSVP.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandlingSVP = scenarioSVP.lagre(repositoryProvider);
        return behandlingSVP;
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom,
                                                            LocalDate periodeTom,
                                                            Inntektskategori inntektskategori) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private void avsluttBehandling(Behandling behandling) {
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
    }
}
