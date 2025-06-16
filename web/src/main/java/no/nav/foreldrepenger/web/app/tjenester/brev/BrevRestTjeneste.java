package no.nav.foreldrepenger.web.app.tjenester.brev;

import java.util.Objects;
import java.util.UUID;
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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
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
import no.nav.foreldrepenger.dokumentbestiller.dto.FritekstDto;
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
    private static final String BREV_HENT_OVERSTYRING_PART_PATH = "/overstyring";
    public static final String BREV_HENT_OVERSTYRING_PATH = BASE_PATH + BREV_HENT_OVERSTYRING_PART_PATH;
    private static final String BREV_MELLOMLAGRE_OVERSTYRING_PART_PATH = "/overstyring/mellomlagring";
    public static final String BREV_MELLOMLAGRE_OVERSTYRING_PATH = BASE_PATH + BREV_MELLOMLAGRE_OVERSTYRING_PART_PATH;

    private DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjeneste,
                            DokumentBestillerTjeneste dokumentBestillerTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository,
                            ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                            HistorikkinnslagRepository historikkinnslagRepository) {
        this.dokumentForhåndsvisningTjeneste = dokumentForhåndsvisningTjeneste;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @POST
    @Path(BREV_BESTILL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public void bestillDokument(@TilpassetAbacAttributt(supplierClass = BrevAbacDataSupplier.class) @Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid BestillDokumentDto bestillBrevDto) {
        behandlingRepository.taSkriveLås(bestillBrevDto.behandlingUuid());
        var behandling = behandlingRepository.hentBehandling(bestillBrevDto.behandlingUuid());
        LOG.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.brevmalkode(), behandling.getId());

        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(bestillBrevDto.behandlingUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(bestillBrevDto.brevmalkode())
            .medRevurderingÅrsak(bestillBrevDto.arsakskode())
            .medFritekst(bestillBrevDto.fritekst() != null ? bestillBrevDto.fritekst().verdi() : null)
            .build();

        if (DokumentMalType.ETTERLYS_INNTEKTSMELDING.equals(bestillBrevDto.brevmalkode())) {
            validerFinnesManglendeInntektsmelding(behandling);
        }

        opprettHistorikkinnslag(behandling, dokumentBestilling);
        dokumentBestillerTjeneste.bestillDokument(dokumentBestilling);
        oppdaterBehandlingBasertPåManueltBrev(bestillBrevDto.brevmalkode(), behandling.getId());
    }

    private void opprettHistorikkinnslag(Behandling behandling,
                                        DokumentBestilling bestilling) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel("Brev er bestilt")
            .addLinje(utledBegrunnelse(bestilling.dokumentMal(), bestilling.journalførSom()))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static String utledBegrunnelse(DokumentMalType dokumentMal, DokumentMalType journalførSom) {
        if (DokumentMalType.FRITEKSTBREV.equals(dokumentMal) || DokumentMalType.FRITEKSTBREV_HMTL.equals(dokumentMal)) {
            Objects.requireNonNull(journalførSom, "journalførSom må være satt om FRITEKST brev brukes.");
            return journalførSom.getNavn() + " (" + dokumentMal.getNavn() + ")";
        }
        return dokumentMal.getNavn();
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

    @GET
    @Path(BREV_HENT_OVERSTYRING_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter html representasjon av brevet som brukes i overstyring av vedtaksbrev med mellomlagret overstyring", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentOverstyringAvBrevMedOrginaltBrevPåHtmlFormat(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var dokument = dokumentForhåndsvisningTjeneste.genererHtml(behandling);
        if (dokument == null || dokument.isEmpty()) {
            return Response.serverError().build();
        }

        var eksiterendeOverstyring = dokumentBehandlingTjeneste.hentMellomlagretOverstyring(behandling.getId());
        var overstyrtBrev = new OverstyrtDokumentDto(dokument, eksiterendeOverstyring.orElse(null));
        return Response.ok(overstyrtBrev)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }

    public record OverstyrtDokumentDto(String opprinneligHtml, String redigertHtml) {
    }

    @POST
    @Path(BREV_MELLOMLAGRE_OVERSTYRING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer ned overstyrt html-representasjon av brevet som brukes ved foreslå vedtak aksjonspunktet", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response mellomlagringAvOverstyring(@TilpassetAbacAttributt(supplierClass = MellomlagringHtmlSupplier.class) @Valid @NotNull MellomlagreHtmlDto mellomlagring) {
        var behandling = behandlingRepository.hentBehandling(mellomlagring.behandlingUuid());
        if (mellomlagring.redigertInnhold() == null) {
            dokumentBehandlingTjeneste.fjernOverstyringAvBrev(behandling);
        } else {
            dokumentBehandlingTjeneste.lagreOverstyrtBrev(behandling, mellomlagring.redigertInnhold().verdi());
        }
        return Response.ok().build();
    }

    public record MellomlagreHtmlDto(@Valid @NotNull UUID behandlingUuid, @Valid FritekstDto redigertInnhold) {
    }

    @POST
    @Path(BREV_VIS_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer en pdf som er en forhåndsvisning av brevet", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response forhåndsvisDokument(@Parameter(description = "Inneholder kode til brevmal og bestillingsdetaljer.") @TilpassetAbacAttributt(supplierClass = ForhåndsvisSupplier.class) @Valid ForhåndsvisDokumentDto forhåndsvisDto) { // NOSONAR
        var behandling = behandlingRepository.hentBehandling(forhåndsvisDto.behandlingUuid());
        var bestilling = DokumentForhandsvisning.builder()
            .medBehandlingUuid(forhåndsvisDto.behandlingUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(forhåndsvisDto.dokumentMal())
            .medRevurderingÅrsak(forhåndsvisDto.arsakskode())
            .medFritekst(forhåndsvisDto.fritekst() != null ? forhåndsvisDto.fritekst().verdi() : null)
            .medTittel(forhåndsvisDto.tittel())
            .medDokumentType(utledDokumentType(forhåndsvisDto.automatiskVedtaksbrev()))
            .build();

        if (DokumentMalType.ETTERLYS_INNTEKTSMELDING.equals(forhåndsvisDto.dokumentMal())) {
            validerFinnesManglendeInntektsmelding(behandling);
        }

        var dokument = dokumentForhåndsvisningTjeneste.forhåndsvisDokument(behandling.getId(), bestilling);
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
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = false) // Bruk create siden behandling ofte er avsluttet når kallet kommer
    public void kvitteringV3(@TilpassetAbacAttributt(supplierClass = DokumentKvitteringDataSupplier.class) @Valid DokumentKvitteringDto kvitto) {
        dokumentBehandlingTjeneste.kvitterSendtBrev(
            new DokumentKvittering(kvitto.behandlingUuid(), kvitto.dokumentbestillingUuid(), kvitto.journalpostId(), kvitto.dokumentId()));
    }

    @GET
    @Path(VARSEL_REVURDERING_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
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

    public static class MellomlagringHtmlSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (MellomlagreHtmlDto) obj;
            return AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid);
        }
    }
}
