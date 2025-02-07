package no.nav.foreldrepenger.web.app.rest;

import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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
    void skal_erstatte_pathparmeters_for_path_template() {
        var path = "/api/{string}/{long}/{uuid}";
        var parameters = new PathParamMap().add("string", "test-string")
            .add("long", 123L)
            .add("uuid", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        var result = Path.of(path, parameters).build();
        assertThat("/api/test-string/123/123e4567-e89b-12d3-a456-426614174000").isEqualTo(result);
    }

    @Test
    void addPathPrefix() {
        assertThat(ResourceLinks.addPathPrefix("/behandling")).isEqualTo("/fpsak/api/behandling");
    }

    @Test
    void skal_feile_ved_manglende_path_param() {
        var path = "/api/behandling/{behandlingId}";
        var parameters = new PathParamMap();

        assertThatIllegalArgumentException().isThrownBy(() -> Path.of(path, parameters).build())
            .withMessage("The template variable 'behandlingId' has no value");
    }
}
