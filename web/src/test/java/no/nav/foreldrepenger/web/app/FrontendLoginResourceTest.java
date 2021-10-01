package no.nav.foreldrepenger.web.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

class FrontendLoginResourceTest {
    private FrontendLoginResource resource = new FrontendLoginResource();

    @SuppressWarnings("resource")
    @Test
    public void skal_hente_ut_relative_path_from_url() {
        Response response = resource.login("http://google.com/asdf");
        assertThat(response.getLocation()).hasPath("/asdf");

        response = resource.login("/");
        assertThat(response.getLocation()).hasPath("/");

        response = resource.login("/fagsak/1234/behandling/1234/opptjening");
        assertThat(response.getLocation()).hasPath("/fagsak/1234/behandling/1234/opptjening");

        response = resource.login("fagsak/1234/behandling/1234/opptjening");
        assertThat(response.getLocation()).hasPath("/fagsak/1234/behandling/1234/opptjening");
    }

    @SuppressWarnings("resource")
    @Test
    public void innlogging_fra_fpsak_sak_web() {
        var basepath = "https://app.adeo.no";
        var response = resource.login(basepath);
        assertThat(URI.create("/")).isEqualTo(response.getLocation());

        var hovedside = "/fpsak/";
        response = resource.login(hovedside);
        assertThat(URI.create(hovedside)).isEqualTo(response.getLocation());

        var medQuery = "/fpsak/web/fagsak/1234/behandling/?collapsed=true";
        response = resource.login(medQuery);
        assertThat(URI.create(medQuery)).isEqualTo(response.getLocation());

        var medFragment = "/fpsak/fagsak/1234/behandling/#panel-42";
        response = resource.login(medFragment);
        assertThat(URI.create(medFragment)).isEqualTo(response.getLocation());

        var medQueryOgFragment = "/fpsak/fagsak/1234/behandling/?collapsed=true#panel42";
        response = resource.login(medQueryOgFragment);
        assertThat(URI.create(medQueryOgFragment)).isEqualTo(response.getLocation());
    }
}
