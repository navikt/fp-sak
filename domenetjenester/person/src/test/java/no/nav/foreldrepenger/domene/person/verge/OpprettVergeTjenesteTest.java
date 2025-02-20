package no.nav.foreldrepenger.domene.person.verge;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
import no.nav.foreldrepenger.domene.person.verge.dto.OpprettVergeDto;

import no.nav.foreldrepenger.domene.typer.AktørId;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    VergeRepository vergeRepository;

    private Behandling behandling;

    @BeforeEach
    public void oppsett() {
        behandling = opprettBehandling();
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
            behandling.getFagsakId(), dto);

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag).satisfies(h -> {
            assertThat(h.getBehandlingId()).isEqualTo(behandling.getId());
            assertThat(h.getFagsakId()).isEqualTo(behandling.getFagsakId());
            assertThat(h.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
            assertThat(h.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(h.getTekstLinjer()).hasSize(2).containsExactly("Registrering av opplysninger om verge/fullmektig.", "Begrunnelse.");
        });
    }

    @Test
    void skal_generere_historikkinnslag_for_oppdatering_av_verge() {
        var dto = opprettDtoVerge();
        var aktørId = AktørId.dummy();

        when(vergeRepository.hentAggregat(any())).thenReturn(Optional.of(new VergeAggregat(new VergeEntitet.Builder().medVergeType(VergeType.BARN)
            .gyldigPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024,12,31))
            .medBruker(NavBruker.opprettNyNB(aktørId))
            .build())));

        when(personinfoAdapter.hentBrukerArbeidsgiverForAktør(any())).thenReturn(Optional.of(new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId)
            .medFødselsdato(LocalDate.of(2000, 1, 1))
            .medPersonIdent(PersonIdent.fra("12345678910"))
            .medNavn("Harald")
            .build()));


        new OpprettVergeTjeneste(personinfoAdapter, brukerTjeneste, vergeRepository, historikkReposistory).opprettVerge(behandling.getId(),
            behandling.getFagsakId(), dto);

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkReposistory).lagre(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag).satisfies(h -> {
            assertThat(h.getBehandlingId()).isEqualTo(behandling.getId());
            assertThat(h.getFagsakId()).isEqualTo(behandling.getFagsakId());
            assertThat(h.getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE);
            assertThat(h.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
            assertThat(h.getTekstLinjer()).hasSize(5)
                .containsExactly("__Navn__ er endret fra Harald til __Harald Hårfagre__.",
                    "__Fødselsnummer__ er endret fra 12345678910 til __12345678901__.",
                    "__Periode f.o.m.__ er endret fra 01.01.2024 til __01.01.2025__.",
                    "__Periode t.o.m.__ er endret fra 31.12.2024 til __31.12.2025__.", "Begrunnelse.");
        });
    }

    private OpprettVergeDto opprettDtoVerge() {
        return new OpprettVergeDto("Harald Hårfagre", "12345678901", LocalDate.of(2025,1,1), LocalDate.of(2025,12,31), VergeType.BARN, null,
            "Begrunnelse");
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagMocked();

        return scenario.getBehandling();
    }

}

