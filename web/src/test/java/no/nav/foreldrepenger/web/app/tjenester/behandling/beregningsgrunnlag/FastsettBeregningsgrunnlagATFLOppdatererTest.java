package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.kalkulus.FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste;

@ExtendWith(MockitoExtension.class)
class FastsettBeregningsgrunnlagATFLOppdatererTest {
    private FastsettBeregningsgrunnlagATFLOppdaterer oppdaterer;

    @Mock
    private FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste historikkKalkulusTjeneste;

    @Mock
    private BeregningTjeneste beregningTjeneste;

    @Mock
    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste historikk;

    @Mock
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Mock
    private Behandling behandling;

    @Mock
    private Fagsak fagsak;

    @Mock
    private Aksjonspunkt ap;

    @Mock
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setup() {
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        oppdaterer = new FastsettBeregningsgrunnlagATFLOppdaterer(beregningsgrunnlagTjeneste, historikk,
                behandlingRepository, historikkKalkulusTjeneste, beregningTjeneste);
    }

    @Test
    void skal_håndtere_overflødig_fastsett_tidsbegrenset_arbeidsforhold_aksjonspunkt() {
        // Arrange
        var bg = BeregningsgrunnlagEntitet.ny().medSkjæringstidspunkt(LocalDate.now()).build();
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(1L, BeregningsgrunnlagTilstand.FASTSATT);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(anyLong()))
                .thenReturn(Optional.of(gr));

        when(behandling.getÅpentAksjonspunktMedDefinisjonOptional(any())).thenReturn(Optional.of(ap));
        when(ap.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);

        // Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.emptyList(), null);
        // Act
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, ap));

        // Assert
        assertThat(resultat.getEkstraAksjonspunktResultat()).hasSize(1);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getAksjonspunktDefinisjon())
                .isEqualTo(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getMålStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }
}
