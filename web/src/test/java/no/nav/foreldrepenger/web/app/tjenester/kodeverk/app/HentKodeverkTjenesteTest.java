package no.nav.foreldrepenger.web.app.tjenester.kodeverk.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

public class HentKodeverkTjenesteTest {

    @Test
    public void skal_filtere_arbeidtyper() {
        var tjeneste = new HentKodeverkTjeneste(new BehandlendeEnhetTjeneste());

        var resultat = tjeneste.hentGruppertKodeliste();
        var arbeidType = resultat.get("ArbeidType");

        assertThat(arbeidType).hasSize(6);
    }
}
