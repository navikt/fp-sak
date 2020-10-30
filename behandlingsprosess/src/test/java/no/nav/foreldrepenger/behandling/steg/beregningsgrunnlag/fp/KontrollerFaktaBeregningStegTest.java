package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.kalkulator.steg.BeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.KontrollerFaktaBeregningSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.GrunnbeløpTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.BeregningTilInputTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.KalkulatorStegProsesseringInputTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningsperioderUtenOverstyringTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class KontrollerFaktaBeregningStegTest extends EntityManagerAwareTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;

    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;
    private BehandlingRepository behandlingRepository;
    private KontrollerFaktaBeregningSteg steg;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        hentBeregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(entityManager);
        var beregningsgrunnlagTjeneste = CDI.current().select(BeregningsgrunnlagTjeneste.class).get();
        var beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        var kalkulusKonfigInjecter = new KalkulusKonfigInjecter(12, new FakeUnleash());
        beregningsgrunnlagKopierOgLagreTjeneste = new BeregningsgrunnlagKopierOgLagreTjeneste(beregningsgrunnlagRepository,
            beregningsgrunnlagTjeneste, new KalkulatorStegProsesseringInputTjeneste(beregningsgrunnlagRepository, behandlingRepository,
            new BeregningTilInputTjeneste(beregningsgrunnlagRepository, kalkulusKonfigInjecter),
            new GrunnbeløpTjeneste(beregningsgrunnlagRepository), kalkulusKonfigInjecter));
        var besteberegningFødendeKvinneTjeneste = new BesteberegningFødendeKvinneTjeneste(new FamilieHendelseRepository(entityManager),
            new OpptjeningForBeregningTjeneste(new OpptjeningsperioderUtenOverstyringTjeneste(new OpptjeningRepository(entityManager, behandlingRepository))),
            new AbakusInMemoryInntektArbeidYtelseTjeneste());

        var andelGraderingTjeneste = Mockito.mock(AndelGraderingTjeneste.class);
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var opptjeningForBeregningTjeneste = Mockito.mock(OpptjeningForBeregningTjeneste.class);
        var inputProvider = Mockito.mock(BeregningsgrunnlagInputProvider.class);
        var kalkuluskonfig = new KalkulusKonfigInjecter();

        InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        var inputTjeneste = new BeregningsgrunnlagInputTjeneste(new BehandlingRepositoryProvider(entityManager), iayTjeneste, skjæringstidspunktTjeneste,
            andelGraderingTjeneste, opptjeningForBeregningTjeneste,
            besteberegningFødendeKvinneTjeneste, inntektsmeldingTjeneste, kalkuluskonfig);
        when(inputProvider.getTjeneste(FagsakYtelseType.FORELDREPENGER)).thenReturn(inputTjeneste);
        steg = new KontrollerFaktaBeregningSteg(beregningsgrunnlagKopierOgLagreTjeneste, behandlingRepository, hentBeregningsgrunnlagTjeneste, inputProvider);
    }

    private Behandling lagreBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(SKJÆRINGSTIDSPUNKT);
        return scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
    }

    @Test
    public void skal_reaktivere_grunnlag_ved_hopp_bakover() {
        var behandling = lagreBehandling();
        lagreBeregningsgrunnlag(false, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER, behandling);
        // Arrange
        BeregningsgrunnlagTilstand overstyrtTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        lagreBeregningsgrunnlag(false, overstyrtTilstand, behandling);
        BehandlingskontrollKontekst kontekst = lagBehandlingskontrollkontekst(behandling);
        BehandlingStegType tilSteg = BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
        BehandlingStegType fraSteg = BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
        // Act
        steg.vedHoppOverBakover(kontekst, null, tilSteg, fraSteg);
        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private void lagreBeregningsgrunnlag(boolean overstyrt, BeregningsgrunnlagTilstand tilstand, Behandling behandling) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medOverstyring(overstyrt).build();
        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, tilstand);
    }

    private BehandlingskontrollKontekst lagBehandlingskontrollkontekst(Behandling behandling) {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling.getId());
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(),
            behandlingLås);
    }
}
