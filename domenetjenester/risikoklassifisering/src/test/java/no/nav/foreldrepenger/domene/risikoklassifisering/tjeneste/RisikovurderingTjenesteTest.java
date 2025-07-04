package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikogruppeDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

class RisikovurderingTjenesteTest {

    private final FpriskTjeneste fpriskTjeneste = mock(FpriskTjeneste.class);
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);

    private RisikovurderingTjeneste risikovurderingTjeneste;

    private Behandling behandling;

    private BehandlingReferanse referanse;

    @BeforeEach
    void setup() {
        var scenarioFørstegang = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenarioFørstegang.lagMocked();
        risikovurderingTjeneste = new RisikovurderingTjeneste(fpriskTjeneste, prosessTaskTjeneste);
        referanse = BehandlingReferanse.fra(behandling);
    }

    @Test
    void skal_teste_at_vi_returnerer_tom_hvis_ikke_noe_resultat_mottas_fra_fprisk() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.empty());

        // Act
        var faresignalWrapper = risikovurderingTjeneste.hentRisikoklassifisering(referanse);

        // Assert
        assertThat(faresignalWrapper).isNotPresent();
    }

    @Test
    void skal_teste_at_aksjonspunkt_opprettes_når_risiko_er_høy() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(RisikoklasseType.HØY, Collections.emptyList(), null)));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isTrue();
    }

    @Test
    void skal_teste_at_aksjonspunkt_ikke_opprettes_når_risiko_er_lav() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(RisikoklasseType.IKKE_HØY, Collections.emptyList(), null)));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    @Test
    void skal_teste_at_aksjonspunkt_ikke_opprettes_det_mangler_kontrollresultat() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.empty());

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    private RisikovurderingResultatDto lagRespons(RisikoklasseType risikoklasse, List<String> faresignaler, FaresignalVurdering faresignalVurdering) {
        var riskGruppe = new RisikogruppeDto(faresignaler);
        return new RisikovurderingResultatDto(risikoklasse, riskGruppe, riskGruppe, faresignalVurdering);
    }

}
