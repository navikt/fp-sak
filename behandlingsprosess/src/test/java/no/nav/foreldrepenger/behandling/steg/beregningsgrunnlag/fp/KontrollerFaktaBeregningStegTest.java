package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.KontrollerFaktaBeregningSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class KontrollerFaktaBeregningStegTest {


    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    @Inject
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Inject
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;

    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(repoRule.getEntityManager());
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private KontrollerFaktaBeregningSteg steg;
    private Behandling behandling;

    @Before
    public void setUp() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(SKJÆRINGSTIDSPUNKT);
        var andelGraderingTjeneste = Mockito.mock(AndelGraderingTjeneste.class);
        var iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var opptjeningForBeregningTjeneste = Mockito.mock(OpptjeningForBeregningTjeneste.class);
        var inputProvider = Mockito.mock(BeregningsgrunnlagInputProvider.class);
        behandling = scenario.lagre(repositoryProvider);

        InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        var inputTjeneste = new BeregningsgrunnlagInputTjeneste(repositoryProvider, iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste, opptjeningForBeregningTjeneste,
            besteberegningFødendeKvinneTjeneste, inntektsmeldingTjeneste);
        when(inputProvider.getTjeneste(FagsakYtelseType.FORELDREPENGER)).thenReturn(inputTjeneste);
        steg = new KontrollerFaktaBeregningSteg(beregningsgrunnlagKopierOgLagreTjeneste, behandlingRepository, hentBeregningsgrunnlagTjeneste, inputProvider);
        lagreBeregningsgrunnlag(false, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    @Test
    public void skal_ikke_reaktivere_grunnlag_ved_hopp_bakover_og_overstyring() {
        // Arrange
        BeregningsgrunnlagTilstand overstyrtTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        lagreBeregningsgrunnlag(true, overstyrtTilstand);
        BehandlingskontrollKontekst kontekst = lagBehandlingskontrollkontekst();
        BehandlingStegType tilSteg = BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
        BehandlingStegType fraSteg = BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
        // Act
        steg.vedHoppOverBakover(kontekst, null, tilSteg, fraSteg);
        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(overstyrtTilstand);
    }

    @Test
    public void skal_reaktivere_grunnlag_ved_hopp_bakover_uten_overstyring() {
        // Arrange
        BeregningsgrunnlagTilstand overstyrtTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        lagreBeregningsgrunnlag(false, overstyrtTilstand);
        BehandlingskontrollKontekst kontekst = lagBehandlingskontrollkontekst();
        BehandlingStegType tilSteg = BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
        BehandlingStegType fraSteg = BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
        // Act
        steg.vedHoppOverBakover(kontekst, null, tilSteg, fraSteg);
        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }


    private void lagreBeregningsgrunnlag(boolean overstyrt, BeregningsgrunnlagTilstand tilstand) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .medOverstyring(overstyrt).build();
        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, tilstand);
    }

    private BehandlingskontrollKontekst lagBehandlingskontrollkontekst() {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling.getId());
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(),
            behandlingLås);
    }
}
