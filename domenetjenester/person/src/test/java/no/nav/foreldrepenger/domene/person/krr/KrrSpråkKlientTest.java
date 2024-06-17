package no.nav.foreldrepenger.domene.person.krr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.krr.KrrSpråkKlient.KrrRespons;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class KrrSpråkKlientTest {

    private KrrSpråkKlient krrSpråkKlient;
    private final RestClient restClient = Mockito.mock(RestClient.class);

    private static final KrrRespons KRR_NYNORSK = new KrrRespons("NN");
    private static final KrrRespons KRR_BOKMÅL = new KrrRespons("NB");
    private static final KrrRespons KRR_ENGELSK = new KrrRespons("EN");
    private static final KrrRespons KRR_UKJENT_VERDI = new KrrRespons("X");


    @BeforeEach
    public void setup() {
        krrSpråkKlient = new KrrSpråkKlient(restClient);
    }

    @Test
    void krrResponsMappesTilSpråkkode() {
        forventMappingFraKrrResponsTilSpråkkode(KRR_BOKMÅL, Språkkode.NB);
        forventMappingFraKrrResponsTilSpråkkode(KRR_NYNORSK, Språkkode.NN);
        forventMappingFraKrrResponsTilSpråkkode(KRR_ENGELSK, Språkkode.EN);

        forventMappingFraKrrResponsTilSpråkkode(KRR_UKJENT_VERDI, Språkkode.NB);
    }

    @Test
    void defaultBokmålVed404() {
        // biblioteket mapper 404 til IntegrasjonException
        when(restClient.sendReturnOptional(any(), any())).thenThrow(new IntegrasjonException("A", "Fant ikke, så her har du en 404."));
        var språk = krrSpråkKlient.finnSpråkkodeForBruker("123");
        assertThat(språk).isEqualTo(Språkkode.NB);
    }

    @Test
    void propagerExceptionVedIntegrasjonExceptionUlik404() {
        when(restClient.sendReturnOptional(any(), any())).thenThrow(new IntegrasjonException("B", "Noe annet feil."));
        assertThatThrownBy(() -> krrSpråkKlient.finnSpråkkodeForBruker("123")).isInstanceOf(IntegrasjonException.class);
    }

    private void forventMappingFraKrrResponsTilSpråkkode(KrrRespons responsFraKrr, Språkkode språkkode) {
        when(restClient.sendReturnOptional(any(), any())).thenReturn(Optional.of(responsFraKrr));
        var språk = krrSpråkKlient.finnSpråkkodeForBruker("123");
        assertThat(språk).isEqualTo(språkkode);
    }

}
