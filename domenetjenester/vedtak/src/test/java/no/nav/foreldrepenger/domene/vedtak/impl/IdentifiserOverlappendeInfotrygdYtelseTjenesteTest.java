package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdPSGrunnlag;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdSPGrunnlag;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Periode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Tema;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.TemaKode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Vedtak;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class IdentifiserOverlappendeInfotrygdYtelseTjenesteTest {

    private IdentifiserOverlappendeInfotrygdYtelseTjeneste overlappendeInfotrygdYtelseTjeneste;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private BehandlingOverlappInfotrygdRepository overlappRepository = new BehandlingOverlappInfotrygdRepository(entityManager);

    @Mock
    private AktørConsumerMedCache aktørConsumerMock;
    @Mock
    private InfotrygdPSGrunnlag infotrygdPSGrTjenesteMock;
    @Mock
    private InfotrygdSPGrunnlag infotrygdSPGrTjenesteMock;

    private Behandling behandlingFP;
    private LocalDate førsteUttaksdatoFp;


    @Before
    public void oppsett() {

        initMocks(this);
        aktørConsumerMock = mock(AktørConsumerMedCache.class);
        infotrygdPSGrTjenesteMock = mock(InfotrygdPSGrunnlag.class);
        infotrygdSPGrTjenesteMock = mock(InfotrygdSPGrunnlag.class);
        overlappendeInfotrygdYtelseTjeneste = new IdentifiserOverlappendeInfotrygdYtelseTjeneste(beregningsresultatRepository, aktørConsumerMock,infotrygdPSGrTjenesteMock, infotrygdSPGrTjenesteMock , overlappRepository, mock(BehandlingRepository.class));
        førsteUttaksdatoFp = LocalDate.now().minusMonths(4).minusWeeks(2);
        førsteUttaksdatoFp = VirkedagUtil.fomVirkedag(førsteUttaksdatoFp);

        ScenarioMorSøkerForeldrepenger scenarioAvsluttetBehMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioAvsluttetBehMor.medSøknadHendelse().medFødselsDato(førsteUttaksdatoFp);
        scenarioAvsluttetBehMor.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehMor.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehMor.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        behandlingFP = scenarioAvsluttetBehMor.lagre(repositoryProvider);
        PersonIdent person = new PersonIdent("12345678901");
        when(aktørConsumerMock.hentPersonIdentForAktørId(any())).thenReturn(Optional.of(person.getIdent()));
    }

    // CASE 1:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    @Test
    public void overlapp_når_Fp_starter_samme_dag_som_IT_opphører() {
        // Arrange
        List<Vedtak> vedtakPeriode = new ArrayList<>();
        vedtakPeriode.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp, 100));
        Grunnlag infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp, vedtakPeriode);


        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(Collections.emptyList());

        BeregningsresultatEntitet berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        List<BehandlingOverlappInfotrygd> overlappIT = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(overlappIT).hasSize(1);
        assertThat(overlappIT.get(0).getPeriodeInfotrygd().getTomDato()).isEqualTo(førsteUttaksdatoFp);
    }

    @Test
    public void overlapp_gradert_opphører() {
        // Arrange
        List<Vedtak> vedtakPeriode = new ArrayList<>();
        vedtakPeriode.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4), 50));
        Grunnlag infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4), vedtakPeriode);


        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(Collections.emptyList());

        BeregningsresultatEntitet berFp = lagGradertBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        List<BehandlingOverlappInfotrygd> overlappIT = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(overlappIT).isEmpty();

        // Arrange2
        List<Vedtak> vedtakPeriode2 = new ArrayList<>();
        vedtakPeriode2.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4), 100));
        Grunnlag infotrygPSGrunnlag2 = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4), vedtakPeriode2);


        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygPSGrunnlag2));

        // Act
        List<BehandlingOverlappInfotrygd> overlappIT2 = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(overlappIT2).hasSize(1);
        assertThat(overlappIT2.get(0).getPeriodeInfotrygd().getTomDato()).isEqualTo(førsteUttaksdatoFp.plusWeeks(4));
    }

    @Test
    public void flereOverlappIlisten() {
        // Arrange
        List<Vedtak> vedtakPerioder = new ArrayList<>();
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp.minusWeeks(3), 100));
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp.minusWeeks(2), 100));
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp.minusWeeks(2), 100));
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp, 100));

        Grunnlag infotrygSPGrunnlag = lagGrunnlagSPIT(førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp, vedtakPerioder);

        List<Vedtak> vedtakPerioderPS = new ArrayList<>();
        vedtakPerioderPS.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(10), førsteUttaksdatoFp, 100));

        Grunnlag infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(20),førsteUttaksdatoFp.plusWeeks(4), vedtakPerioderPS);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygSPGrunnlag));

        BeregningsresultatEntitet berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusMonths(5));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        List<BehandlingOverlappInfotrygd> flereSomOverlapper = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(flereSomOverlapper).hasSize(2);
    }

    @Test
    public void flereGrunnlagMenEttOverlappIlisten() {

        // Arrange
        List<Vedtak> vedtakPerioder = new ArrayList<>();
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.minusDays(5), 100));
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(4), førsteUttaksdatoFp.plusWeeks(3), 100));

        Grunnlag infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp, førsteUttaksdatoFp.plusDays(30), vedtakPerioder);

        List<Vedtak> vedtakPerioderSP = new ArrayList<>();
        Vedtak vedtakSP1 = lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(10), førsteUttaksdatoFp.minusDays(1), 100);
        vedtakPerioderSP.add(vedtakSP1);

        Grunnlag infotrygSPGrunnlag = lagGrunnlagSPIT(førsteUttaksdatoFp.minusDays(20),førsteUttaksdatoFp, vedtakPerioderSP);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygSPGrunnlag));

        BeregningsresultatEntitet berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        List<BehandlingOverlappInfotrygd> flereSomOverlapper = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(flereSomOverlapper).hasSize(1);
        assertThat(flereSomOverlapper.get(0).getPeriodeInfotrygd().getTomDato()).isEqualTo(førsteUttaksdatoFp.plusWeeks(3));
    }

    @Test
    public void ingenOverlapp() {
        // Arrange
        List<Vedtak> vedtakPeriode = new ArrayList<>();
        vedtakPeriode.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.minusDays(1), 100));
        Grunnlag infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.minusDays(1), vedtakPeriode);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(Collections.emptyList());

        BeregningsresultatEntitet berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        List<BehandlingOverlappInfotrygd> overlappIT = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(overlappIT).hasSize(0);
    }

    @Test
    public void ingenGrunnlag() {
        // Arrange
        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(Collections.emptyList());
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any(), any(), any())).thenReturn(Collections.emptyList());

        BeregningsresultatEntitet berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        List<BehandlingOverlappInfotrygd> overlappIT = overlappendeInfotrygdYtelseTjeneste.vurderOmOverlappInfotrygd("", behandlingFP, null);

        // Assert
        assertThat(overlappIT).isEmpty();
    }

    public Grunnlag lagGrunnlagPSIT(LocalDate fom, LocalDate tom, List<Vedtak> vedtakPerioder) {
        Periode periode= new Periode(fom, tom);
        Tema tema = new Tema(TemaKode.BS, "Pleiepenger");

        Grunnlag grunnlagIT = new Grunnlag(null, tema, null, null, null, null, periode, null, null, null, tom, 0, LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1), "", vedtakPerioder);
        return grunnlagIT;
    }

    public Vedtak lagVedtakForGrunnlag(LocalDate fom, LocalDate tom, int utbetGrad) {
        Periode periode = new Periode(fom, tom);
        Vedtak vedtak = new Vedtak(periode, utbetGrad);

        return vedtak;
    }

    public Grunnlag lagGrunnlagSPIT(LocalDate fom, LocalDate tom, List<Vedtak> vedtakPerioder) {
        Periode periode= new Periode(fom, tom);
        Tema tema = new Tema(TemaKode.SP, "Sykepenger");

        Grunnlag grunnlagIT = new Grunnlag(null, tema, null, null, null, null, periode, null, null, null, tom, 0, LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1), "", vedtakPerioder);
        return grunnlagIT;
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(LocalDate periodeFom, LocalDate periodeTom) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(200)
            .medDagsatsFraBg(200)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private BeregningsresultatEntitet lagGradertBeregningsresultatFP(LocalDate periodeFom, LocalDate periodeTom) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(100)
            .medDagsatsFraBg(200)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }
}
