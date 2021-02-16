package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingBrevDtoTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentProdusertDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(BrevRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class BrevRestTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrevRestTjeneste.class);

    static final String BASE_PATH = "/brev";
    private static final String DOKUMENT_SENDT_PART_PATH = "/dokument-sendt";
    public static final String DOKUMENT_SENDT_PATH = BASE_PATH + DOKUMENT_SENDT_PART_PATH; // NOSONAR TFP-2234
    private static final String VARSEL_REVURDERING_PART_PATH = "/varsel/revurdering";
    public static final String VARSEL_REVURDERING_PATH = BASE_PATH + VARSEL_REVURDERING_PART_PATH;
    private static final String BREV_BESTILL_PART_PATH = "/bestill";
    public static final String BREV_BESTILL_PATH = BASE_PATH + BREV_BESTILL_PART_PATH;
    private static final String BREV_RESSURSER__PART_PATH = "/ressurser";
    public static final String BREV_RESSURSER_PATH = BASE_PATH + BREV_RESSURSER__PART_PATH;

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingBrevDtoTjeneste behandlingBrevDtoTjeneste;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository,
                            BehandlingBrevDtoTjeneste behandlingBrevDtoTjeneste) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingBrevDtoTjeneste = behandlingBrevDtoTjeneste;
    }

    @POST
    @Path(BREV_BESTILL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public void bestillDokument(
            @Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillBrevDto bestillBrevDto) { // NOSONAR
        // FIXME: behandlingUuid brukes mot fp-formidling og kan derfor også brukes mot
        // fpsak-frontend her
        LOGGER.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.getBrevmalkode(), bestillBrevDto.getBehandlingId());
        dokumentBestillerTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER, true);
        oppdaterBehandlingBasertPåManueltBrev(DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getBehandlingId());
    }

    private void oppdaterBehandlingBasertPåManueltBrev(DokumentMalType brevmalkode, Long behandlingId) {
        if (DokumentMalType.REVURDERING_DOK.equals(brevmalkode) || DokumentMalType.VARSEL_OM_REVURDERING.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_RESPONS_REVURDERING, behandlingId);
        } else if (DokumentMalType.INNHENT_DOK.equals(brevmalkode) || DokumentMalType.INNHENTE_OPPLYSNINGER.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_DOK, behandlingId);
        } else if (DokumentMalType.FORLENGET_DOK.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManuelt(behandlingId);
        } else if (DokumentMalType.FORLENGET_MEDL_DOK.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManueltMedlemskap(behandlingId);
        }
    }

    private void settBehandlingPåVent(Venteårsak avvResponsRevurdering, Long behandlingId) {
        dokumentBehandlingTjeneste.settBehandlingPåVent(behandlingId, avvResponsRevurdering);
    }

    @POST
    @Path(DOKUMENT_SENDT_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekker om dokument for mal er sendt", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Boolean harProdusertDokument(@Valid DokumentProdusertDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.fraKode(dto.getDokumentMal())); // NOSONAR
    }

    @GET
    @Path(VARSEL_REVURDERING_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Boolean harSendtVarselOmRevurdering(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.REVURDERING_DOK)
            || dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING); // NOSONAR
    }

    @GET
    @Path(BREV_RESSURSER_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent behandling med tilhørende ressurslenker for bruk i brev", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentBehandlingDtoForBrev(@NotNull @Parameter(description = "Id eller UUID for behandlingen") @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) {
        if (behandlingIdDto.getBehandlingUuid() != null) {
            var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingIdDto.getBehandlingUuid());
            var dto = behandling.map(value -> behandlingBrevDtoTjeneste.lagDtoForBrev(value)).orElse(null);
            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        } else {
            var behandling = behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingId());
            var dto = behandling != null ? behandlingBrevDtoTjeneste.lagDtoForBrev(behandling) : null;
            Response.ResponseBuilder responseBuilder = Response.ok().entity(dto);
            return responseBuilder.build();
        }
    }

}
