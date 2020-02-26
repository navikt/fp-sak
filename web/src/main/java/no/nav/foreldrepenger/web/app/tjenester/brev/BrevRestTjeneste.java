package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.dto.BrevmalDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentProdusertDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(BrevRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transaction
public class BrevRestTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrevRestTjeneste.class);

    static final String BASE_PATH = "/brev";
    private static final String MALER_PART_PATH = "/maler";
    public static final String MALER_PATH = BASE_PATH + MALER_PART_PATH;
    private static final String DOKUMENT_SENDT_PART_PATH = "/dokument-sendt";
    public static final String DOKUMENT_SENDT_PATH = BASE_PATH + DOKUMENT_SENDT_PART_PATH; //NOSONAR TFP-2234
    private static final String VARSEL_REVURDERING_PART_PATH = "/varsel/revurdering";
    public static final String VARSEL_REVURDERING_PATH = BASE_PATH + VARSEL_REVURDERING_PART_PATH;
    private static final String BREV_BESTILL_PART_PATH = "/bestill";
    public static final String BREV_BESTILL_PATH = BASE_PATH + BREV_BESTILL_PART_PATH;

    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository) {
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path(BREV_BESTILL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void bestillDokument(@Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillBrevDto bestillBrevDto) { // NOSONAR
        // FIXME: bør støttes behandlingUuid i fp-formidling
        LOGGER.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.getBrevmalkode(), bestillBrevDto.getBehandlingId());
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER, true);
        oppdaterBehandlingBasertPåManueltBrev(DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getBehandlingId());
    }

    private void oppdaterBehandlingBasertPåManueltBrev(DokumentMalType brevmalkode, Long behandlingId) {
        if (DokumentMalType.REVURDERING_DOK.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_RESPONS_REVURDERING, behandlingId);
        } else if (DokumentMalType.INNHENT_DOK.equals(brevmalkode)) {
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

    @GET
    @Path(MALER_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter liste over tilgjengelige brevtyper", tags = "brev")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BrevmalDto> hentMaler(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        LOGGER.info("Utvikler-feil: Gammel tjeneste for henting av brevmaler via Fpsak ble kalt. Skal ikke skje etter TFP-1404 på fpsak-frontend - gi beskjed til Jan Erik!");
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.hentBrevmalerFor(behandling.getId());
    }

    @POST
    @Path(DOKUMENT_SENDT_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekker om dokument for mal er sendt", tags = "brev")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harProdusertDokument(@Valid DokumentProdusertDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.fraKode(dto.getDokumentMal())); // NOSONAR
    }

    @GET
    @Path(VARSEL_REVURDERING_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harSendtVarselOmRevurdering(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(), DokumentMalType.REVURDERING_DOK); // NOSONAR
    }
}
