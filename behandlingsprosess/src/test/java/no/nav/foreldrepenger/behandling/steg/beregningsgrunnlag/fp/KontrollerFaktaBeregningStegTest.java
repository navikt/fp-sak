package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.KontrollerFaktaBeregningSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;

@CdiDbAwareTest
class KontrollerFaktaBeregningStegTest {

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
    void skal_reaktivere_grunnlag_ved_hopp_bakover() {
        var behandling = lagreBehandling();
        lagreBeregningsgrunnlag(false, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER, behandling);
        // Arrange
        var overstyrtTilstand = BeregningsgrunnlagTilstand.KOFAKBER_UT;
        lagreBeregningsgrunnlag(false, overstyrtTilstand, behandling);
        var kontekst = lagBehandlingskontrollkontekst(behandling);
        var tilSteg = BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
        var fraSteg = BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG;
        // Act
        steg.vedHoppOverBakover(kontekst, null, tilSteg, fraSteg);
        // Assert
        var aktivtGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(
                behandling.getId());
        assertThat(aktivtGrunnlag.get().getBeregningsgrunnlagTilstand()).isEqualTo(
                BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    private void lagreBeregningsgrunnlag(boolean overstyrt,
            BeregningsgrunnlagTilstand tilstand,
            Behandling behandling) {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .medOverstyring(overstyrt)
                .build();
        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag,
                tilstand);
    }

    private BehandlingskontrollKontekst lagBehandlingskontrollkontekst(Behandling behandling) {
        var behandlingLås = behandlingRepository.taSkriveLås(behandling.getId());
        return new BehandlingskontrollKontekst(behandling, behandlingLås);
    }
}
