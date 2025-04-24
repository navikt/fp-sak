package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@ExtendWith(MockitoExtension.class)
class ForeslåBeregningsgrunnlagStegTest {

    @Mock
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Mock
    private BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagRegelResultat;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingskontrollKontekst kontekst;
    @Mock
    private BeregningTjeneste beregningTjeneste;
    @Mock
    private Behandling behandling;
    private ForeslåBeregningsgrunnlagSteg steg;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    @BeforeEach
    void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(kontekst.getBehandlingId()).thenReturn(behandling.getId());
        when(beregningTjeneste.beregn(any(), any())).thenReturn(beregningsgrunnlagRegelResultat);

        steg = new ForeslåBeregningsgrunnlagSteg(behandlingRepository, beregningsgrunnlagKopierOgLagreTjeneste, beregningTjeneste);

        iayTjeneste.lagreInntektsmeldinger(behandling.getSaksnummer(), behandling.getId(), List.of());
    }

    @Test
    void stegUtførtUtenAksjonspunkter() {
        // Arrange
        opprettVilkårResultatForBehandling();

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void stegUtførtNårRegelResultatInneholderAutopunkt() {
        // Arrange
        opprettVilkårResultatForBehandling();
        when(beregningsgrunnlagRegelResultat.getAksjonspunkter()).thenReturn(List.of(AksjonspunktResultat.opprettForAksjonspunkt(
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS)));

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).hasSize(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
    }

    private void opprettVilkårResultatForBehandling() {
        var vilkårResultat = VilkårResultat.builder().buildFor(behandling);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
    }
}
