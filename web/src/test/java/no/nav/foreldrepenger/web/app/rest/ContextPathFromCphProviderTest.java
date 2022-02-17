package no.nav.foreldrepenger.web.app.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.sikkerhet.ContextPathHolder;

class ContextPathFromCphProviderTest {

    @Test
    void returnerer_fra_context_path_provider() {
        var provider = new ContextPathFromCphProvider();

        var cp = "cp";
        ContextPathHolder.instance(cp);

        assertThat(provider.get()).isEqualTo(cp);
    }

}
