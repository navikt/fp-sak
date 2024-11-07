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
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentForhandsvisning;
import no.nav.foreldrepenger.dokumentbestiller.DokumentForhåndsvisningTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentKvittering;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillDokumentDto;
import no.nav.foreldrepenger.dokumentbestiller.dto.ForhåndsvisDokumentDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentKvitteringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.StandardAbacAttributtType;
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
    private static final String BREV_VIS_PART_PATH = "/forhandsvis";
    public static final String BREV_VIS_PATH = BASE_PATH + BREV_VIS_PART_PATH;
    private static final String BREV_BESTILL_PART_PATH = "/bestill";
    public static final String BREV_BESTILL_PATH = BASE_PATH + BREV_BESTILL_PART_PATH;

    private DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjeneste,
                            DokumentBestillerTjeneste dokumentBestillerTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository,
                            ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.dokumentForhåndsvisningTjeneste = dokumentForhåndsvisningTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    @POST
    @Path(BREV_BESTILL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public void bestillDokument(@TilpassetAbacAttributt(supplierClass = BrevAbacDataSupplier.class) @Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillDokumentDto bestillBrevDto) {
        var behandling = behandlingRepository.hentBehandling(bestillBrevDto.behandlingUuid());
        LOG.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.brevmalkode(), behandling.getId());

        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(bestillBrevDto.behandlingUuid())
            .medDokumentMal(bestillBrevDto.brevmalkode())
            .medRevurderingÅrsak(bestillBrevDto.arsakskode())
            .medFritekst(bestillBrevDto.fritekst())
            .build();

        if (DokumentMalType.ETTERLYS_INNTEKTSMELDING.equals(bestillBrevDto.brevmalkode())) {
            validerFinnesManglendeInntektsmelding(behandling);
        }

        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling, HistorikkAktør.SAKSBEHANDLER);
        oppdaterBehandlingBasertPåManueltBrev(bestillBrevDto.brevmalkode(), behandling.getId());
    }

    private void validerFinnesManglendeInntektsmelding(Behandling behandling) {
        var statuser = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(BehandlingReferanse.fra(behandling));
        var finnesManglendeIM = statuser.stream()
            .anyMatch(st -> st.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT));
        if (!finnesManglendeIM) {
            throw new IllegalStateException(
                "Det er ikke tillatt å bestille etterlys inntektsmeldingsbrev på en behandling som ikke mangler inntektsmelding. BehandlingId: "
                    + behandling.getId());
        }
    }

    @POST
    @Path(BREV_VIS_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer en pdf som er en forhåndsvisning av brevet", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response forhåndsvisDokument(@Parameter(description = "Inneholder kode til brevmal og bestillingsdetaljer.") @TilpassetAbacAttributt(supplierClass = ForhåndsvisSupplier.class) @Valid ForhåndsvisDokumentDto forhåndsvisDto) { // NOSONAR

        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(forhåndsvisDto.behandlingUuid())
            .medDokumentMal(forhåndsvisDto.dokumentMal())
            .medRevurderingÅrsak(forhåndsvisDto.arsakskode())
            .medFritekst(forhåndsvisDto.fritekst())
            .medTittel(forhåndsvisDto.tittel())
            .medDokumentType(utledDokumentType(forhåndsvisDto.automatiskVedtaksbrev()))
            .build();

        if (DokumentMalType.ETTERLYS_INNTEKTSMELDING.equals(forhåndsvisDto.dokumentMal())) {
            var behandling = behandlingRepository.hentBehandling(forhåndsvisDto.behandlingUuid());
            validerFinnesManglendeInntektsmelding(behandling);
        }

        var dokument = dokumentForhåndsvisningTjeneste.forhåndsvisDokument(bestilling);
        if (dokument != null && dokument.length != 0) {
            var responseBuilder = Response.ok(dokument);
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=dokument.pdf");
            return responseBuilder.build();
        }
        return Response.serverError().build();
    }

    private DokumentForhandsvisning.DokumentType utledDokumentType(boolean gjelderAutomatiskBrev) {
        return gjelderAutomatiskBrev ? DokumentForhandsvisning.DokumentType.AUTOMATISK : DokumentForhandsvisning.DokumentType.OVERSTYRT;
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
        } else if (DokumentMalType.forlengetSaksbehandlingstidMedlemskap().contains(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManueltMedlemskap(behandlingId);
        }
    }

    private void settBehandlingPåVent(Venteårsak avvResponsRevurdering, Long behandlingId) {
        dokumentBehandlingTjeneste.settBehandlingPåVent(behandlingId, avvResponsRevurdering);
    }

    @POST
    @Path("/kvittering/v3")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Kvitterer at brevet ble produsert og sendt. BREV_SENT historikk blir lagt og behandling dokument blir oppdatert med journalpostId.", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK)
    public void kvitteringV3(@TilpassetAbacAttributt(supplierClass = DokumentKvitteringDataSupplier.class) @Valid DokumentKvitteringDto kvitto) {
        dokumentBehandlingTjeneste.kvitterSendtBrev(
            new DokumentKvittering(kvitto.behandlingUuid(), kvitto.dokumentbestillingUuid(), kvitto.journalpostId(), kvitto.dokumentId()));
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
            var req = (BestillDokumentDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

    public static class DokumentKvitteringDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (DokumentKvitteringDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

    public static class ForhåndsvisSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (ForhåndsvisDokumentDto) obj;
            return AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid());
        }
    }

}
