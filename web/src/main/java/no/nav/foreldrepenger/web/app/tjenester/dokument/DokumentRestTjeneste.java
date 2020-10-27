package no.nav.foreldrepenger.web.app.tjenester.dokument;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
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
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentIdDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.JournalpostIdDto;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.MottattDokumentDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(DokumentRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class DokumentRestTjeneste {

    static final String BASE_PATH = "/dokument";
    private static final String MOTTATT_DOKUMENTER_PART_PATH = "/hent-mottattdokumentliste";
    public static final String MOTTATT_DOKUMENTER_PATH = BASE_PATH + MOTTATT_DOKUMENTER_PART_PATH;
    private static final String DOKUMENTER_PART_PATH = "/hent-dokumentliste";
    public static final String DOKUMENTER_PATH = BASE_PATH + DOKUMENTER_PART_PATH;
    private static final String DOKUMENT_PART_PATH = "/hent-dokument";
    public static final String DOKUMENT_PATH = BASE_PATH + DOKUMENTER_PART_PATH; // NOSONAR TFP-2234

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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path(MOTTATT_DOKUMENTER_PART_PATH)
    @Operation(description = "Henter listen av mottatte dokumenter knyttet til en fagsak", tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public Collection<MottattDokumentDto> hentAlleMottatteDokumenterForBehandling(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId()).stream()
            .map(MottattDokumentDto::new)
            .collect(Collectors.toList());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(MOTTATT_DOKUMENTER_PART_PATH)
    @Operation(description = "Henter listen av mottatte dokumenter knyttet til en fagsak", tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Collection<MottattDokumentDto> hentAlleMottatteDokumenterForBehandling(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentAlleMottatteDokumenterForBehandling(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path(DOKUMENTER_PART_PATH)
    @Operation(description = "Henter dokumentlisten knyttet til en sak", summary = ("Oversikt over alle pdf dokumenter fra dokumentarkiv registrert for saksnummer."), tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Collection<DokumentDto> hentAlleDokumenterForSak(
            @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid SaksnummerDto saksnummerDto) {
        try {
            Saksnummer saksnummer = new Saksnummer(saksnummerDto.getVerdi());
            final Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
            final Long fagsakId = fagsak.map(Fagsak::getId).orElse(null);
            if (fagsakId == null) {
                return new ArrayList<>();
            }

            Set<Long> åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream().map(Behandling::getId)
                    .collect(Collectors.toSet());

            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger = inntektsmeldingTjeneste
                    .hentAlleInntektsmeldingerForAngitteBehandlinger(åpneBehandlinger).stream()
                    .collect(Collectors.groupingBy(Inntektsmelding::getJournalpostId));
            // Burde brukt map på dokumentid, men den lagres ikke i praksis.
            Map<JournalpostId, List<MottattDokument>> mottatteIMDokument = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId)
                    .stream()
                    .filter(mdok -> DokumentTypeId.INNTEKTSMELDING.getKode().equals(mdok.getDokumentType().getKode()))
                    .collect(Collectors.groupingBy(MottattDokument::getJournalpostId));

            List<ArkivJournalPost> journalPostList = dokumentArkivTjeneste.hentAlleDokumenterForVisning(saksnummer);
            List<DokumentDto> dokumentResultat = new ArrayList<>();
            journalPostList.forEach(
                    arkivJournalPost -> dokumentResultat.addAll(mapFraArkivJournalPost(arkivJournalPost, mottatteIMDokument, inntektsMeldinger)));
            dokumentResultat.sort(Comparator.comparing(DokumentDto::getTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));

            return dokumentResultat;
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentJournalpostListeTjeneste(e).toException();
        }
    }

    @GET
    @Path(DOKUMENT_PART_PATH)
    @Operation(description = "Søk etter dokument på JOARK-identifikatorene journalpostId og dokumentId", summary = ("Retunerer dokument som er tilknyttet saksnummer, journalpostId og dokumentId."), tags = "dokument")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response hentDokument(
            @SuppressWarnings("unused") @NotNull @QueryParam("saksnummer") @Parameter(description = "Saksnummer") @Valid SaksnummerDto saksnummer,
            @NotNull @QueryParam("journalpostId") @Parameter(description = "Unik identifikator av journalposten (forsendelsenivå)") @Valid JournalpostIdDto journalpostId,
            @NotNull @QueryParam("dokumentId") @Parameter(description = "Unik identifikator av DokumentInfo/Dokumentbeskrivelse (dokumentnivå)") @Valid DokumentIdDto dokumentId) {
        try {
            ResponseBuilder responseBuilder = Response.ok(
                    new ByteArrayInputStream(
                            dokumentArkivTjeneste.hentDokument(new JournalpostId(journalpostId.getJournalpostId()), dokumentId.getDokumentId())));
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=dokument.pdf");
            return responseBuilder.build();
        } catch (TekniskException e) {
            throw DokumentRestTjenesteFeil.FACTORY.dokumentIkkeFunnet(journalpostId.getJournalpostId(), dokumentId.getDokumentId(), e).toException();
        } catch (ManglerTilgangException e) {
            throw DokumentRestTjenesteFeil.FACTORY.applikasjonHarIkkeTilgangTilHentDokumentTjeneste(e).toException();
        }
    }

    private List<DokumentDto> mapFraArkivJournalPost(ArkivJournalPost arkivJournalPost, Map<JournalpostId, List<MottattDokument>> mottatteIMDokument,
            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        List<DokumentDto> dokumentForJP = new ArrayList<>();
        if (arkivJournalPost.getHovedDokument() != null) {
            dokumentForJP.add(mapFraArkivDokument(arkivJournalPost, arkivJournalPost.getHovedDokument(), mottatteIMDokument, inntektsMeldinger));
        }
        if (arkivJournalPost.getAndreDokument() != null) {
            arkivJournalPost.getAndreDokument().forEach(dok -> {
                dokumentForJP.add(mapFraArkivDokument(arkivJournalPost, dok, mottatteIMDokument, inntektsMeldinger));
            });
        }
        return dokumentForJP;
    }

    private DokumentDto mapFraArkivDokument(ArkivJournalPost arkivJournalPost, ArkivDokument arkivDokument,
            Map<JournalpostId, List<MottattDokument>> mottatteIMDokument,
            Map<JournalpostId, List<Inntektsmelding>> inntektsMeldinger) {
        DokumentDto dto = new DokumentDto(arkivJournalPost, arkivDokument);
        if (DokumentTypeId.INNTEKTSMELDING.equals(arkivDokument.getDokumentType())
                && mottatteIMDokument.containsKey(arkivJournalPost.getJournalpostId())) {
            List<Long> behandlinger = mottatteIMDokument.get(dto.getJournalpostId()).stream()
                    .filter(imdok -> inntektsMeldinger.containsKey(dto.getJournalpostId()))
                    .map(MottattDokument::getBehandlingId)
                    .collect(Collectors.toList());
            dto.setBehandlinger(behandlinger);

            Optional<String> navn = inntektsMeldinger.getOrDefault(dto.getJournalpostId(), Collections.emptyList())
                    .stream()
                    .map((Inntektsmelding inn) -> {
                        var t = inn.getArbeidsgiver();
                        if (t.getErVirksomhet()) {
                            return virksomhetTjeneste.finnOrganisasjon(t.getOrgnr())
                                    .orElseThrow(() -> new IllegalArgumentException("Kunne ikke hente virksomhet for orgNummer: " + t.getOrgnr()))
                                    .getNavn();
                        } else {
                            return "Privatperson";
                        }
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
