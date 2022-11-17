package no.nav.foreldrepenger.web.app.tjenester.gosys;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.Behandlingstema;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.FinnSakListeRequest;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.FinnSakListeResponse;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.Sak;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.Saksstatus;
import no.nav.foreldrepenger.web.app.tjenester.gosys.opprettSak.OpprettSakRequest;
import no.nav.foreldrepenger.web.app.tjenester.gosys.opprettSak.OpprettSakResponse;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Erstattning for WS for å finne relevante fagsaker i VL.
 * WS dokumentasjon finnes her https://confluence.adeo.no/pages/viewpage.action?pageId=220528950"
 */
@Path("/sak")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class GosysRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(GosysRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private OpprettSakOrchestrator opprettSakOrchestrator;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    public GosysRestTjeneste() {
        // CDI
    }

    @Inject
    public GosysRestTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             OpprettSakOrchestrator opprettSakOrchestrator,
                             DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.opprettSakOrchestrator = opprettSakOrchestrator;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }
    @POST
    @Path("/opprettSak/v1")
    @Operation(description = "For å opprette en ny fagsak i FPSAK.", tags = "gosys", responses = {
        @ApiResponse(responseCode = "200", description = "Sak opprettet", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = OpprettSakResponse.class))),
        @ApiResponse(responseCode = "400", description = "Feil i request", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeilDto.class))),
        @ApiResponse(responseCode = "403", description = "Mangler tilgang", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeilDto.class))),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public OpprettSakResponse opprettSak(
        @Parameter(description = "Trenger journalpostId, behandlingstema og aktørId til brukeren for å kunne opprette en ny sak i FPSAK.")
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = OpprettSakAbacDataSupplier.class) OpprettSakRequest opprettSakRequest) {

        if (opprettSakRequest.aktørId() == null) {
            throw new TekniskException("FP-34235", lagUgyldigInputMelding("AktørId", null));
        }
        var aktørId = new AktørId(opprettSakRequest.aktørId());

        JournalpostId journalpostId = new JournalpostId(opprettSakRequest.journalpostId());
        validerJournalpost(journalpostId, hentBehandlingstema(opprettSakRequest.behandlingsTema()), aktørId);
        var saksnummer = opprettSakOrchestrator.opprettSak(journalpostId, hentBehandlingstema(opprettSakRequest.behandlingsTema()), aktørId);

        return lagOpprettSakResponse(saksnummer);
    }

    private OpprettSakResponse lagOpprettSakResponse(Saksnummer saksnummer) {
        return new OpprettSakResponse(saksnummer.getVerdi());
    }

    private BehandlingTema hentBehandlingstema(String behandlingstemaOffisiellKode) {
        var behandlingTema = BehandlingTema.finnForKodeverkEiersKode(behandlingstemaOffisiellKode);
        if (BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            var feilMelding = lagUgyldigInputMelding("Behandlingstema", null);
            throw new TekniskException("FP-34235", feilMelding);
        }
        if (FagsakYtelseType.UDEFINERT.equals(behandlingTema.getFagsakYtelseType())) {
            var feilMelding = lagUgyldigInputMelding("Behandlingstema", behandlingstemaOffisiellKode);
            throw new TekniskException("FP-34235", feilMelding);
        }
        return behandlingTema;
    }

    private void validerJournalpost(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) {

        var journalpost = dokumentArkivTjeneste.hentJournalpostForSak(journalpostId);
        var hoveddokument = journalpost.map(ArkivJournalPost::getHovedDokument);
        var journalpostYtelseType = FagsakYtelseType.UDEFINERT;
        if (hoveddokument.map(ArkivDokument::getDokumentType).filter(DokumentTypeId::erSøknadType).isPresent()) {
            journalpostYtelseType = getFagsakYtelseType(behandlingTema, hoveddokument);
            if (journalpostYtelseType == null) {
                return;
            }
        } else if (hoveddokument.map(ArkivDokument::getDokumentType).filter(DokumentTypeId.INNTEKTSMELDING::equals).isPresent()) {
            var original = dokumentArkivTjeneste.hentStrukturertDokument(journalpostId, hoveddokument.map(ArkivDokument::getDokumentId).orElseThrow()).toLowerCase();
            if (original.contains("ytelse>foreldrepenger<")) {
                if (opprettSakOrchestrator.harAktivSak(aktørId, behandlingTema)) {
                    var feilMelding = lagUgyldigInputMelding("Journalpost", "Inntektsmelding når det finnes åpen Foreldrepengesak");
                    throw new TekniskException("FP-34235", feilMelding);
                }
                journalpostYtelseType = FagsakYtelseType.FORELDREPENGER;
            } else if (original.contains("ytelse>svangerskapspenger<")) {
                journalpostYtelseType = FagsakYtelseType.SVANGERSKAPSPENGER;
            }
        }
        LOG.info("FPSAK vurdering ytelsedok {} vs ytelseoppgitt {}", journalpostYtelseType, behandlingTema.getFagsakYtelseType());
        if (!behandlingTema.getFagsakYtelseType().equals(journalpostYtelseType)) {
            throw new FunksjonellException("FP-785356", "Dokument og valgt ytelsetype i uoverenstemmelse",
                "Velg ytelsetype som samstemmer med dokument");
        }
        if (FagsakYtelseType.UDEFINERT.equals(journalpostYtelseType)) {
            throw new FunksjonellException("FP-785354", "Kan ikke opprette sak basert på oppgitt dokument",
                "Journalføre dokument på annen sak");
        }
    }

    private static FagsakYtelseType getFagsakYtelseType(BehandlingTema behandlingTema, Optional<ArkivDokument> hoveddokument) {
        FagsakYtelseType journalpostYtelseType;
        var hovedtype = hoveddokument.map(ArkivDokument::getDokumentType).orElseThrow();
        journalpostYtelseType = switch (hovedtype) {
            case SØKNAD_ENGANGSSTØNAD_ADOPSJON -> FagsakYtelseType.ENGANGSTØNAD;
            case SØKNAD_ENGANGSSTØNAD_FØDSEL -> FagsakYtelseType.ENGANGSTØNAD;
            case SØKNAD_FORELDREPENGER_ADOPSJON -> FagsakYtelseType.FORELDREPENGER;
            case SØKNAD_FORELDREPENGER_FØDSEL -> FagsakYtelseType.FORELDREPENGER;
            case SØKNAD_SVANGERSKAPSPENGER ->  FagsakYtelseType.SVANGERSKAPSPENGER;
            default -> FagsakYtelseType.UDEFINERT;
        };
        if (behandlingTema.getFagsakYtelseType().equals(journalpostYtelseType))
            return null;
        return journalpostYtelseType;
    }

    private String lagUgyldigInputMelding(String feltnavn, String verdi) {
        return String.format("Ugyldig input: %s med verdi: %s er ugyldig input.", feltnavn, verdi);
    }

    @POST
    @Path("/finnSak/v1")
    @Operation(description = "For å finne relevante fagsaker i FPSAK.", tags = "gosys")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.SAKLISTE)
    public FinnSakListeResponse finnSakListe(
        @Parameter(description = "AktørId til personen som det skal finnes saker for i FPSAK.")
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = FinnSakAbacDataSupplier.class) FinnSakListeRequest request) {

        var fagsaker = fagsakRepository.hentForBruker(new AktørId(request.aktørId()));
        return lagFinnSakResponse(fagsaker);
    }

    FinnSakListeResponse lagFinnSakResponse(List<Fagsak> fagsaker) {
        var saksliste = new ArrayList<Sak>();
        for (var fagsak : fagsaker) {
            saksliste.add(lagEksternRepresentasjon(fagsak));
        }
        return new FinnSakListeResponse(saksliste);
    }

    private Sak lagEksternRepresentasjon(Fagsak fagsak) {
        return new Sak(fagsak.getSaksnummer().getVerdi(), fagsak.getOpprettetTidspunkt(), lagEksternRepresentasjon(fagsak.getStatus()),
            fagsak.getEndretTidspunkt(), lagEksternRepresentasjonBehandlingstema(fagsak));
    }

    private Behandlingstema lagEksternRepresentasjonBehandlingstema(Fagsak fagsak) {
        var behandlingTemaKodeliste = getBehandlingTema(fagsak);

        return new Behandlingstema(behandlingTemaKodeliste.getNavn(), behandlingTemaKodeliste.getOffisiellKode(), "", null);
    }

    private static Saksstatus lagEksternRepresentasjon(FagsakStatus status) {
        return new Saksstatus(status.getNavn(), status.getKode(), "", null);
    }

    private BehandlingTema getBehandlingTema(Fagsak fagsak) {
        if (!erStøttetYtelseType(fagsak.getYtelseType())) {
            throw new TekniskException("FP-861850", "Ikke-støttet ytelsestype: " + fagsak.getYtelseType());
        }

        var behandlingTema = getBehandlingsTemaForFagsak(fagsak);
        if (BehandlingTema.gjelderEngangsstønad(behandlingTema) || BehandlingTema.gjelderForeldrepenger(behandlingTema)
            || BehandlingTema.gjelderSvangerskapspenger(behandlingTema)) {
            return behandlingTema;
        }
        // det er riktig å rapportere på årsakstype, selv om koden over bruker
        // BehandlingTema
        throw new TekniskException("FP-132949", "Ikke-støttet årsakstype: " + behandlingTema);
    }

    private BehandlingTema getBehandlingsTemaForFagsak(Fagsak s) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(s.getId());
        if (behandling.isEmpty()) {
            return BehandlingTema.fraFagsak(s, null);
        }

        var sisteBehandling = behandling.get();
        final var grunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(sisteBehandling.getId());
        return BehandlingTema.fraFagsak(s, grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
    }

    private static boolean erStøttetYtelseType(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType) || FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)
            || FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType);
    }

    public static class FinnSakAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (FinnSakListeRequest) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, req.aktørId());
        }
    }

    public static class OpprettSakAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (OpprettSakRequest) obj;
            var dataAttributter = AbacDataAttributter.opprett();
            if (req.aktørId() != null) {
                dataAttributter = dataAttributter.leggTil(AppAbacAttributtType.AKTØR_ID, req.aktørId());
            }
            return dataAttributter;
        }
    }

}
