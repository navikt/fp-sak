package no.nav.foreldrepenger.web.app.rest;

import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLinksTest {

    @Test
    void toQuery() {
        var uuid = "e79acf0f-1ccf-4046-9f71-4eb8288f3f5f";
        var saksnummer = "12345678";
        assertThat(ResourceLinks.toQuery(null)).isEmpty();
        assertThat(ResourceLinks.toQuery(new SaksnummerDto(saksnummer))).isEqualTo("?saksnummer=" + saksnummer);
        assertThat(ResourceLinks.toQuery(new BehandlingIdDto(uuid))).isEqualTo("?behandlingUuid=" + uuid);
        assertThat(ResourceLinks.toQuery(new UuidDto("e79acf0f-1ccf-4046-9f71-4eb8288f3f5f"))).isEqualTo("?uuid=" + uuid);
        assertThat(ResourceLinks.toQuery(Map.of("behandlingVersjon", 3L, "saksnummer", saksnummer))).startsWith("?")
            .contains("saksnummer=" + saksnummer)
            .contains("&")
            .contains("behandlingVersjon=3");
    }

    @Test
    void addPathPrefix() {
        assertThat(ResourceLinks.addPathPrefix("/behandling")).isEqualTo("/fpsak/api/behandling");
    }
}
