package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;

import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

class FpInntektsmeldingKlientTest {

    @Test
    void skal_gi_funksjonell_feil_når_notifikasjon_finnes_fra_før() {
        var råFeilmelding = "Funksjonel feil ved opprettelse av ny beskjed: Funksjonellfeil:"
            + "notifikasjon med angitt eksternId og merkelapp finnes fra før";
        var klient = klientSomFeilerMed(råFeilmelding);
        var request = new NyBeskjedRequest(new OrganisasjonsnummerDto("999999999"), new SaksnummerDto("1234"));

        assertThatThrownBy(() -> klient.sendNyBeskjedPåForespørsel(request))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det er allerede sendt en beskjed til arbeidsgiveren i dag.")
            .hasMessageNotContaining(råFeilmelding);
    }

    @Test
    void skal_beholde_teknisk_feil_for_andre_feilmeldinger() {
        var råFeilmelding = "Tilkoblingen feilet";
        var klient = klientSomFeilerMed(råFeilmelding);
        var request = new NyBeskjedRequest(new OrganisasjonsnummerDto("999999999"), new SaksnummerDto("1234"));

        assertThatThrownBy(() -> klient.sendNyBeskjedPåForespørsel(request))
            .isInstanceOf(TekniskException.class)
            .hasMessageContaining("Feil ved kall til Fpinntektsmelding")
            .hasMessageNotContaining(råFeilmelding);
    }

    private static FpInntektsmeldingKlient klientSomFeilerMed(String feilmelding) {
        var restClient = mock(RestClient.class);
        when(restClient.send(any(RestRequest.class), eq(SendNyBeskjedResponse.class)))
            .thenThrow(new RuntimeException(feilmelding));

        var uri = URI.create("http://localhost");
        var restConfig = new RestConfig(TokenFlow.NO_AUTH_NEEDED, uri, null, uri);
        var uriBuilder = mock(UriBuilder.class);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(uri);
        try (var restClientMock = mockStatic(RestClient.class);
             var restConfigMock = mockStatic(RestConfig.class);
             var uriBuilderMock = mockStatic(UriBuilder.class)) {
            restClientMock.when(RestClient::client).thenReturn(restClient);
            restConfigMock.when(() -> RestConfig.forClient(FpInntektsmeldingKlient.class)).thenReturn(restConfig);
            uriBuilderMock.when(() -> UriBuilder.fromUri(uri)).thenReturn(uriBuilder);
            return new FpInntektsmeldingKlient();
        }
    }
}
