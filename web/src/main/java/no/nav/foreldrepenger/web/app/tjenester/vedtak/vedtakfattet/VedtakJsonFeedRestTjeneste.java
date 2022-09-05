package no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.jsonfeed.VedtakFattetTjeneste;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedDto;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.AktørParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.HendelseTypeParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.MaxAntallParam;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.SekvensIdParam;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(VedtakJsonFeedRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class VedtakJsonFeedRestTjeneste {

    static final String BASE_PATH = "/feed/vedtak";
    private static final String FORELDREPENGER_PART_PATH = "/foreldrepenger";
    public static final String FORELDREPENGER_PATH = BASE_PATH + FORELDREPENGER_PART_PATH; // NOSONAR TFP-2234
    private static final String SVANGERSKAPSPENGER_PART_PATH = "/svangerskapspenger";
    public static final String SVANGERSKAPSPENGER_PATH = BASE_PATH + SVANGERSKAPSPENGER_PART_PATH; // NOSONAR TFP-2234

    private static final Logger LOG = LoggerFactory.getLogger(VedtakJsonFeedRestTjeneste.class);

    private VedtakFattetTjeneste tjeneste;

    public VedtakJsonFeedRestTjeneste() {
    }

    @Inject
    public VedtakJsonFeedRestTjeneste(VedtakFattetTjeneste vedtakFattetTjeneste) {
        this.tjeneste = vedtakFattetTjeneste;
    }

    @GET
    @Path(FORELDREPENGER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter ut hendelser om foreldrepenger-vedtak", tags = "feed", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hendelser om foreldrepenger-vedtak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeedDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public FeedDto fpVedtakHendelser(
            @QueryParam("sistLesteSekvensId") @Parameter(description = "Siste sekvensId lest") @Valid @NotNull SekvensIdParam sistLesteSekvensIdParam,
            @DefaultValue("100") @QueryParam("maxAntall") @Parameter(description = "max antall returnert") @Valid MaxAntallParam maxAntallParam,
            @DefaultValue("") @QueryParam("type") @Parameter(description = "Filtrerer på type hendelse") @Valid HendelseTypeParam hendelseTypeParam,
            @DefaultValue("") @QueryParam("aktoerId") @Parameter(description = "aktoerId") @Valid AktørParam aktørParam) {
        final var dto = tjeneste.hentFpVedtak(sistLesteSekvensIdParam.get(), maxAntallParam.get(), hendelseTypeParam.get(), aktørParam.get());
        LOG.info("VedtakFeed FP sekvens {} max {} type {} aktør {} antall {}", sistLesteSekvensIdParam.get(), maxAntallParam.get(),
                hendelseTypeParam.get() == null ? "notype" : hendelseTypeParam.get(), aktørParam.get().isPresent() ? "angitt" : "tom",
                dto.getElementer().size());
        return new FeedDto.Builder().medTittel("ForeldrepengerVedtak_v1").medElementer(dto.getElementer())
                .medInneholderFlereElementer(dto.isHarFlereElementer()).build();
    }

    @GET
    @Path(SVANGERSKAPSPENGER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter ut hendelser om svangerskapspenger-vedtak", tags = "feed", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hendelser om svangerskapspenger-vedtak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeedDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public FeedDto svpVedtakHendelser(
            @QueryParam("sistLesteSekvensId") @Parameter(description = "Siste sekvensId lest") @Valid @NotNull SekvensIdParam sistLesteSekvensIdParam,
            @DefaultValue("100") @QueryParam("maxAntall") @Parameter(description = "max antall returnert") @Valid MaxAntallParam maxAntallParam,
            @DefaultValue("") @QueryParam("type") @Parameter(description = "Filtrerer på type hendelse") @Valid HendelseTypeParam hendelseTypeParam,
            @DefaultValue("") @QueryParam("aktoerId") @Parameter(description = "aktoerId") @Valid AktørParam aktørParam) {
        final var dto = tjeneste.hentSvpVedtak(sistLesteSekvensIdParam.get(), maxAntallParam.get(), hendelseTypeParam.get(), aktørParam.get());
        LOG.info("VedtakFeed SVP sekvens {} max {} type {} aktør {} antall {}", sistLesteSekvensIdParam.get(), maxAntallParam.get(),
                hendelseTypeParam.get() == null ? "notype" : hendelseTypeParam.get(), aktørParam.get().isPresent() ? "angitt" : "tom",
                dto.getElementer().size());
        return new FeedDto.Builder().medTittel("SVPVedtak_v1").medElementer(dto.getElementer()).medInneholderFlereElementer(dto.isHarFlereElementer())
                .build();
    }
}
