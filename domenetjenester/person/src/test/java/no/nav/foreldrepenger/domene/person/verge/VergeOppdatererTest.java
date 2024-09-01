package no.nav.foreldrepenger.domene.person.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(MockitoExtension.class)
class VergeOppdatererTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_VERGE;

    @Mock
    private HistorikkTjenesteAdapter historikkTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private NavBrukerTjeneste brukerTjeneste;

    private NavBruker vergeBruker;

    @BeforeEach
    public void oppsett() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        @SuppressWarnings("unused") var behandling = scenario.lagMocked();

        vergeBruker = NavBruker.opprettNyNB(AktørId.dummy());

        lenient().when(personinfoAdapter.hentAktørForFnr(Mockito.any())).thenReturn(Optional.of(AktørId.dummy()));
        lenient().when(brukerTjeneste.hentEllerOpprettFraAktørId(Mockito.any())).thenReturn(vergeBruker);
    }

    @Test
    void skal_generere_historikkinnslag_ved_bekreftet() {
        // Behandling
        var behandling = opprettBehandling();
        var dto = opprettDtoVerge();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        new VergeOppdaterer(historikkTjeneste, personinfoAdapter, mock(VergeRepository.class), brukerTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Verifiserer HistorikkinnslagDto
        var historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkTjeneste).lagInnslag(historikkCapture.capture());
        var historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.REGISTRER_OM_VERGE);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        var del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke").hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE.getKode()));
        assertThat(del.getHendelse()).as("hendelse").hasValueSatisfying(hendelse -> assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.REGISTRER_OM_VERGE.getKode()));
    }

    private AvklarVergeDto opprettDtoVerge() {
        var dto = new AvklarVergeDto();
        dto.setNavn("Navn");
        dto.setFnr("12345678901");
        dto.setGyldigFom(LocalDate.now().minusDays(10));
        dto.setGyldigTom(LocalDate.now().plusDays(10));
        dto.setVergeType(VergeType.BARN);
        return dto;
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagMocked();

        return scenario.getBehandling();
    }

}

