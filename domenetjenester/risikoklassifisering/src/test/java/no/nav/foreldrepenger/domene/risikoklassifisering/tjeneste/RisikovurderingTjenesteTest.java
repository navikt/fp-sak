package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.COLLECTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikogruppeDto;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.KontrollresultatWrapper;

public class RisikovurderingTjenesteTest {

    private final RisikoklassifiseringRepository risikoklassifiseringRepository = mock(RisikoklassifiseringRepository.class);

    private final FpriskTjeneste fpriskTjeneste = mock(FpriskTjeneste.class);

    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);

    private RisikovurderingTjeneste risikovurderingTjeneste;

    private Behandling behandling;

    private BehandlingReferanse referanse;


    @BeforeEach
    public void setup() {
        var scenarioFørstegang = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenarioFørstegang.lagMocked();
        risikovurderingTjeneste = new RisikovurderingTjeneste(risikoklassifiseringRepository,
            behandlingRepository,
            fpriskTjeneste, behandlingskontrollTjeneste);
        referanse = BehandlingReferanse.fra(behandling);
    }

    @Test
    public void skal_teste_at_risikowrapper_lagres_for_en_behandling_som_matcher_uuid() {
        // Arrange
        var uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.of(behandling));
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.empty());

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verify(risikoklassifiseringRepository).lagreRisikoklassifisering(any(), anyLong());
    }

    @Test
    public void skal_teste_at_risikowrapper_ikke_lagres_for_en_behandling_når_det_allerede_finnes_et_resultat() {
        // Arrange
        var uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.of(behandling));
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong()))
            .thenReturn(Optional.of(RisikoklassifiseringEntitet.builder().medKontrollresultat(Kontrollresultat.HØY).buildFor(123L)));

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verify(risikoklassifiseringRepository, times(0)).lagreRisikoklassifisering(any(), anyLong());
    }

    @Test
    public void skal_teste_at_risikowrapper_lagres_for_en_behandling_når_det_allerede_finnes_et_lavt_resultat() {
        // Arrange
        var uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.of(behandling));
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong()))
            .thenReturn(Optional.of(RisikoklassifiseringEntitet.builder().medKontrollresultat(Kontrollresultat.IKKE_HØY).buildFor(123L)));

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verify(risikoklassifiseringRepository).lagreRisikoklassifisering(any(), anyLong());
    }


    @Test
    public void skal_teste_at_risikowrapper_ikke_lagres_når_det_ikke_finnes_behandling_med_matchende_uuid() {
        // Arrange
        var uuid = behandling.getUuid();
        when(behandlingRepository.hentBehandlingHvisFinnes(uuid)).thenReturn(Optional.empty());

        // Act
        risikovurderingTjeneste.lagreKontrollresultat(lagWrapper(uuid, Kontrollresultat.HØY));

        // Assert
        verifyZeroInteractions(risikoklassifiseringRepository);
    }

    @Test
    public void skal_teste_at_vi_returnerer_tom_hvis_ikke_noe_resultat_er_lagret() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.empty());

        // Act
        var faresignalWrapper = risikovurderingTjeneste.hentRisikoklassifisering(referanse);

        // Assert
        assertThat(faresignalWrapper).isNotPresent();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_opprettes_når_risiko_er_høy() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(RisikoklasseType.HØY, Collections.emptyList(), null)));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isTrue();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_når_risiko_er_lav() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(RisikoklasseType.IKKE_HØY, Collections.emptyList(), null)));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    @Test
    public void skal_teste_at_vi_fylller_på_faresignalvurdering_fra_fpsak_om_det_ikke_fins_i_fprisk() {
        // Arrange
        var faresignaler = Arrays.asList("Test 1", "Test 2");
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(RisikoklasseType.HØY, faresignaler, null)));
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.HØY, no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering.AVSLAG_ANNET)));

        // Act
        var risikoklassifisering = risikovurderingTjeneste.hentRisikoklassifisering(referanse);

        // Assert
        assertThat(risikoklassifisering).isPresent();
        assertThat(risikoklassifisering.get().iayFaresignaler().faresignaler()).isEqualTo(faresignaler);
        assertThat(risikoklassifisering.get().medlemskapFaresignaler().faresignaler()).isEqualTo(faresignaler);
        assertThat(risikoklassifisering.get().faresignalVurdering()).isEqualTo(no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering.AVSLAG_ANNET);

    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_det_mangler_kontrollresultat() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.empty());

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(referanse);

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    private RisikoklassifiseringEntitet lagEntitet(Kontrollresultat kontrollresultat,
                                                   no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering vurdering) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).medFaresignalVurdering(vurdering).buildFor(123L);
    }

    private KontrollresultatWrapper lagWrapper(UUID uuid, Kontrollresultat resultat) {
        return new KontrollresultatWrapper(uuid, resultat);
    }

    private RisikovurderingResultatDto lagRespons(RisikoklasseType risikoklasse, List<String> faresignaler, FaresignalVurdering faresignalVurdering) {
        var riskGruppe = new RisikogruppeDto(faresignaler);
        return new RisikovurderingResultatDto(risikoklasse, riskGruppe, riskGruppe, faresignalVurdering);
    }

}
