package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.K27PatchDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.KvitteringDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragPatchDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningOppdrag")
@ApplicationScoped
@Transactional
public class ForvaltningOppdragRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningOppdragRestTjeneste.class);

    private ForvaltningOppdragTjeneste forvaltningOppdragTjeneste;

    public ForvaltningOppdragRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningOppdragRestTjeneste(ForvaltningOppdragTjeneste forvaltningOppdragTjeneste) {
        this.forvaltningOppdragTjeneste = forvaltningOppdragTjeneste;
    }

    @POST
    @Path("/kvitter-oppdrag-ok")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Kvitterer oppdrag manuelt. Brukes kun når det er avklart at oppdrag har gått OK, og kvittering ikke kommer til å komme fra Oppdragsystemet. Sjekk med Team Ukelønn hvis i tvil", tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response kvitterOK(
            @Parameter(description = "Identifikasjon av oppdrag som kvitteres OK. Sett oppdaterProsessTask til false kun når prosesstasken allerede er flyttet til FERDIG") @NotNull @Valid KvitteringDto kvitteringDto) {
        forvaltningOppdragTjeneste.kvitterOk(
            kvitteringDto.getBehandlingId(),
            kvitteringDto.getFagsystemId(),
            kvitteringDto.getOppdaterProsessTask());
        return Response.ok().build();
    }

    @POST
    @Path("/patch-oppdrag")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Patcher oppdrag som har feilet fordi fpsak har generert det på feil måte, og sender over til oppdragsysstemet. Sjekk med Team Ukelønn hvis i tvil. Viktig at det sjekkes i Oppdragsystemet etter oversending at alt har gått som forventet", tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response patchOppdrag(@NotNull @Valid OppdragPatchDto dto) {
        forvaltningOppdragTjeneste.patchOppdrag(dto);
        return Response.ok("Patchet oppdrag for behandling=" + dto.getBehandlingId()).build();
    }

    @POST
    @Path("/patch-oppdrag-hardt-og-rekjoer")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Som /patch-oppdrag, men kan også patche når behandling er ferdig. Sjekk med Team Foreldrepenger hvis i tvil. Viktig at det sjekkes i Oppdragsystemet etter oversending at alt har gått som forventet", tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response patchOppdragOgRekjør(@NotNull @Valid OppdragPatchDto dto) {
        forvaltningOppdragTjeneste.patchOppdragOgRekjør(dto);
        return Response.ok("Patchet oppdrag for behandling=" + dto.getBehandlingId()).build();
    }

    @POST
    @Path("/patch-k27")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Patcher oppdrag som har feil i maks dato ved refusjon til AG, og sender over til oppdragsysstemet. Sjekk med Team FP hvis i tvil. Viktig at det sjekkes i Oppdragsystemet etter oversending at alt har gått som forventet", tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response patchK27(@NotNull @Valid K27PatchDto dto) {
        forvaltningOppdragTjeneste.patchk27(dto.getBehandlingId(), dto.getFagsystemId(), dto.getMaksDato());
        return Response.ok("Patchet oppdrag for behandling=" + dto.getBehandlingId()).build();
    }
}
