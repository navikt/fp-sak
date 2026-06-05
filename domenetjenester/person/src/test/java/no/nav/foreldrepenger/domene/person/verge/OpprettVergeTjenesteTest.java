package no.nav.foreldrepenger.domene.person.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import no.nav.vedtak.felles.integrasjon.organisasjon.OrgInfo;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonEReg;

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
    @Mock
    private OrgInfo eregKlient;

    private Behandling behandling;
    private OpprettVergeTjeneste tjeneste;

    @BeforeEach
    void oppsett() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        tjeneste = new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, vergeRepository, historikkReposistory, eregKlient);
    }

    private void stubPersonVerge() {
        var vergeBruker = NavBruker.opprettNyNB(behandling.getAktørId());
        when(personinfoAdapter.hentAktørForFnr(any())).thenReturn(Optional.of(behandling.getAktørId()));
        when(brukerTjeneste.hentEllerOpprettFraAktørId(any())).thenReturn(vergeBruker);
    }

    @Test
    void skal_generere_historikkinnslag_for_ny_verge() {
        stubPersonVerge();
        var dto = opprettDtoVerge();
        when(vergeRepository.hentAggregat(any())).thenReturn(Optional.empty());
        tjeneste.opprettVerge(behandling.getId(), behandling.getFagsakId(), dto, "Begrunnelse");

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
        stubPersonVerge();
        var dto = opprettDtoVerge();

        when(vergeRepository.hentAggregat(any())).thenReturn(
            Optional.of(new VergeAggregat(new VergeEntitet.Builder().medVergeType(VergeType.BARN).build())));

        tjeneste.opprettVerge(behandling.getId(), behandling.getFagsakId(), dto, "Begrunnelse");

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

    @Test
    void skal_opprette_verge_advokat_når_orgnummer_finnes_i_ereg() {
        var orgnummer = "974760673";
        var dto = VergeDto.organisasjon(VergeType.ADVOKAT, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            "Advokatfirmaet AS", orgnummer);
        when(vergeRepository.hentAggregat(any())).thenReturn(Optional.empty());
        when(eregKlient.hentOrganisasjon(eq(orgnummer))).thenReturn(new OrganisasjonEReg(orgnummer, null, null, null, null));

        tjeneste.opprettVerge(behandling.getId(), behandling.getFagsakId(), dto, null);

        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        assertThat(historikkCapture.getValue().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
    }

    @Test
    void skal_feile_når_orgnummer_ikke_finnes_i_ereg() {
        var orgnummer = "000000000";
        var dto = VergeDto.organisasjon(VergeType.ADVOKAT, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            "Ukjent firma", orgnummer);
        when(eregKlient.hentOrganisasjon(eq(orgnummer))).thenThrow(new RuntimeException("404 Not Found"));

        assertThatThrownBy(() -> tjeneste.opprettVerge(behandling.getId(), behandling.getFagsakId(), dto, null))
            .isInstanceOf(RuntimeException.class);
    }

    private VergeDto opprettDtoVerge() {
        return VergeDto.person(VergeType.BARN, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            "Harald Hårfagre", "12345678901");
    }
}

