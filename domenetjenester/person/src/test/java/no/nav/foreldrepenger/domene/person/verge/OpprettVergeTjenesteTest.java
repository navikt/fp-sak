package no.nav.foreldrepenger.domene.person.verge;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.OpprettVergeDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettVergeTjenesteTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_VERGE;

    @Mock
    private HistorikkinnslagRepository historikkReposistory;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private NavBrukerTjeneste brukerTjeneste;

    private Behandling behandling;

    @BeforeEach
    public void oppsett() {
        behandling = opprettBehandling();
        var vergeBruker = NavBruker.opprettNyNB(behandling.getAktørId());
        when(personinfoAdapter.hentAktørForFnr(Mockito.any())).thenReturn(Optional.of(behandling.getAktørId()));
        when(brukerTjeneste.hentEllerOpprettFraAktørId(Mockito.any())).thenReturn(vergeBruker);

    }

    @Test
    void skal_generere_historikkinnslag_for_ny_verge() {
        // Behandling
        var dto = opprettDtoVerge();

        new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, mock(VergeRepository.class), historikkReposistory)
                .opprettVerge(behandling.getId(), behandling.getFagsakId(), dto);

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag).satisfies(h -> {
            assertThat(h.getBehandlingId()).isEqualTo(behandling.getId());
            assertThat(h.getFagsakId()).isEqualTo(behandling.getFagsakId());
            assertThat(h.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
            assertThat(h.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(h.getTekstLinjer())
                    .hasSize(2)
                    .containsExactly("Registrering av opplysninger om verge/fullmektig.", "Begrunnelse.");
        });
    }

    @Test
    void skal_generere_historikkinnslag_for_oppdatering_av_verge() {
        // Behandling
        var dto = opprettDtoVerge();

        new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, mock(VergeRepository.class), historikkReposistory)
                .opprettVerge(behandling.getId(), behandling.getFagsakId(), dto);

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag).satisfies(h -> {
            assertThat(h.getBehandlingId()).isEqualTo(behandling.getId());
            assertThat(h.getFagsakId()).isEqualTo(behandling.getFagsakId());
            assertThat(h.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
            assertThat(h.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(h.getTekstLinjer())
                    .hasSize(2)
                    .containsExactly("Registrering av opplysninger om verge/fullmektig.", "Begrunnelse.");
        });
    }

    private OpprettVergeDto opprettDtoVerge() {
        return new OpprettVergeDto(
                "Navn",
                "12345678901",
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(10),
                VergeType.BARN,
                null,
                "Begrunnelse"
        );
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagMocked();

        return scenario.getBehandling();
    }

}

