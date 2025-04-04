package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.BehandlendeFagsystemDto;
import no.nav.foreldrepenger.kontrakter.fordel.FagsakInfomasjonDto;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostIdDto;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostKnyttningDto;
import no.nav.foreldrepenger.kontrakter.fordel.JournalpostMottakDto;
import no.nav.foreldrepenger.kontrakter.fordel.OpprettSakDto;
import no.nav.foreldrepenger.kontrakter.fordel.OpprettSakV2Dto;
import no.nav.foreldrepenger.kontrakter.fordel.SakInfoV2Dto;
import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;
import no.nav.foreldrepenger.kontrakter.fordel.VurderFagsystemDto;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Mottar dokumenter fra f.eks. FPFORDEL og håndterer dispatch internt for
 * saksbehandlingsløsningen.
 */
@Path("/fordel")
@ApplicationScoped
@Transactional
public class FordelRestTjeneste {
    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private OpprettSakTjeneste opprettSakTjeneste;
    private VurderFagsystemFellesTjeneste vurderFagsystemTjeneste;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private SakInfoDtoTjeneste sakInfoDtoTjeneste;

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(SaksbehandlingDokumentmottakTjeneste dokumentmottakTjeneste,
                              FagsakTjeneste fagsakTjeneste,
                              OpprettSakTjeneste opprettSakTjeneste,
                              BehandlingRepositoryProvider repositoryProvider,
                              VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste,
                              SakInfoDtoTjeneste sakInfoDtoTjeneste) {
        this.dokumentmottakTjeneste = dokumentmottakTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vurderFagsystemTjeneste = vurderFagsystemFellesTjeneste;
        this.sakInfoDtoTjeneste = sakInfoDtoTjeneste;
    }

