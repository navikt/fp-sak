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
    endpointProperty = "kelvin.base.url", endpointDefault = "https://aap-api.intern.nav.no",
    scopesProperty = "kelvin.scopes", scopesDefault = "api://prod-gcp.aap.api-intern/.default")
public class KelvinKlient {

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI endpointKunKelvinAAP;
    @SuppressWarnings("unused")
    private final URI endpointMaksimumAAP;

    public KelvinKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.endpointKunKelvinAAP = UriBuilder.fromUri(restConfig.endpoint()).path("/kelvin/maksimumUtenUtbetaling").build();
        this.endpointMaksimumAAP = UriBuilder.fromUri(restConfig.endpoint()).path("/maksimum").build();
    }

    // TODO: Utvide med metode for å hente utbetalte perioder fra Kelvin dersom overlapp av rettighetsperioder. Så det skilles mellom utbetalt og framtidig
    public LocalDateTimeline<BigDecimal> hentAAPVedtaksTidslinje(PersonIdent ident, LocalDate fom, LocalDate tom) {
        var body = new KelvinRequest(ident.getIdent(), fom, tom);
        var request = RestRequest.newPOSTJson(body, endpointKunKelvinAAP, restConfig);
        return restClient.sendReturnOptional(request, KelvinArbeidsavklaringspengerResponse.class)
            .map(KelvinArbeidsavklaringspengerResponse::vedtak).orElseGet(List::of).stream()
            .filter(v -> KelvinArbeidsavklaringspengerResponse.Kildesystem.KELVIN.equals(v.kildesystem()))
            .map(KelvinArbeidsavklaringspengerResponse.AAPVedtak::periode)
            .map(p -> new LocalDateSegment<>(p.fraOgMedDato(), p.tilOgMedDato(), BigDecimal.TEN.multiply(BigDecimal.TEN)))
            .collect(Collectors.collectingAndThen(Collectors.toList(), l -> new LocalDateTimeline<>(l, StandardCombinators::max)));
    }

    public record KelvinRequest(String personidentifikator, LocalDate fraOgMedDato, LocalDate tilOgMedDato) { }


}
