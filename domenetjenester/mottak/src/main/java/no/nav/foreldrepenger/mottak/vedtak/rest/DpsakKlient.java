package no.nav.foreldrepenger.mottak.vedtak.rest;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC,
    endpointProperty = "dagpengerdatadeling.base.url", endpointDefault = "https://dp-datadeling.intern.nav.no",
    scopesProperty = "dagpengerdatadeling.scopes", scopesDefault = "api://prod-gcp.teamdagpenger.dp-datadeling/.default")
public class DpsakKlient {

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI perioderEndpoint;

    public DpsakKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(DpsakKlient.class);
        this.perioderEndpoint = UriBuilder.fromUri(restConfig.endpoint()).path("/dagpenger/datadeling/v1/perioder").build();
    }

    public LocalDateTimeline<BigDecimal> hentRettighetsperioder(PersonIdent personIdent, LocalDate fom, LocalDate tom) {
        var prequest = new PersonRequest(personIdent.getIdent(), fom, tom);
        var rrequest = RestRequest.newPOSTJson(prequest, perioderEndpoint, restConfig);
        return restClient.sendReturnOptional(rrequest, DagpengerRettighetsperioder.class)
            .map(DagpengerRettighetsperioder::perioder).orElseGet(List::of).stream()
            .filter(v -> DagpengerRettighetsperioder.DagpengerKilde.DP_SAK.equals(v.kilde()))
            .map(p -> new LocalDateSegment<>(p.fraOgMedDato(), p.tilOgMedDato(), BigDecimal.TEN.multiply(BigDecimal.TEN)))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, StandardCombinators::max)));
    }

    public record PersonRequest(String personIdent, LocalDate fraOgMedDato, LocalDate tilOgMedDato) { }


}