    @POST
    @Path("/vurderFagsystem")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Informasjon om en fagsak", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public BehandlendeFagsystemDto vurderFagsystem(@Parameter(description = "Krever behandlingstemaOffisiellKode", required = true) @Valid AbacVurderFagsystemDto vurderFagsystemDto) {
        ensureCallId();
        var vurderFagsystem = map(vurderFagsystemDto);
        var behandlendeFagsystem = vurderFagsystemTjeneste.vurderFagsystem(vurderFagsystem);
        return map(behandlendeFagsystem);
    }

    @POST
    @Path("/klageinstans")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Informasjon om en fagsak klageinstansrelatert", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public BehandlendeFagsystemDto vurderForKlageinstans(@Parameter(description = "Krever behandlingstemaOffisiellKode", required = true) @Valid AbacVurderFagsystemDto vurderFagsystemDto) {
        ensureCallId();
        var vurderFagsystem = map(vurderFagsystemDto);
        var behandlendeFagsystem = vurderFagsystemTjeneste.vurderFagsystemForKlageinstans(vurderFagsystem);
        return map(behandlendeFagsystem);
    }

    @POST
    @Path("/fagsak/informasjon")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Informasjon om en fagsak", summary = "Varsel om en ny journalpost som skal behandles i systemet.", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public FagsakInfomasjonDto fagsak(@Parameter(description = "Saksnummeret det skal hentes saksinformasjon om") @Valid AbacSaksnummerDto saksnummerDto) {
        ensureCallId();
        var optFagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(saksnummerDto.getSaksnummer()), false);
        if (optFagsak.isEmpty() || optFagsak.get().erStengt()) {
            return null;
        }
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(optFagsak.get().getId());
        FamilieHendelseEntitet familieHendelse = null;
        if (behandling.isPresent()) {
            familieHendelse = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.get().getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .orElse(null);
        }
        var behandlingTemaFraKodeverksRepo = BehandlingTema.fraFagsak(optFagsak.get(), familieHendelse);
        var behandlingstemaOffisiellKode = behandlingTemaFraKodeverksRepo.getOffisiellKode();
        var aktørId = optFagsak.get().getAktørId();
        return new FagsakInfomasjonDto(aktørId.getId(), behandlingstemaOffisiellKode);
    }

    /**
     * @deprecated skal fjernes - bruk /fagsak/opprett/v2. Må fikses i fpfordel så fjernes her.
     */
    @Deprecated(forRemoval = true)
    @POST
    @Path("/fagsak/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Ny journalpost skal behandles.", summary = "Varsel om en ny journalpost som skal behandles i systemet.", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public SaksnummerDto opprettSak(@Parameter(description = "Oppretter fagsak") @Valid AbacOpprettSakDto opprettSakDto) {
        ensureCallId();
        var journalpostId = opprettSakDto.getJournalpostId();
        var behandlingTema = BehandlingTema.finnForKodeverkEiersKode(opprettSakDto.getBehandlingstemaOffisiellKode());

        var aktørId = new AktørId(opprettSakDto.getAktørId());

        var ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);

        var s = opprettSakTjeneste.opprettSak(ytelseType, aktørId, journalpostId.map(JournalpostId::new).orElse(null));
        return new SaksnummerDto(s.getVerdi());
    }

    @POST
    @Path("/fagsak/opprett/v2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Ny journalpost skal behandles.", summary = "Varsel om en ny journalpost som skal behandles i systemet.", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public SaksnummerDto opprettSakv2(@TilpassetAbacAttributt(supplierClass = AktørIdDataSupplier.class) @Parameter(description = "Oppretter fagsak") @Valid OpprettSakV2Dto opprettSakDto) {
        ensureCallId();
        var journalpostId = Optional.ofNullable(opprettSakDto.journalpostId());
        var ytelseType = mapYtelseType(opprettSakDto.ytelseType());

        var aktørId = new AktørId(opprettSakDto.aktørId());

        var saksnummer = opprettSakTjeneste.opprettSak(ytelseType, aktørId, journalpostId.map(JournalpostId::new).orElse(null));
        return new SaksnummerDto(saksnummer.getVerdi());
    }

    private FagsakYtelseType mapYtelseType(YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case FORELDREPENGER -> FagsakYtelseType.FORELDREPENGER;
            case ENGANGSTØNAD -> FagsakYtelseType.ENGANGSTØNAD;
            case SVANGERSKAPSPENGER -> FagsakYtelseType.SVANGERSKAPSPENGER;
        };
    }

    public static class AktørIdDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, ((OpprettSakV2Dto) obj).aktørId());
        }
    }

    @POST
    @Path("/fagsak/knyttJournalpost")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Knytt journalpost til fagsak.", summary = "Før en journalpost journalføres på en fagsak skal fagsaken oppdateres med journalposten.", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response knyttSakOgJournalpost(@Parameter(description = "Saksnummer og JournalpostId som skal knyttes sammen") @Valid AbacJournalpostKnyttningDto journalpostKnytningDto) {
        ensureCallId();
        var saksnummer = new Saksnummer(journalpostKnytningDto.getSaksnummer());
        var fagsak = opprettSakTjeneste.finnSak(saksnummer);

        if (fagsak.isPresent()) {
            opprettSakTjeneste.knyttSakOgJournalpost(saksnummer, new JournalpostId(journalpostKnytningDto.getJournalpostId()));
        } else {
            throw new TekniskException("FP-840572", "Finner ikke fagsak med angitt saksnummer " + saksnummer);
        }
        return Response.ok().build();
    }

    @POST
    @Path("/journalpost")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Ny journalpost skal behandles.", summary = "Varsel om en ny journalpost som skal behandles i systemet.", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response mottaJournalpost(@Parameter(description = "Krever saksnummer, journalpostId og behandlingstemaOffisiellKode") @Valid AbacJournalpostMottakDto mottattJournalpost) {
        var dokumentTypeId = mottattJournalpost.getDokumentTypeIdOffisiellKode()
            .map(DokumentTypeId::finnForKodeverkEiersKode)
            .orElse(DokumentTypeId.UDEFINERT);
        if (DokumentTypeId.TILBAKEKREVING_UTTALSELSE.equals(dokumentTypeId) || DokumentTypeId.TILBAKEBETALING_UTTALSELSE.equals(dokumentTypeId)) {
            return Response.ok().build();
        }
        ensureCallId();
        var saksnummer = new Saksnummer(mottattJournalpost.getSaksnummer());
        var dokument = mapTilMottattDokument(mottattJournalpost, dokumentTypeId, saksnummer);
        dokumentmottakTjeneste.dokumentAnkommet(dokument, null, saksnummer);
        return Response.ok().build();
    }

    @POST
    @Path("/finnFagsaker/v2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Finn alle saker for en bruker.", summary = "Finn alle saker for en bruker", tags = "fordel", responses = {@ApiResponse(responseCode = "200", description = "Liste av alle brukers saker, ellers tom liste", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SakInfoV2Dto.class))))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public List<SakInfoV2Dto> finnAlleSakerForBrukerV2(@TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) @Parameter(description = "AktørId") @Valid AktørIdDto bruker) {
        ensureCallId();
        if (!AktørId.erGyldigAktørId(bruker.aktørId())) {
            throw new IllegalArgumentException("Oppgitt aktørId er ikke en gyldig ident.");
        }
        return fagsakTjeneste.finnFagsakerForAktør(new AktørId(bruker.aktørId())).stream()
            .map(fagsak -> sakInfoDtoTjeneste.mapSakInfoV2Dto(fagsak)).toList();
    }

    @POST
    @Path("/sakInntektsmelding")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sjekker om det finnes en sak som potensielt kan knyttes til inntektsmelding med gitt startdato", tags = "fordel")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response sjekkSakForInntektsmelding(@TilpassetAbacAttributt(supplierClass = SakInntektsmeldingDtoAbacDataSupplier.class) @Parameter(description = "AktørId") @Valid SakInntektsmeldingDto sakInntektsmeldingDto) {
        ensureCallId();
        if (!AktørId.erGyldigAktørId(sakInntektsmeldingDto.bruker().aktørId())) {
            throw new IllegalArgumentException("Oppgitt aktørId er ikke en gyldig ident.");
        }
        var søkersFagsaker = fagsakTjeneste.finnFagsakerForAktør(new AktørId(sakInntektsmeldingDto.bruker().aktørId()));
        var ytelseDetSjekkesMot = sakInntektsmeldingDto.ytelse().equals(SakInntektsmeldingDto.YtelseType.FORELDREPENGER) ? FagsakYtelseType.FORELDREPENGER : FagsakYtelseType.SVANGERSKAPSPENGER;
        var finnesSakPåSøkerForYtelse = søkersFagsaker.stream()
            .filter(fag -> !fag.getStatus().equals(FagsakStatus.AVSLUTTET))
            .anyMatch(fag -> erÅpenForBehandling(fag.getId()) && fag.getYtelseType().equals(ytelseDetSjekkesMot));
        return Response.ok(new SakInntektsmeldingResponse(finnesSakPåSøkerForYtelse)).build();
    }

    private boolean erÅpenForBehandling(Long fagsakId) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        // I enkelte tilfeller skal vi ikke tillate innsending av inntektsmelding selv om det finnes sak, fordi saken reelt sett ikke er klar for behandling
        return behandling.map(b -> b.getAksjonspunkter().stream()
            .noneMatch(ap -> stårIÅpentAksjonspunkt(ap, Set.of(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD,
                AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, AksjonspunktDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER))))
            .orElse(false);
    }

    private boolean stårIÅpentAksjonspunkt(Aksjonspunkt ap, Set<AksjonspunktDefinisjon> definisjoner) {
        return ap.getStatus().equals(AksjonspunktStatus.OPPRETTET) && definisjoner.contains(ap.getAksjonspunktDefinisjon());
    }

    public record AktørIdDto(@NotNull @Digits(integer = 19, fraction = 0) String aktørId) {
        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + maskerAktørId() + ">";
        }

        private String maskerAktørId() {
            if (aktørId == null) {
                return "";
            }
            var length = aktørId.length();
            if (length <= 4) {
                return "*".repeat(length);
            }
            return "*".repeat(length - 4) + aktørId.substring(length - 4);
        }
    }

    public record SakInntektsmeldingResponse(boolean søkerHarSak){}

    public record SakInntektsmeldingDto(@NotNull @Valid AktørIdDto bruker, @NotNull @Valid YtelseType ytelse){
        protected enum YtelseType {
            FORELDREPENGER,
            SVANGERSKAPSPENGER
        }
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (AktørIdDto) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, req.aktørId());
        }
    }

    private void ensureCallId() {
        var callId = MDCOperations.getCallId();
        if (callId == null || callId.isBlank()) {
            MDCOperations.putCallId();
        }
    }

    private VurderFagsystem map(VurderFagsystemDto dto) {
        var v = new VurderFagsystem();
        dto.getJournalpostId().map(JournalpostId::new).ifPresent(v::setJournalpostId);
        v.setStrukturertSøknad(dto.isStrukturertSøknad());
        v.setAktørId(new AktørId(dto.getAktørId()));
        var behandlingTema = BehandlingTema.finnForKodeverkEiersKode(dto.getBehandlingstemaOffisiellKode());

        v.setBehandlingTema(behandlingTema);
        v.setAdopsjonsbarnFodselsdatoer(dto.getAdopsjonsBarnFodselsdatoer());

        dto.getBarnTermindato().ifPresent(v::setBarnTermindato);
        dto.getBarnFodselsdato().ifPresent(v::setBarnFodselsdato);
        dto.getOmsorgsovertakelsedato().ifPresent(v::setOmsorgsovertakelsedato);
        dto.getÅrsakInnsendingInntektsmelding().ifPresent(v::setÅrsakInnsendingInntektsmelding);
        dto.getVirksomhetsnummer().ifPresent(v::setVirksomhetsnummer);
        dto.getArbeidsgiverAktørId().map(AktørId::new).ifPresent(v::setArbeidsgiverAktørId);
        dto.getArbeidsforholdsid().ifPresent(v::setArbeidsforholdsid);
        dto.getForsendelseMottattTidspunkt().ifPresent(v::setForsendelseMottattTidspunkt);
        dto.getStartDatoForeldrepengerInntektsmelding().ifPresent(v::setStartDatoForeldrepengerInntektsmelding);

        dto.getSaksnummer().ifPresent(sn -> v.setSaksnummer(new Saksnummer(sn)));
        dto.getAnnenPart().map(AktørId::new).ifPresent(v::setAnnenPart);

        v.setDokumentTypeId(DokumentTypeId.UDEFINERT);
        v.setDokumentKategori(DokumentKategori.UDEFINERT);
        if (dto.getDokumentTypeIdOffisiellKode() != null) {
            v.setDokumentTypeId(DokumentTypeId.finnForKodeverkEiersKode(dto.getDokumentTypeIdOffisiellKode()));
        }
        if (dto.getDokumentKategoriOffisiellKode() != null) {
            v.setDokumentKategori(DokumentKategori.finnForKodeverkEiersKode(dto.getDokumentKategoriOffisiellKode()));
        }

        return v;
    }

    private BehandlendeFagsystemDto map(BehandlendeFagsystem behandlendeFagsystem) {
        BehandlendeFagsystemDto dto;
        var saksnummer = behandlendeFagsystem.getSaksnummer();
        dto = saksnummer.map(value -> new BehandlendeFagsystemDto(value.getVerdi())).orElseGet(BehandlendeFagsystemDto::new);
        if (Objects.requireNonNull(behandlendeFagsystem.behandlendeSystem()) == BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING) {
            dto.setBehandlesIVedtaksløsningen(true);
        } else if (behandlendeFagsystem.behandlendeSystem() == BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING) {
            dto.setManuellVurdering(true);
        }
        return dto;
    }

    private MottattDokument mapTilMottattDokument(AbacJournalpostMottakDto journalpostMottakDto, DokumentTypeId dokumentTypeId, Saksnummer saksnummer) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false)
            .orElseThrow(() -> new IllegalStateException("Finner ingen fagsak for saksnummer " + saksnummer));
        var dokumentKategori = utledDokumentKategori(journalpostMottakDto.getDokumentKategoriOffisiellKode(), dokumentTypeId);

        var builder = new MottattDokument.Builder().medJournalPostId(new JournalpostId(journalpostMottakDto.getJournalpostId()))
            .medDokumentType(dokumentTypeId)
            .medDokumentKategori(dokumentKategori)
            .medMottattDato(journalpostMottakDto.getForsendelseMottatt().orElse(LocalDate.now()))
            .medMottattTidspunkt(journalpostMottakDto.getForsendelseMottattTidspunkt()
                != null ? journalpostMottakDto.getForsendelseMottattTidspunkt() : LocalDateTime.now())
            .medElektroniskRegistrert(journalpostMottakDto.getPayloadXml().isPresent())
            .medFagsakId(fagsak.getId())
            .medJournalFørendeEnhet(journalpostMottakDto.getJournalForendeEnhet());

        journalpostMottakDto.getForsendelseId().ifPresent(builder::medForsendelseId);
        journalpostMottakDto.getPayloadXml().ifPresent(builder::medXmlPayload);
        if (DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId)) {
            journalpostMottakDto.getEksternReferanseId().ifPresent(builder::medKanalreferanse);
        }

        return builder.build();
    }

    private DokumentKategori utledDokumentKategori(String dokumentKategori, DokumentTypeId dokumentTypeId) {
        if (DokumentTypeId.getSøknadTyper().contains(dokumentTypeId)) {
            return DokumentKategori.SØKNAD;
        }
        if (DokumentTypeId.KLAGE_DOKUMENT.equals(dokumentTypeId)) {
            return DokumentKategori.KLAGE_ELLER_ANKE;
        }
        return dokumentKategori != null ? DokumentKategori.finnForKodeverkEiersKode(dokumentKategori) : DokumentKategori.UDEFINERT;
    }

    public static class AbacJournalpostMottakDto extends JournalpostMottakDto implements AbacDto {
        public AbacJournalpostMottakDto() {
            super();
        }

        public AbacJournalpostMottakDto(String saksnummer,
                                        String journalpostId,
                                        String behandlingstemaOffisiellKode,
                                        String dokumentTypeIdOffisiellKode,
                                        LocalDateTime forsendelseMottattTidspunkt,
                                        String payloadXml) {
            super(saksnummer, journalpostId, behandlingstemaOffisiellKode, dokumentTypeIdOffisiellKode, forsendelseMottattTidspunkt, payloadXml);
        }

        @JsonIgnore
        public Optional<String> getPayloadXml() {
            return getPayloadValiderLengde(base64EncodedPayloadXml, payloadLength);
        }

        static Optional<String> getPayloadValiderLengde(String base64EncodedPayload, Integer deklarertLengde) {
            if (base64EncodedPayload == null) {
                return Optional.empty();
            }
            if (deklarertLengde == null) {
                throw new TekniskException("F-217605", "Input-validering-feil: Avsender sendte payload, men oppgav ikke lengde på innhold");
            }
            var bytes = Base64.getUrlDecoder().decode(base64EncodedPayload);
            var streng = new String(bytes, StandardCharsets.UTF_8);
            if (streng.length() != deklarertLengde) {
                throw new TekniskException("F-483098",
                    String.format("Input-validering-feil: Avsender oppgav at lengde på innhold var %s, men lengden var egentlig %s", deklarertLengde,
                        streng.length()));
            }
            return Optional.of(streng);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, getSaksnummer());
        }

    }

    public static class AbacJournalpostKnyttningDto extends JournalpostKnyttningDto implements AbacDto {
        public AbacJournalpostKnyttningDto() {
            super();
        }

        public AbacJournalpostKnyttningDto(SaksnummerDto saksnummerDto, JournalpostIdDto journalpostIdDto) {
            super(saksnummerDto, journalpostIdDto);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.JOURNALPOST_ID, getJournalpostId())
                .leggTil(AppAbacAttributtType.SAKSNUMMER, getSaksnummer());
        }
    }

    public static class AbacVurderFagsystemDto extends VurderFagsystemDto implements AbacDto {
        public AbacVurderFagsystemDto() {
            super();
        }

        public AbacVurderFagsystemDto(String journalpostId, boolean strukturertSøknad, String aktørId, String behandlingstemaOffisiellKode) {
            super(journalpostId, strukturertSøknad, aktørId, behandlingstemaOffisiellKode);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            var abacDataAttributter = AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, getAktørId());

            getJournalpostId().ifPresent(id -> abacDataAttributter.leggTil(AppAbacAttributtType.JOURNALPOST_ID, id));
            getSaksnummer().ifPresent(sn -> abacDataAttributter.leggTil(AppAbacAttributtType.SAKSNUMMER, sn));
            return abacDataAttributter;
        }
    }

    public static class AbacOpprettSakDto extends OpprettSakDto implements AbacDto {
        public AbacOpprettSakDto() {
            super();
        }

        public AbacOpprettSakDto(String journalpostId, String behandlingstemaOffisiellKode, String aktørId) {
            super(journalpostId, behandlingstemaOffisiellKode, aktørId);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, getAktørId());
        }
    }

    public static class AbacSaksnummerDto extends SaksnummerDto implements AbacDto {
        public AbacSaksnummerDto() {
            // for Jackson
        }

        public AbacSaksnummerDto(String saksnummer) {
            super(saksnummer);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, getSaksnummer());
        }

    }

    public static class SakInntektsmeldingDtoAbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SakInntektsmeldingDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.AKTØR_ID, req.bruker().aktørId());
        }
    }
}
