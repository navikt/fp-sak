package no.nav.foreldrepenger.web.app.tjenester.dokument;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.DokumentRespons;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentIdDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.JournalpostIdDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.MottattDokumentDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(DokumentRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class DokumentRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentRestTjeneste.class);

    static final String BASE_PATH = "/dokument";
    private static final String MOTTATT_DOKUMENTER_PART_PATH = "/hent-mottattdokumentliste";
    public static final String MOTTATT_DOKUMENTER_PATH = BASE_PATH + MOTTATT_DOKUMENTER_PART_PATH;
    private static final String DOKUMENTER_PART_PATH = "/hent-dokumentliste";
    public static final String DOKUMENTER_PATH = BASE_PATH + DOKUMENTER_PART_PATH;
    private static final String DOKUMENT_PART_PATH = "/hent-dokument";

    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private VirksomhetTjeneste virksomhetTjeneste;
    private BehandlingRepository behandlingRepository;

    public DokumentRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public DokumentRestTjeneste(DokumentArkivTjeneste dokumentArkivTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste,
            FagsakRepository fagsakRepository,
            MottatteDokumentRepository mottatteDokumentRepository,
            VirksomhetTjeneste virksomhetTjeneste,
            BehandlingRepository behandlingRepository) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.behandlingRepository = behandlingRepository;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(MOTTATT_DOKUMENTER_PART_PATH)
    @Operation(description = "Henter listen av mottatte dokumenter knyttet til en fagsak", tags = "dokument")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Collection<MottattDokumentDto> hentAlleMottatteDokumenterForBehandling(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId()).stream()
            .map(MottattDokumentDto::new)
            .toList();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(DOKUMENTER_PART_PATH)
    @Operation(description = "Henter dokumentlisten knyttet til en sak", summary = "Oversikt over alle pdf dokumenter fra dokumentarkiv registrert for saksnummer.", tags = "dokument")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Collection<DokumentDto> hentAlleDokumenterForSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        var fagsakId = fagsak.map(Fagsak::getId).orElse(null);
        if (fagsakId == null) {
            return new ArrayList<>();
        }

        var åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream().map(Behandling::getId)
                .collect(Collectors.toSet());

        var inntektsMeldinger = inntektsmeldingTjeneste
                .hentAlleInntektsmeldingerForAngitteBehandlinger(åpneBehandlinger).stream()
                .collect(Collectors.groupingBy(Inntektsmelding::getJournalpostId));
        // Burde brukt map på dokumentid, men den lagres ikke i praksis.
        var mottatteIMDokument = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId)
                .stream()
                .filter(mdok -> DokumentTypeId.INNTEKTSMELDING.getKode().equals(mdok.getDokumentType().getKode()))
                .collect(Collectors.groupingBy(MottattDokument::getJournalpostId));

        var journalPostList = dokumentArkivTjeneste.hentAlleDokumenterCached(saksnummer);
        List<DokumentDto> dokumentResultat = new ArrayList<>();
        journalPostList.forEach(
                arkivJournalPost -> dokumentResultat.addAll(mapFraArkivJournalPost(arkivJournalPost, mottatteIMDokument, inntektsMeldinger)));
        dokumentResultat.sort(Comparator.comparing(DokumentDto::getTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));

        return dokumentResultat;
    }

    @GET
    @Path(DOKUMENT_PART_PATH)
    @Operation(description = "Søk etter dokument på JOARK-identifikatorene journalpostId og dokumentId", summary = "Retunerer dokument som er tilknyttet saksnummer, journalpostId og dokumentId (bare pdf)", tags = "dokument")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentDokument(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid SaksnummerDto saksnummer,
            @TilpassetAbacAttributt(supplierClass = JournalIdAbacSupplier.class) @NotNull @QueryParam("journalpostId") @Parameter(description = "Unik identifikator av journalposten (forsendelsenivå)") @Valid JournalpostIdDto journalpostId,
            @TilpassetAbacAttributt(supplierClass = DokumentIdAbacSupplier.class) @NotNull @QueryParam("dokumentId") @Parameter(description = "Unik identifikator av DokumentInfo/Dokumentbeskrivelse (dokumentnivå)") @Valid DokumentIdDto dokumentId) {
        try {
            return tilRespons(dokumentArkivTjeneste.hentDokument(new JournalpostId(journalpostId.getJournalpostId()), dokumentId.getDokumentId()));
        } catch (TekniskException e) {
            var feilmelding = String.format("Dokument ikke funnet for journalpostId= %s dokumentId= %s",
                journalpostId.getJournalpostId(), dokumentId.getDokumentId());
            LOG.warn(feilmelding, e);
            return notFound(feilmelding);
        }
    }

    static Response tilRespons(DokumentRespons dokumentRespons) {
        var responseBuilder = Response.ok(new ByteArrayInputStream(dokumentRespons.innhold()));
        responseBuilder.type(dokumentRespons.contentType());
        responseBuilder.header("Content-Disposition", dokumentRespons.contentDisp());
        return responseBuilder.build();
    }

    public static class DokumentIdAbacSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

    public static class JournalIdAbacSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (JournalpostIdDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.JOURNALPOST_ID, req.getJournalpostId());
        }
    }

    private Response notFound(String feilmelding) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new FeilDto(FeilType.TOMT_RESULTAT_FEIL, feilmelding))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    private List<DokumentDto> mapFraArkivJournalPost(ArkivJournalPost arkivJournalPost, Map<JournalpostId, List<MottattDokument>> mottatteIMDokument,
            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        List<DokumentDto> dokumentForJP = new ArrayList<>();
        if (arkivJournalPost.getHovedDokument() != null) {
            dokumentForJP.add(mapFraArkivDokument(arkivJournalPost, arkivJournalPost.getHovedDokument(), mottatteIMDokument, inntektsMeldinger));
        }
        if (arkivJournalPost.getAndreDokument() != null) {
            arkivJournalPost.getAndreDokument().forEach(dok -> dokumentForJP.add(mapFraArkivDokument(arkivJournalPost, dok, mottatteIMDokument, inntektsMeldinger)));
        }
        return dokumentForJP;
    }

    private DokumentDto mapFraArkivDokument(ArkivJournalPost arkivJournalPost, ArkivDokument arkivDokument,
            Map<JournalpostId, List<MottattDokument>> mottatteIMDokument,
            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        var dto = new DokumentDto(arkivJournalPost, arkivDokument);
        if (DokumentTypeId.INNTEKTSMELDING.equals(arkivDokument.getDokumentType())
                && mottatteIMDokument.containsKey(arkivJournalPost.getJournalpostId())) {
            var behandlinger = mottatteIMDokument.get(dto.getJournalpostId()).stream()
                    .filter(imdok -> inntektsMeldinger.containsKey(dto.getJournalpostId()))
                    .map(MottattDokument::getBehandlingId)
                    .toList();
            dto.setBehandlinger(behandlinger);
            var behandlingUuidList = mottatteIMDokument.get(dto.getJournalpostId()).stream()
                .filter(imdok -> inntektsMeldinger.containsKey(dto.getJournalpostId()))
                .map(MottattDokument::getBehandlingId)
                .filter(Objects::nonNull)
                .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId).getUuid())
                .toList();
            dto.setBehandlingUuidList(behandlingUuidList);

            var navn = inntektsMeldinger.getOrDefault(dto.getJournalpostId(), Collections.emptyList())
                    .stream()
                    .map((Inntektsmelding inn) -> {
                        var t = inn.getArbeidsgiver();
                        if (t.getErVirksomhet()) {
                            return virksomhetTjeneste.finnOrganisasjon(t.getOrgnr())
                                    .orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + t.getOrgnr()))
                                    .getNavn();
                        }
                        return "Privatperson";
                    })// TODO slå opp navnet på privatpersonen?
                    .findFirst();
            navn.ifPresent(dto::setGjelderFor);
            var referanse = inntektsMeldinger.getOrDefault(dto.getJournalpostId(), Collections.emptyList()).stream()
                .map(Inntektsmelding::getArbeidsgiver)
                .map(Arbeidsgiver::getIdentifikator)
                .findFirst();
            referanse.ifPresent(dto::setArbeidsgiverReferanse);
        }
        return dto;
    }
}
