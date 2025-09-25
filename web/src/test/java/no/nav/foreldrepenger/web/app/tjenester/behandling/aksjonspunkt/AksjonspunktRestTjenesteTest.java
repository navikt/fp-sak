package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.SjekkTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.SjekkManglendeFødselAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FastsetteUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.vedtak.exception.FunksjonellException;

class AksjonspunktRestTjenesteTest {

    // skal_håndtere_overlappende_perioder data
    private static final LocalDate now = LocalDate.now();
    private static final UUID behandlingUuid = UUID.randomUUID();
    private static final Long BEHANDLING_VERSJON = 2L;
    private static final String BEGRUNNELSE = "skal_håndtere_overlappende_perioder";
    private static final LocalDate fødselsdato = now.plusDays(40);
    private static final LocalDate termindato = now.plusDays(30);
    private static final LocalDate utstedtdato = now.minusDays(10);
    private final AksjonspunktTjeneste aksjonspunktTjenesteMock = mock(AksjonspunktTjeneste.class);
    private final BehandlingsutredningTjeneste behandlingsutredningTjenesteMock = mock(BehandlingsutredningTjeneste.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final BehandlingsresultatRepository behandlingsresultatRepository = mock(BehandlingsresultatRepository.class);
    private final Behandling behandling = mock(Behandling.class);
    private final TotrinnTjeneste totrinnTjeneste = mock(TotrinnTjeneste.class);
    private AksjonspunktRestTjeneste aksjonspunktRestTjeneste;

    @BeforeEach
    void setUp() {
        when(behandling.getUuid()).thenReturn(UUID.randomUUID());
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(behandling);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(behandling.getStatus()).thenReturn(BehandlingStatus.OPPRETTET);
        doNothing().when(behandlingsutredningTjenesteMock).kanEndreBehandling(any(), anyLong());
        aksjonspunktRestTjeneste = new AksjonspunktRestTjeneste(aksjonspunktTjenesteMock, behandlingRepository, behandlingsresultatRepository,
            behandlingsutredningTjenesteMock, totrinnTjeneste);

    }

    @Test
    void skal_bekrefte_terminbekreftelse() throws Exception {
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        aksjonspunkt.add(new SjekkTerminbekreftelseAksjonspunktDto(BEGRUNNELSE, termindato, utstedtdato, 1));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), any(), any());
    }

    @Test
    void skal_bekrefte_fødsel() throws Exception {
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        var uidentifiserteBarn = new DokumentertBarnDto[]{new DokumentertBarnDto(fødselsdato, null)};
        aksjonspunkt.add(new SjekkManglendeFødselAksjonspunktDto(BEGRUNNELSE, List.of(uidentifiserteBarn)));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), any(), any());
    }

    @Test
    void skal_bekrefte_antall_barn() throws Exception {
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        aksjonspunkt.add(new SjekkManglendeFødselAksjonspunktDto(BEGRUNNELSE, new ArrayList<>()));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), any(), any());

    }

    @Test
    void skal_bekrefte_fatte_vedtak_med_aksjonspunkt_godkjent() throws Exception {
        when(behandling.getStatus()).thenReturn(BehandlingStatus.FATTER_VEDTAK);
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos = new ArrayList<>();
        var godkjentAksjonspunkt = opprettetGodkjentAksjonspunkt();
        aksjonspunktGodkjenningDtos.add(godkjentAksjonspunkt);
        aksjonspunkt.add(new FatterVedtakAksjonspunktDto(BEGRUNNELSE, aksjonspunktGodkjenningDtos));

        aksjonspunktRestTjeneste.beslutt(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), any(), any());
    }

    @Test
    void skal_ikke_kunne_bekrefte_andre_aksjonspunkt_ved_status_fatter_vedtak() {
        when(behandling.getStatus()).thenReturn(BehandlingStatus.FATTER_VEDTAK);
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        aksjonspunkt.add(new SjekkManglendeFødselAksjonspunktDto(BEGRUNNELSE, new ArrayList<>()));
        var dto = BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON, aksjonspunkt);
        var request = mock(HttpServletRequest.class);
        assertThrows(FunksjonellException.class, () -> aksjonspunktRestTjeneste.bekreft(request, dto));
    }

    @Test
    void skal_kunne_sende_fatte_vedtak_til_beslutter_endepunkt() throws URISyntaxException {
        aksjonspunktRestTjeneste.beslutt(mock(HttpServletRequest.class), BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON,
            List.of(new FatterVedtakAksjonspunktDto(BEGRUNNELSE, List.of(new AksjonspunktGodkjenningDto())))));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), any(), any());
    }

    @Test
    void skal_ikke_kunne_sende_andre_ap_til_beslutter_endepunkt() {
        var dto = BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON,
            List.of(new FastsetteUttakDto.FastsetteUttakPerioderDto(List.of())));
        assertThatThrownBy(() -> aksjonspunktRestTjeneste.beslutt(mock(HttpServletRequest.class), dto)).isExactlyInstanceOf(
            IllegalArgumentException.class);
    }

    @Test
    void skal_ikke_kunne_sende_fatter_vedtak_ap_til_aksjonspunkt_endepunkt() {
        var dto = BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, BEHANDLING_VERSJON, List.of(new FatterVedtakAksjonspunktDto()));
        assertThatThrownBy(() -> aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class), dto)).isExactlyInstanceOf(
            IllegalArgumentException.class);
    }

    private AksjonspunktGodkjenningDto opprettetGodkjentAksjonspunkt() {
        var endretDto = new AksjonspunktGodkjenningDto();
        endretDto.setAksjonspunktKode(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        endretDto.setGodkjent(true);
        return endretDto;
    }

}
