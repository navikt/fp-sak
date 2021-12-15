package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.risikoklassifisering.json.KontrollresultatMapper;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.KontrollresultatWrapper;

public class RisikovurderingTjenesteTest {

    private final RisikoklassifiseringRepository risikoklassifiseringRepository = mock(RisikoklassifiseringRepository.class);

    private final FpriskTjeneste fpriskTjeneste = mock(FpriskTjeneste.class);

    private final KontrollresultatMapper mapper = mock(KontrollresultatMapper.class);

    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);

    private RisikovurderingTjeneste risikovurderingTjeneste;

    private Behandling behandling;


    @BeforeEach
    public void setup() {
        var scenarioFørstegang = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenarioFørstegang.lagMocked();
        risikovurderingTjeneste = new RisikovurderingTjeneste(risikoklassifiseringRepository,
            behandlingRepository,
            fpriskTjeneste,
            mapper, behandlingskontrollTjeneste);
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
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.empty());

        // Act
        var faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(faresignalWrapper).isNotPresent();
        verifyZeroInteractions(fpriskTjeneste);
    }

    @Test
    public void skal_teste_at_vi_ikke_henter_resultat_fra_fprisk_ved_ikke_høy_risiko() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.IKKE_HØY)));

        // Act
        var faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(faresignalWrapper).isPresent();
        assertThat(faresignalWrapper.get().kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(faresignalWrapper.get().medlemskapFaresignaler()).isNull();
        assertThat(faresignalWrapper.get().iayFaresignaler()).isNull();
        verifyZeroInteractions(mapper);
        verifyZeroInteractions(fpriskTjeneste);
    }

    @Test
    public void skal_teste_at_vi_henter_resultat_fra_fprisk_ved_høy_risiko() {
        // Arrange
        var uuid = behandling.getUuid();
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.HØY)));
        var respons = new RisikovurderingResultatDto(RisikoklasseType.HØY, null, null, null);
        when(fpriskTjeneste.hentFaresignalerForBehandling(uuid)).thenReturn(Optional.of(respons));
        when(mapper.fraFaresignalRespons(any())).thenReturn(new FaresignalWrapper(Kontrollresultat.HØY, null, null, null));

        // Act
        var faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(faresignalWrapper).isPresent();
        verify(fpriskTjeneste).hentFaresignalerForBehandling(uuid);
        verify(mapper).fraFaresignalRespons(respons);
    }

    @Test
    public void skal_teste_at_aksjonspunkt_opprettes_når_risiko_er_høy() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.HØY)));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(behandling.getId());

        // Assert
        assertThat(skalOppretteAksjonspunkt).isTrue();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_når_risiko_er_lav() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.of(lagEntitet(Kontrollresultat.IKKE_HØY)));

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(behandling.getId());

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    @Test
    public void skal_teste_at_aksjonspunkt_ikke_opprettes_det_mangler_kontrollresultat() {
        // Arrange
        when(risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(anyLong())).thenReturn(Optional.empty());

        // Act
        var skalOppretteAksjonspunkt = risikovurderingTjeneste.skalVurdereFaresignaler(behandling.getId());

        // Assert
        assertThat(skalOppretteAksjonspunkt).isFalse();
    }

    private RisikoklassifiseringEntitet lagEntitet(Kontrollresultat kontrollresultat) {
        return RisikoklassifiseringEntitet.builder().medKontrollresultat(kontrollresultat).buildFor(123L);
    }

    private KontrollresultatWrapper lagWrapper(UUID uuid, Kontrollresultat resultat) {
        return new KontrollresultatWrapper(uuid, resultat);
    }
}
