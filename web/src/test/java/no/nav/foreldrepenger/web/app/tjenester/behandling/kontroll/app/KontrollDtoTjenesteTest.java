package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;

class KontrollDtoTjenesteTest {

    private KontrollDtoTjeneste kontrollDtoTjeneste;

    private RisikovurderingTjeneste risikovurderingTjeneste = mock(RisikovurderingTjeneste.class);

    private Behandling behandling;

    private BehandlingReferanse referanse;

    @BeforeEach
    void setup() {
        var scenarioKlage = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenarioKlage.lagMocked();
        referanse = BehandlingReferanse.fra(behandling);
        kontrollDtoTjeneste = new KontrollDtoTjeneste(risikovurderingTjeneste);
    }

    @Test
    void skal_teste_at_dto_lages_korrekt_når_resultatet_mangler() {
        // Arrange
        when(risikovurderingTjeneste.hentRisikoklassifisering(referanse)).thenReturn(Optional.empty());

        // Act
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(referanse);

        // Assert
        assertThat(kontrollresultatDto).isPresent();
        assertThat(kontrollresultatDto.get().kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_KLASSIFISERT);
    }

    @Test
    void skal_teste_at_dto_lages_korrekt_når_resultatet_viser_ikke_høy() {
        // Arrange
        when(risikovurderingTjeneste.hentRisikoklassifisering(referanse))
                .thenReturn(Optional.of(lagFaresignalWrapper(Kontrollresultat.IKKE_HØY, Collections.emptyList())));

        // Act
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(referanse);

        // Assert
        assertThat(kontrollresultatDto).isPresent();
        assertThat(kontrollresultatDto.get().kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(kontrollresultatDto.get().medlFaresignaler()).isNull();
        assertThat(kontrollresultatDto.get().iayFaresignaler()).isNull();

    }

    @Test
    void skal_teste_at_dto_lages_korrekt_når_resultatet_viser_høy() {
        // Arrange
        var faresignaler = Arrays.asList("Grunn en", "Grunn to", "Grunn tre", "Grunn 4", "Grunn 5");
        when(risikovurderingTjeneste.hentRisikoklassifisering(referanse))
                .thenReturn(Optional.of(lagFaresignalWrapper(Kontrollresultat.HØY, faresignaler)));

        // Act
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(referanse);

        // Assert
        assertThat(kontrollresultatDto).isPresent();
        assertThat(kontrollresultatDto.get().kontrollresultat()).isEqualTo(Kontrollresultat.HØY);
        assertThat(kontrollresultatDto.get().medlFaresignaler()).isNotNull();
        assertThat(kontrollresultatDto.get().medlFaresignaler().faresignaler()).containsAll(faresignaler);
        assertThat(kontrollresultatDto.get().iayFaresignaler()).isNotNull();
        assertThat(kontrollresultatDto.get().iayFaresignaler().faresignaler()).containsAll(faresignaler);
    }

    private FaresignalWrapper lagFaresignalWrapper(Kontrollresultat kontrollresultat, List<String> faresignaler) {
        if (!faresignaler.isEmpty()) {
            var iayBuilder = new FaresignalGruppeWrapper(faresignaler);
            var medlBuilder = new FaresignalGruppeWrapper(faresignaler);
            return new FaresignalWrapper(kontrollresultat, null, medlBuilder, iayBuilder);
        }
        return new FaresignalWrapper(kontrollresultat, null, null, null);
    }
}
