package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

public class HentKodeverkTjenesteImplTest {

    private no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste enhetsTjeneste = new BehandlendeEnhetTjeneste();

    @Test
    public void skal_filtere_arbeidtyper() {
        var kodeverk = new HentKodeverkTjeneste(enhetsTjeneste);

        var resultat = kodeverk.hentGruppertKodeliste();
        var arbeidType = resultat.get("ArbeidType");

        assertThat(arbeidType).hasSize(6);
    }
}
