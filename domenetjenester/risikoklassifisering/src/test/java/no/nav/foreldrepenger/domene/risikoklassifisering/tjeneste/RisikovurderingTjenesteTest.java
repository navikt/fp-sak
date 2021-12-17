package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.HentRisikovurderingDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikogruppeDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;

public class RisikovurderingTjenesteTest {

    private final RisikoklassifiseringRepository risikoklassifiseringRepository = mock(RisikoklassifiseringRepository.class);

    private final FpriskTjeneste fpriskTjeneste = mock(FpriskTjeneste.class);

    private RisikovurderingTjeneste risikovurderingTjeneste;

    private Behandling behandling;


    @BeforeEach
    public void setup() {
        var scenarioFørstegang = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenarioFørstegang.lagMocked();
        risikovurderingTjeneste = new RisikovurderingTjeneste(risikoklassifiseringRepository,
            fpriskTjeneste);
    }

    @Test
    public void skal_teste_at_vi_henter_resultat_fra_fprisk_ved_ikke_høy_risiko() {
        // Arrange
        var referanse = BehandlingReferanse.fra(behandling);
        var respons = lagRespons(RisikoklasseType.IKKE_HØY, Collections.emptyList(), null);
        when(fpriskTjeneste.hentFaresignalerForBehandling(new HentRisikovurderingDto(referanse.getBehandlingUuid()))).thenReturn(Optional.of(respons));

        // Act
        var faresignalWrapper = risikovurderingTjeneste.hentRisikoklassifisering(referanse);

        // Assert
        assertThat(faresignalWrapper).isPresent();
        assertThat(faresignalWrapper.get().kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(faresignalWrapper.get().medlemskapFaresignaler()).isNull();
        assertThat(faresignalWrapper.get().iayFaresignaler()).isNull();
    }

    @Test
    public void skal_teste_at_vi_henter_resultat_fra_fprisk_ved_høy_risiko() {
        // Arrange
        var referanse = BehandlingReferanse.fra(behandling);
        var faresignaler = Arrays.asList("test 1", "test 2", "test 3");
        var respons = lagRespons(RisikoklasseType.HØY, faresignaler, FaresignalVurdering.AVSLAG_ANNET);
        when(fpriskTjeneste.hentFaresignalerForBehandling(new HentRisikovurderingDto(referanse.getBehandlingUuid()))).thenReturn(Optional.of(respons));

        // Act
        var faresignalWrapper = risikovurderingTjeneste.hentRisikoklassifisering(referanse);

        // Assert
        assertThat(faresignalWrapper).isPresent();
        assertThat(faresignalWrapper.get().kontrollresultat()).isEqualTo(Kontrollresultat.HØY);
        assertThat(faresignalWrapper.get().medlemskapFaresignaler().faresignaler()).isEqualTo(faresignaler);
        assertThat(faresignalWrapper.get().iayFaresignaler().faresignaler()).isEqualTo(faresignaler);
    }

    @Test
    public void skal_teste_at_aksjonspunkt_opprettes_når_risiko_er_høy() {
        // Arrange
        var referanse = BehandlingReferanse.fra(behandling);
        var faresignaler = Arrays.asList("test 1", "test 2", "test 3");
        var respons = lagRespons(RisikoklasseType.HØY, faresignaler, FaresignalVurdering.AVSLAG_ANNET);
        when(fpriskTjeneste.hentFaresignalerForBehandling(new HentRisikovurderingDto(referanse.getBehandlingUuid()))).thenReturn(Optional.of(respons));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isTrue();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_når_risiko_er_lav() {
        // Arrange
        var referanse = BehandlingReferanse.fra(behandling);
        var respons = lagRespons(RisikoklasseType.IKKE_HØY, Collections.emptyList(), null);
        when(fpriskTjeneste.hentFaresignalerForBehandling(new HentRisikovurderingDto(referanse.getBehandlingUuid()))).thenReturn(Optional.of(respons));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_det_mangler_kontrollresultat() {
        // Arrange
        var ref = BehandlingReferanse.fra(behandling);
        when(fpriskTjeneste.hentFaresignalerForBehandling(new HentRisikovurderingDto(ref.getBehandlingUuid()))).thenReturn(Optional.empty());

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(ref);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    private RisikovurderingResultatDto lagRespons(RisikoklasseType risikoklasse, List<String> faresignaler, FaresignalVurdering faresignalVurdering) {
        var riskGruppe = new RisikogruppeDto(faresignaler);
        return new RisikovurderingResultatDto(risikoklasse, riskGruppe, riskGruppe, faresignalVurdering);
    }
}
