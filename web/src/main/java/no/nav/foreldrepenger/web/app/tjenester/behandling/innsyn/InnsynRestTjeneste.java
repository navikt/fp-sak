package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakMedBehandlingType;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.VedtaksdokumentasjonDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

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
    @Operation(description = "Hent diverse informasjon om innsynsbehandlingen",
        summary = ("Returnerer info om innsynsbehandling"),
        tags = "innsyn",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer innsynsbehandling eller ingenting hvis uuid ikke peker på en innsynsbehandling",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = InnsynsbehandlingDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getInnsynsbehandling(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        InnsynsbehandlingDto dto = mapFra(behandling);
        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    private InnsynsbehandlingDto mapFra(Behandling behandling) {
        InnsynsbehandlingDto dto = new InnsynsbehandlingDto();
        List<LagretVedtakMedBehandlingType> lagreteVedtak = vedtakTjeneste.hentLagreteVedtakPåFagsak(behandling.getFagsakId());

        Optional<InnsynEntitet> innsynOpt = innsynRepository.hentForBehandling(behandling.getId());
        if (!innsynOpt.isPresent() && lagreteVedtak.isEmpty()) {
            return null; // quick return
        }

        if (innsynOpt.isPresent()) {
            InnsynEntitet innsyn = innsynOpt.get();

            dto.setInnsynMottattDato(innsyn.getMottattDato());
            dto.setInnsynResultatType(innsyn.getInnsynResultatType());

            List<InnsynDokumentDto> doks = new ArrayList<>();
            if (innsyn.getInnsynDokumenterOld() != null) {
                for (InnsynDokumentEntitet innsynDokument : innsyn.getInnsynDokumenterOld()) {
                    InnsynDokumentDto dokumentDto = new InnsynDokumentDto();
                    dokumentDto.setDokumentId(innsynDokument.getDokumentId());
                    dokumentDto.setJournalpostId(innsynDokument.getJournalpostId().getVerdi());
                    dokumentDto.setFikkInnsyn(innsynDokument.isFikkInnsyn());

                    doks.add(dokumentDto);
                }
            }

            dto.setDokumenter(doks);
        }

        lagreteVedtak.forEach(lagretVedtakMedBehandlingType -> {
            VedtaksdokumentasjonDto vedtaksdokumentasjonDto = new VedtaksdokumentasjonDto();
            vedtaksdokumentasjonDto.setDokumentId(lagretVedtakMedBehandlingType.getId().toString());
            vedtaksdokumentasjonDto.setOpprettetDato(lagretVedtakMedBehandlingType.getOpprettetDato());
            vedtaksdokumentasjonDto.setTittel(lagretVedtakMedBehandlingType.getBehandlingType());
            dto.getVedtaksdokumentasjon().add(vedtaksdokumentasjonDto);
        });

        return dto;

    }

}
