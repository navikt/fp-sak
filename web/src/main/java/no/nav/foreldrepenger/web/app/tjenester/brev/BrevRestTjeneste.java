package no.nav.foreldrepenger.web.app.tjenester.brev;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.BrevBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(BrevRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class BrevRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevRestTjeneste.class);

    static final String BASE_PATH = "/brev";
    private static final String VARSEL_REVURDERING_PART_PATH = "/varsel/revurdering";
    public static final String VARSEL_REVURDERING_PATH = BASE_PATH + VARSEL_REVURDERING_PART_PATH;
    private static final String MANUELL_BREV_VIS_PART_PATH = "/forhandsvis/manuell";
    public static final String MANUELL_BREV_VIS_PATH = BASE_PATH + MANUELL_BREV_VIS_PART_PATH;
    private static final String BREV_BESTILL_PART_PATH = "/bestill";
    public static final String BREV_BESTILL_PATH = BASE_PATH + BREV_BESTILL_PART_PATH;

    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentBestillerTjeneste dokumentBestillerTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository) {
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path(BREV_BESTILL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public void bestillDokument(@TilpassetAbacAttributt(supplierClass = BrevAbacDataSupplier.class) @Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillBrevDto bestillBrevDto) {
        var behandlingId = behandlingRepository.hentBehandling(bestillBrevDto.behandlingUuid()).getId();
        LOG.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.brevmalkode(), behandlingId);

        var brevBestilling = BrevBestilling.builder()
            .medBehandlingUuid(bestillBrevDto.behandlingUuid())
            .medDokumentMal(bestillBrevDto.brevmalkode())
            .medRevurderingÅrsak(bestillBrevDto.arsakskode())
            .medFritekst(bestillBrevDto.fritekst())
            .build();

        dokumentBestillerTjeneste.bestillDokument(brevBestilling, HistorikkAktør.SAKSBEHANDLER);
        oppdaterBehandlingBasertPåManueltBrev(bestillBrevDto.brevmalkode(), behandlingId);
    }

    @POST
    @Path(MANUELL_BREV_VIS_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Forhåndsviser et manuell brev (sendt av saksbehandler fra GUI)", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public void forhåndsvisManuelDokument(@TilpassetAbacAttributt(supplierClass = BrevAbacDataSupplier.class) @Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillBrevDto bestillBrevDto) {
        LOG.info("Brev med brevmalkode={} forhåndsvist på behandlingUuid={}", bestillBrevDto.brevmalkode(), bestillBrevDto.behandlingUuid());

        var brevBestilling = BrevBestilling.builder()
            .medBehandlingUuid(bestillBrevDto.behandlingUuid())
            .medDokumentMal(bestillBrevDto.brevmalkode())
            .medRevurderingÅrsak(bestillBrevDto.arsakskode())
            .medFritekst(bestillBrevDto.fritekst())
            .build();

       // dokumentBestillerTjeneste.bestillDokument(brevBestilling);
    }

    private void oppdaterBehandlingBasertPåManueltBrev(DokumentMalType brevmalkode, Long behandlingId) {
        if (DokumentMalType.VARSEL_OM_REVURDERING.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_RESPONS_REVURDERING, behandlingId);
        } else if (DokumentMalType.INNHENTE_OPPLYSNINGER.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_DOK, behandlingId);
        } else if (DokumentMalType.ETTERLYS_INNTEKTSMELDING.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.VENT_OPDT_INNTEKTSMELDING, behandlingId);
        } else if (DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManuelt(behandlingId);
        } else if (DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManueltMedlemskap(behandlingId);
        }
    }

    private void settBehandlingPåVent(Venteårsak avvResponsRevurdering, Long behandlingId) {
        dokumentBehandlingTjeneste.settBehandlingPåVent(behandlingId, avvResponsRevurdering);
    }

    @POST
    @Path("/kvittering")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kvitterer at brevet ble produsert og sendt. BREV_SENT historikk blir lagt og behandling dokument blir oppdatert med journalpostId.", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public void kvittering(@TilpassetAbacAttributt(supplierClass = DokumentProdusertDataSupplier.class) @Valid no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto kvittering) {
        dokumentBehandlingTjeneste.kvitterBrevSent(kvittering);
    }

    @GET
    @Path(VARSEL_REVURDERING_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Boolean harSendtVarselOmRevurdering(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.VARSEL_OM_REVURDERING);
    }

    public static class BrevAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BestillBrevDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

    public static class DokumentProdusertDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

}
