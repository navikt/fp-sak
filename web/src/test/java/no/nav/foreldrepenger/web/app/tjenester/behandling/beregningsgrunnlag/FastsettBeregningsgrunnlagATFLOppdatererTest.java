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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
import no.nav.foreldrepenger.domene.rest.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;

@ExtendWith(MockitoExtension.class)
public class FastsettBeregningsgrunnlagATFLOppdatererTest {
    private FastsettBeregningsgrunnlagATFLOppdaterer oppdaterer;

    @Mock
    private FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste historikkTidsbegrenset;

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
    private BeregningHåndterer beregningHåndterer;

    @Mock
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;

    @Mock
    private BeregningsgrunnlagInputFelles beregningsgrunnlagInputFelles;

    @Mock
    private BeregningsgrunnlagInput input;

    @BeforeEach
    public void setup() {
        when(behandling.getFagsak()).thenReturn(fagsak);
        oppdaterer = new FastsettBeregningsgrunnlagATFLOppdaterer(beregningsgrunnlagTjeneste, historikk, historikkTidsbegrenset,
                beregningsgrunnlagInputTjeneste, beregningHåndterer);
    }

    @Test
    public void skal_håndtere_overflødig_fastsett_tidsbegrenset_arbeidsforhold_aksjonspunkt() {
        // Arrange
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(anyLong()))
                .thenReturn(BeregningsgrunnlagEntitet.ny().medSkjæringstidspunkt(LocalDate.now()).build());

        when(behandling.getÅpentAksjonspunktMedDefinisjonOptional(any())).thenReturn(Optional.of(ap));
        when(ap.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        when(beregningsgrunnlagInputTjeneste.getTjeneste(Mockito.any())).thenReturn(beregningsgrunnlagInputFelles);
        when(beregningsgrunnlagInputFelles.lagInput(any(BehandlingReferanse.class))).thenReturn(input);

        // Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.emptyList(), null);
        // Act
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, ap, dto));

        // Assert
        assertThat(resultat.getEkstraAksjonspunktResultat()).hasSize(1);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getAksjonspunktDefinisjon())
                .isEqualTo(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getMålStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }
}
