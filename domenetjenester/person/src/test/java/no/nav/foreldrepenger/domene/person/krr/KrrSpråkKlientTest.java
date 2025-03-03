package no.nav.foreldrepenger.domene.person.krr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class KrrSpråkKlientTest {

    private KrrSpråkKlient krrSpråkKlient;
    private final RestClient restClient = Mockito.mock(RestClient.class);

    @BeforeEach
    public void setup() { krrSpråkKlient = new KrrSpråkKlient(restClient);
    }

    @Test
    void krrResponsMappesTilSpråkkode() {
        forventMappingFraKrrResponsTilSpråkkode(Språkkode.NB, Språkkode.NB);
        forventMappingFraKrrResponsTilSpråkkode(Språkkode.NN, Språkkode.NN);
        forventMappingFraKrrResponsTilSpråkkode(Språkkode.EN, Språkkode.EN);

        forventMappingFraKrrResponsTilSpråkkode(Språkkode.UDEFINERT, Språkkode.NB);
    }

    @Test
    void default_bokmål_ved_person_ikke_funnet_i_pdl() {
        // biblioteket mapper 404 til IntegrasjonException
        when(restClient.send(any(), any())).thenReturn(new Kontaktinformasjoner(null, Map.of("123", Kontaktinformasjoner.FeilKode.person_ikke_funnet)));
        var språk = krrSpråkKlient.finnSpråkkodeForBruker("123");
        assertThat(språk).isEqualTo(Språkkode.NB);
    }

    @Test
    void default_bokmål_når_person_finne_i_pdl_men_mangler_kontaktinfo_i_KRR() {
        // biblioteket mapper 404 til IntegrasjonException
        when(restClient.send(any(), any())).thenReturn(new Kontaktinformasjoner(Map.of("123", new Kontaktinformasjoner.Kontaktinformasjon(false, null)), null));
        var språk = krrSpråkKlient.finnSpråkkodeForBruker("123");
        assertThat(språk).isEqualTo(Språkkode.NB);
    }

    @Test
    void propagerExceptionVedIntegrasjonExceptionUlik404() {
        when(restClient.send(any(), any())).thenThrow(new IntegrasjonException("B", "Noe annet feil."));
        assertThatThrownBy(()-> krrSpråkKlient.finnSpråkkodeForBruker("123")).isInstanceOf(IntegrasjonException.class);
    }

    private void forventMappingFraKrrResponsTilSpråkkode(Språkkode responsFraKrr, Språkkode språkkode) {
        when(restClient.send(any(), any())).thenReturn(
            new Kontaktinformasjoner(Map.of("123", new Kontaktinformasjoner.Kontaktinformasjon(true, responsFraKrr.getKode())), null));
        var språk = krrSpråkKlient.finnSpråkkodeForBruker("123");
        assertThat(språk).isEqualTo(språkkode);
    }

}
