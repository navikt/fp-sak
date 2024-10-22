package no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import no.nav.vedtak.sikkerhet.abac.beskyttet.AvailabilityType;

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
import no.nav.vedtak.log.util.LoggerUtils;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(VedtakJsonFeedRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class VedtakJsonFeedRestTjeneste {

    static final String BASE_PATH = "/feed/vedtak";
    private static final String FORELDREPENGER_PART_PATH = "/foreldrepenger";
    private static final String SVANGERSKAPSPENGER_PART_PATH = "/svangerskapspenger";

    private static final Logger LOG = LoggerFactory.getLogger(VedtakJsonFeedRestTjeneste.class);

    private VedtakFattetTjeneste tjeneste;

    public VedtakJsonFeedRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, availabilityType = AvailabilityType.ALL)
    public FeedDto fpVedtakHendelser(
            @QueryParam("sistLesteSekvensId") @Parameter(description = "Siste sekvensId lest") @Valid @NotNull SekvensIdParam sistLesteSekvensIdParam,
            @DefaultValue("100") @QueryParam("maxAntall") @Parameter(description = "max antall returnert") @Valid MaxAntallParam maxAntallParam,
            @DefaultValue("") @QueryParam("type") @Parameter(description = "Filtrerer på type hendelse") @Valid HendelseTypeParam hendelseTypeParam,
            @DefaultValue("") @QueryParam("aktoerId") @Parameter(description = "aktoerId") @Valid AktørParam aktørParam) {

        var hendelseType = LoggerUtils.removeLineBreaks(hendelseTypeParam.get());

        var dto = tjeneste.hentFpVedtak(sistLesteSekvensIdParam.get(), maxAntallParam.get(), hendelseType, aktørParam.get());

        if (LOG.isInfoEnabled()) {
            LOG.info("VedtakFeed FP sekvens {} max {} type {} aktør {} antall {}",
                LoggerUtils.toStringWithoutLineBreaks(sistLesteSekvensIdParam.get()),
                LoggerUtils.toStringWithoutLineBreaks(maxAntallParam.get()),
                hendelseType == null ? "notype" : hendelseType,
                aktørParam.get().isPresent() ? "angitt" : "tom",
                dto.getElementer().size());
        }
        return new FeedDto.Builder().medTittel("ForeldrepengerVedtak_v1").medElementer(dto.getElementer())
                .medInneholderFlereElementer(dto.isHarFlereElementer()).build();
    }

    @GET
    @Path(SVANGERSKAPSPENGER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter ut hendelser om svangerskapspenger-vedtak", tags = "feed", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer hendelser om svangerskapspenger-vedtak", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeedDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, availabilityType = AvailabilityType.ALL)
    public FeedDto svpVedtakHendelser(
            @QueryParam("sistLesteSekvensId") @Parameter(description = "Siste sekvensId lest") @Valid @NotNull SekvensIdParam sistLesteSekvensIdParam,
            @DefaultValue("100") @QueryParam("maxAntall") @Parameter(description = "max antall returnert") @Valid MaxAntallParam maxAntallParam,
            @DefaultValue("") @QueryParam("type") @Parameter(description = "Filtrerer på type hendelse") @Valid HendelseTypeParam hendelseTypeParam,
            @DefaultValue("") @QueryParam("aktoerId") @Parameter(description = "aktoerId") @Valid AktørParam aktørParam) {

        var hendelseType = LoggerUtils.removeLineBreaks(hendelseTypeParam.get());

        var dto = tjeneste.hentSvpVedtak(sistLesteSekvensIdParam.get(), maxAntallParam.get(), hendelseType, aktørParam.get());

        if (LOG.isInfoEnabled()) {
            LOG.info("VedtakFeed SVP sekvens {} max {} type {} aktør {} antall {}",
                sistLesteSekvensIdParam.get(), maxAntallParam.get(),
                hendelseType == null ? "notype" : hendelseType,
                aktørParam.get().isPresent() ? "angitt" : "tom",
                dto.getElementer().size());
        }

        return new FeedDto.Builder().medTittel("SVPVedtak_v1").medElementer(dto.getElementer()).medInneholderFlereElementer(dto.isHarFlereElementer())
                .build();
    }
}
