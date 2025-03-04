package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.InnsynVedtaksdokumentasjonDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Produces(MediaType.APPLICATION_JSON)
@Path(InnsynRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class InnsynRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String INNSYN_PART_PATH = "/innsyn";
    public static final String INNSYN_PATH = BASE_PATH + INNSYN_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private InnsynRepository innsynRepository;
    private VedtakTjeneste vedtakTjeneste;

    public InnsynRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InnsynRestTjeneste(BehandlingRepository behandlingRepository,
            InnsynRepository innsynRepository,
            VedtakTjeneste vedtakTjeneste) {
        this.vedtakTjeneste = vedtakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.innsynRepository = innsynRepository;
    }

    @GET
    @Path(INNSYN_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent diverse informasjon om innsynsbehandlingen", summary = "Returnerer info om innsynsbehandling", tags = "innsyn", responses = {@ApiResponse(responseCode = "200", description = "Returnerer innsynsbehandling eller ingenting hvis uuid ikke peker på en innsynsbehandling", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InnsynsbehandlingDto.class)))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response getInnsynsbehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        var dto = mapFra(behandling);
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    private InnsynsbehandlingDto mapFra(Behandling behandling) {
        var dto = new InnsynsbehandlingDto();
        var lagreteVedtak = vedtakTjeneste.hentLagreteVedtakPåFagsak(behandling.getFagsakId());

        var innsynOpt = innsynRepository.hentForBehandling(behandling.getId());
        if (innsynOpt.isEmpty() && lagreteVedtak.isEmpty()) {
            return null; // quick return
        }

        if (innsynOpt.isPresent()) {
            var innsyn = innsynOpt.get();

            dto.setInnsynMottattDato(innsyn.getMottattDato());
            dto.setInnsynResultatType(innsyn.getInnsynResultatType());

            List<InnsynDokumentDto> doks = new ArrayList<>();
            if (innsyn.getInnsynDokumenterOld() != null) {
                for (var innsynDokument : innsyn.getInnsynDokumenterOld()) {
                    var dokumentDto = new InnsynDokumentDto();
                    dokumentDto.setDokumentId(innsynDokument.getDokumentId());
                    dokumentDto.setJournalpostId(innsynDokument.getJournalpostId().getVerdi());
                    dokumentDto.setFikkInnsyn(innsynDokument.isFikkInnsyn());

                    doks.add(dokumentDto);
                }
            }

            dto.setDokumenter(doks);
        }

        lagreteVedtak.forEach(vedtak -> {
            var b = behandlingRepository.hentBehandling(vedtak.getBehandlingId());
            var behandlingUuid = b.getUuid();
            var tittel = b.getType().getKode();
            var opprettetDato = vedtak.getOpprettetTidspunkt().toLocalDate();
            var vedtaksdokumentasjonDto = new InnsynVedtaksdokumentasjonDto(behandlingUuid, tittel, opprettetDato);
            dto.getVedtaksdokumentasjon().add(vedtaksdokumentasjonDto);
        });

        return dto;

    }

}
