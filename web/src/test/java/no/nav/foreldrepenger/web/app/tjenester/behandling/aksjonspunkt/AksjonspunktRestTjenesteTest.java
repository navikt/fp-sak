package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import jakarta.servlet.http.HttpServletRequest;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AksjonspunktGodkjenningDto;
import no.nav.vedtak.exception.FunksjonellException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class AksjonspunktRestTjenesteTest {

    // skal_håndtere_overlappende_perioder data
    private static final LocalDate now = LocalDate.now();
    private static final UUID behandlingUuid = UUID.randomUUID();
    private static final Long behandlingVersjon = 2L;
    private static final String begrunnelse = "skal_håndtere_overlappende_perioder";
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
    public void setUp() {
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
        aksjonspunkt.add(new BekreftTerminbekreftelseAksjonspunktDto(begrunnelse, termindato, utstedtdato, 1));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, behandlingVersjon, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), anyLong());
    }

    @Test
    void skal_bekrefte_fødsel() throws Exception {
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        var uidentifiserteBarn = new UidentifisertBarnDto[]{new UidentifisertBarnDto(fødselsdato, null)};
        aksjonspunkt.add(new SjekkManglendeFodselDto(begrunnelse, true, false, List.of(uidentifiserteBarn)));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, behandlingVersjon, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), anyLong());
    }

    @Test
    void skal_bekrefte_antall_barn() throws Exception {
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        aksjonspunkt.add(new SjekkManglendeFodselDto(begrunnelse, false, false, new ArrayList<>()));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, behandlingVersjon, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), anyLong());

    }

    @Test
    void skal_bekrefte_fatte_vedtak_med_aksjonspunkt_godkjent() throws Exception {
        when(behandling.getStatus()).thenReturn(BehandlingStatus.FATTER_VEDTAK);
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtos = new ArrayList<>();
        var godkjentAksjonspunkt = opprettetGodkjentAksjonspunkt();
        aksjonspunktGodkjenningDtos.add(godkjentAksjonspunkt);
        aksjonspunkt.add(new FatterVedtakAksjonspunktDto(begrunnelse, aksjonspunktGodkjenningDtos));

        aksjonspunktRestTjeneste.bekreft(mock(HttpServletRequest.class),
            BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, behandlingVersjon, aksjonspunkt));

        verify(aksjonspunktTjenesteMock).bekreftAksjonspunkter(ArgumentMatchers.anyCollection(), anyLong());
    }

    @Test
    void skal_ikke_kunne_bekrefte_andre_aksjonspunkt_ved_status_fatter_vedtak() {
        when(behandling.getStatus()).thenReturn(BehandlingStatus.FATTER_VEDTAK);
        Collection<BekreftetAksjonspunktDto> aksjonspunkt = new ArrayList<>();
        aksjonspunkt.add(new SjekkManglendeFodselDto(begrunnelse, false, false, new ArrayList<>()));
        var dto = BekreftedeAksjonspunkterDto.lagDto(behandlingUuid, behandlingVersjon, aksjonspunkt);
        var request = mock(HttpServletRequest.class);
        assertThrows(FunksjonellException.class, () -> aksjonspunktRestTjeneste.bekreft(request, dto));
    }

    private AksjonspunktGodkjenningDto opprettetGodkjentAksjonspunkt() {
        var endretDto = new AksjonspunktGodkjenningDto();
        endretDto.setAksjonspunktKode(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        endretDto.setGodkjent(true);
        return endretDto;
    }

}
