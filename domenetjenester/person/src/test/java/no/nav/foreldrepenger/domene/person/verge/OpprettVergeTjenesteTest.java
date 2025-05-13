package no.nav.foreldrepenger.domene.person.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;

@ExtendWith(MockitoExtension.class)
class OpprettVergeTjenesteTest {

    @Mock
    private HistorikkinnslagRepository historikkReposistory;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private NavBrukerTjeneste brukerTjeneste;
    @Mock
    VergeRepository vergeRepository;

    private Behandling behandling;

    @BeforeEach
    void oppsett() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();

        var vergeBruker = NavBruker.opprettNyNB(behandling.getAktørId());
        when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(behandling.getAktørId()));
        when(brukerTjeneste.hentEllerOpprettFraAktørId(any())).thenReturn(vergeBruker);

    }

    @Test
    void skal_generere_historikkinnslag_for_ny_verge() {
        // Behandling
        var dto = opprettDtoVerge();
        when(vergeRepository.hentAggregat(any())).thenReturn(Optional.empty());
        new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, vergeRepository, historikkReposistory).opprettVerge(behandling.getId(),
            behandling.getFagsakId(), dto, "Begrunnelse");

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag).satisfies(h -> {
            assertThat(h.getBehandlingId()).isEqualTo(behandling.getId());
            assertThat(h.getFagsakId()).isEqualTo(behandling.getFagsakId());
            assertThat(h.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
            assertThat(h.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(h.getTekstLinjer()).hasSize(2).containsExactly("Opplysninger om verge/fullmektig er registrert.", "Begrunnelse.");
        });
    }

    @Test
    void skal_generere_historikkinnslag_for_endring_av_verge() {
        var dto = opprettDtoVerge();

        when(vergeRepository.hentAggregat(any())).thenReturn(
            Optional.of(new VergeAggregat(new VergeEntitet.Builder().medVergeType(VergeType.BARN).build())));


        new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, vergeRepository, historikkReposistory).opprettVerge(behandling.getId(),
            behandling.getFagsakId(), dto, "Begrunnelse");

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag).satisfies(h -> {
            assertThat(h.getBehandlingId()).isEqualTo(behandling.getId());
            assertThat(h.getFagsakId()).isEqualTo(behandling.getFagsakId());
            assertThat(h.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
            assertThat(h.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(h.getTekstLinjer()).hasSize(2).containsExactly("Opplysninger om verge/fullmektig er endret.", "Begrunnelse.");
        });
    }

    private VergeDto opprettDtoVerge() {
        return VergeDto.person(VergeType.BARN, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            "Harald Hårfagre", "12345678901");
    }
}

