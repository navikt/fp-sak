package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.KontrollerFaktaBeregningSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

@CdiDbAwareTest
public class KontrollerFaktaBeregningStegTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    @Inject
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Inject
    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    @BehandlingTypeRef
    private KontrollerFaktaBeregningSteg steg;

    private Behandling lagreBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medAntallBarn(1).medFødselsDato(SKJÆRINGSTIDSPUNKT);
        return scenario.lagre(repositoryProvider);
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
        Optional<BeregningsgrunnlagGrunnlagEntitet> aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(
                behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(
                BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private void lagreBeregningsgrunnlag(boolean overstyrt,
            BeregningsgrunnlagTilstand tilstand,
            Behandling behandling) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medOverstyring(overstyrt)
                .build();
        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag,
                tilstand);
    }

    private BehandlingskontrollKontekst lagBehandlingskontrollkontekst(Behandling behandling) {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling.getId());
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingLås);
    }
}
