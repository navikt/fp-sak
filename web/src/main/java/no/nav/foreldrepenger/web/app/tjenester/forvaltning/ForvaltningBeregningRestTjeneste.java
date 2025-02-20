package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.domene.migrering.MigrerBeregningSakTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.satsregulering.GrunnbeløpReguleringTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BeregningSatsDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.StoppRefusjonDto;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transactional
public class ForvaltningBeregningRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SatsRepository satsRepository;

    @Inject
    public ForvaltningBeregningRestTjeneste(ProsessTaskTjeneste taskTjeneste,
                                            BehandlingRepository behandlingRepository,
                                            FagsakRepository fagsakRepository,
                                            BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider,
                                            BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            SatsRepository satsRepository) {
        this.taskTjeneste = taskTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.beregningsgrunnlagInputProvider = beregningsgrunnlagInputProvider;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.iayTjeneste = iayTjeneste;
        this.satsRepository = satsRepository;
    }

    public ForvaltningBeregningRestTjeneste() {
        // CDI
    }

    @GET
    @Path("/satsHentGjeldende")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av gjeldende eller nyeste sats", tags = "FORVALTNING-beregning", responses = {@ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))})
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT)
    public List<BeregningSatsDto> hentGjeldendeSatser() {
        return Set.of(BeregningSatsType.ENGANG, BeregningSatsType.GRUNNBELØP, BeregningSatsType.GSNITT)
            .stream()
            .map(satsRepository::finnGjeldendeSats)
            .map(BeregningSatsDto::new)
            .toList();
    }

    @POST
    @Path("/satsLagreNy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre ny sats", tags = "FORVALTNING-beregning", responses = {@ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))})
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public List<BeregningSatsDto> lagreNySats(@BeanParam @Valid @NotNull BeregningSatsDto dto) {
        var type = dto.getSatsType();
        var brukTom = dto.getSatsTom() != null ? dto.getSatsTom() : LocalDate.now().plusYears(99);
        var gjeldende = satsRepository.finnGjeldendeSats(type);
        var eksisterende = new BeregningSatsDto(gjeldende);

        if (Objects.equals(gjeldende.getPeriode().getFomDato(), dto.getSatsFom())) {
            // Ved behov for å oppdatere gjeldende sats verdi eller tom
            if (!Objects.equals(gjeldende.getVerdi(), dto.getSatsVerdi()) && brukTom.isAfter(gjeldende.getPeriode().getFomDato())) {
                LOG.warn("SATSJUSTERTING oppdatering: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
                gjeldende.setVerdi(dto.getSatsVerdi());
                gjeldende.setTomDato(brukTom);
                satsRepository.lagreSats(gjeldende);
            } else {
                throw new ForvaltningException("Ulovlige verdier " + dto);
            }
        } else {
            // Nytt innslag. Sett sluttdato på gjeldende og legg til ny
            if (!sjekkVerdierOK(dto, gjeldende, brukTom))
                throw new ForvaltningException("Ulovlige verdier " + dto);
            LOG.warn("SATSJUSTERTING: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
            gjeldende.setTomDato(dto.getSatsFom().minusDays(1));
            satsRepository.lagreSats(gjeldende);
            var nysats = new BeregningSats(type, DatoIntervallEntitet.fraOgMedTilOgMed(dto.getSatsFom(), brukTom), dto.getSatsVerdi());
            satsRepository.lagreSats(nysats);
        }
        var nygjeldende = satsRepository.finnGjeldendeSats(type);
        return List.of(eksisterende, new BeregningSatsDto(nygjeldende));
    }

    private boolean sjekkVerdierOK(BeregningSatsDto dto, BeregningSats gjeldende, LocalDate brukTom) {
        if (!brukTom.isAfter(dto.getSatsFom()) || !dto.getSatsFom().isAfter(gjeldende.getPeriode().getFomDato()))
            return false;
        if (BeregningSatsType.GRUNNBELØP.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom()) && Month.MAY.equals(dto.getSatsFom().getMonth()) && dto.getSatsFom().getDayOfMonth() == 1;
        }
        if (BeregningSatsType.ENGANG.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom());
        }
        // GSNITT skal være bounded
        return dto.getSatsTom() != null && dto.getSatsFom().equals(gjeldende.getPeriode().getTomDato().plusDays(1)) && dto.getSatsTom()
            .equals(dto.getSatsFom().plusYears(1).minusDays(1));
    }

    @POST
    @Path("/opprettGreguleringEnkeltSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Steng fagsak og flytt til Infotrygd", tags = "FORVALTNING-fagsak", responses = {@ApiResponse(responseCode = "200", description = "Flyttet fagsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))), @ApiResponse(responseCode = "400", description = "Ukjent fagsak oppgitt."), @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil.")})
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK)
    public Response opprettGreguleringEnkeltSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        var saksnummer = new Saksnummer(saksnummerDto.getVerdi());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var åpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId())
            .stream().anyMatch(SpesialBehandling::erIkkeSpesialBehandling);
        if (no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()) || åpneBehandlinger) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var prosessTaskData = ProsessTaskData.forProsessTask(GrunnbeløpReguleringTask.class);
        prosessTaskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        prosessTaskData.setProperty(GrunnbeløpReguleringTask.MANUELL_KEY, "true");
        taskTjeneste.lagre(prosessTaskData);
        return Response.ok().build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingRepository.hentBehandling(dto.getBehandlingUuid());
    }

    @POST
    @Path("/stoppRefusjon")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter opphørsdato for refusjon for en gitt journalpost", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opphørRefusjonInntektsmelding(@BeanParam @Valid StoppRefusjonDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        var inntektsmeldinger = iayTjeneste.finnGrunnlag(behandling.getId())
            .flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var matchetInntektsmelding = inntektsmeldinger.stream().filter(im -> im.getJournalpostId().getVerdi().equals(dto.getJournalpostId())).findFirst();
        if (matchetInntektsmelding.isEmpty()) {
            var msg = String.format("Finner ikke inntektsmelding med journalpostId %s på behandling med uuid %s ", dto.getJournalpostId(),
                dto.getBehandlingUuid());
            return Response.ok(msg).build();
        }
        var task = ProsessTaskData.forProsessTask(OverstyrInntektsmeldingTask.class);
        task.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        task.setProperty(OverstyrInntektsmeldingTask.BEHANDLING_ID, behandling.getId().toString());
        task.setProperty(OverstyrInntektsmeldingTask.JOURNALPOST_ID, dto.getJournalpostId());
        task.setProperty(OverstyrInntektsmeldingTask.OPPHØR_FOM, dto.getRefusjonOpphørFom().toString());
        task.setProperty(OverstyrInntektsmeldingTask.SAKSBEHANDLER_IDENT, KontekstHolder.getKontekst().getUid());
        taskTjeneste.lagre(task);
        return Response.ok().build();
    }

    @POST
    @Path("/hentRefusjonskravperioderInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentRefusjonskravperioderInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandling.getFagsakYtelseType());
        var beregningsgrunnlagInput = inputTjeneste.lagInput(BehandlingReferanse.fra(behandling));
        if (beregningsgrunnlagInput == null) {
            return Response.noContent().build();
        }
        var json = StandardJsonConfig.toJson(beregningsgrunnlagInput.getKravPrArbeidsgiver());
        return Response.ok(json).build();
    }

    @POST
    @Path("/hentBeregningsgrunnlagInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentBeregningsgrunnlagInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandling.getFagsakYtelseType());
        var beregningsgrunnlagInput = inputTjeneste.lagInput(BehandlingReferanse.fra(behandling));
        var kalkulatorInputDto = MapTilKalkulatorInput.map(beregningsgrunnlagInput);
        if (kalkulatorInputDto == null) {
            return Response.noContent().build();
        }
        return Response.ok(kalkulatorInputDto).build();
    }

    @POST
    @Path("/migrerSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Migrerer en sak over til kalkulus", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response migrerSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto dto) {
        var migreringstask = ProsessTaskData.forProsessTask(MigrerBeregningSakTask.class);
        migreringstask.setProperty(MigrerBeregningSakTask.SAKSNUMMER_TASK_KEY, dto.getVerdi());
        taskTjeneste.lagre(migreringstask);
        return Response.ok().build();
    }

    @POST
    @Path("/sjekkDiffInntektRegisterMotInntektsmelding")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter ut behandlinger med diff på inntekt register og IM for en periode", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hentInfOmBehandlingerMedDiff() {
        LocalDateTime fraDatoTid = LocalDateTime.now().minusMonths(2);

        //Hent alle behandlinger som er vedtatt på foreldrepegner og har 1 arbeidsforhold i perioden
        List<Fagsak> fagsaker = fagsakRepository.finnLøpendeFagsakerFPForEnPeriode(fraDatoTid, fraDatoTid.plusDays(1));
        List<DiffInntektIMData> resultatAvDiffInntektImData = new ArrayList<>();

        LOG.info("sjekkDiffInntektRegisterMotInntektsmelding: Antall saker funnet: {}", fagsaker.size());

        List<DetaljerMedDiff> detaljerMedDiffs = new ArrayList<>();
        int tellerDiff = 0;
        int tellerUtenDiff = 0;

        for (var fagsak : fagsaker) {
            Behandling behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElse(null);

            if (behandling != null) {
                var behandlingId = behandling.getId();
                var beregningsgrunnlagPerStatusOgAndelListe = getBeregningsgrunnlagPerStatusOgAndelForArbeidsgivere(behandlingId);
                //ønsker kun å hente de med 1 arbeidsforhold
                if (beregningsgrunnlagPerStatusOgAndelListe.size() == 1) {
                    try {
                        var beregningsgrunnlagPerStatusOgAndel = beregningsgrunnlagPerStatusOgAndelListe.get(0);
                        var arbeidsgiver = beregningsgrunnlagPerStatusOgAndel.getArbeidsgiver()
                            .orElseThrow(() -> new IllegalStateException("Arbeidsgiverandel mangler arbeidsgiver"));
                        DatoIntervallEntitet beregningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(
                            beregningsgrunnlagPerStatusOgAndel.getBeregningsperiodeFom(),
                            beregningsgrunnlagPerStatusOgAndel.getBeregningsperiodeTom());
                        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);

                        var beløpFraIM = hentBeregnetBeløpFraIm(inntektArbeidYtelseGrunnlag, arbeidsgiver);
                        var inntekterIBeregningsperioden = hentInntekterFraAInntektIBeregningsperioden(behandling, arbeidsgiver, beregningsperiode,
                            inntektArbeidYtelseGrunnlag);
                        var sumInntekterAInntekt = inntekterIBeregningsperioden.stream()
                            .map(Inntektspost::getBeløp)
                            .filter(Objects::nonNull)
                            .reduce(Beløp::adder)
                            .orElse(Beløp.ZERO);

                        if (sumInntekterAInntekt != Beløp.ZERO) {
                            var gjennomsnittInntektAInntekt = sumInntekterAInntekt.getVerdi()
                                .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_EVEN);
                            if (beløpFraIM.compareTo(gjennomsnittInntektAInntekt) != 0) {
                                tellerDiff++;
                                detaljerMedDiffs.add(new DetaljerMedDiff(fagsak.getSaksnummer().getVerdi(), behandling.getUuid(), beregningsperiode,
                                    gjennomsnittInntektAInntekt, beløpFraIM, beløpFraIM.subtract(gjennomsnittInntektAInntekt),
                                    mapInntekter(inntekterIBeregningsperioden)));
                            } else {
                                tellerUtenDiff++;
                            }
                        }
                    } catch (TekniskException e) {
                        if (e.getMessage().contains("MANGLER_TILGANG_FEIL")) {
                            LOG.info("Mangler tilgang, fortsetter");
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }
        resultatAvDiffInntektImData.add(new DiffInntektIMData(tellerUtenDiff, tellerDiff, detaljerMedDiffs));

        LOG.info("Resultat av sjekkDiffInntektRegisterMotInntektsmelding {}", resultatAvDiffInntektImData);
        return Response.ok(resultatAvDiffInntektImData).build();
    }

    private static List<Inntektspost> hentInntekterFraAInntektIBeregningsperioden(Behandling behandling,
                                                                      Arbeidsgiver arbeidsgiver,
                                                                      DatoIntervallEntitet beregningsperiode,
                                                                      InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(behandling.getAktørId())
            .map(AktørInntekt::getInntekt)
            .orElse(Collections.emptyList())
            .stream()
            .filter(innt -> innt.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .filter(innt -> innt.getArbeidsgiver() != null && innt.getArbeidsgiver().equals(arbeidsgiver))
            .map(Inntekt::getAlleInntektsposter)
            .flatMap(Collection::stream)
            .filter(inntektsposts -> inntektsposts.getPeriode().overlapper(beregningsperiode))
            .filter(inntektspost -> !inntektspost.getBeløp().erNullEllerNulltall())
            .toList();
    }

    private static BigDecimal hentBeregnetBeløpFraIm(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, Arbeidsgiver arbeidsgiver) {
        return inntektArbeidYtelseGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(Collection::stream)
            .filter(inntektsmelding -> inntektsmelding.getArbeidsgiver().equals(arbeidsgiver))
            .findFirst()
            .map(inntektsmelding -> inntektsmelding.getInntektBeløp().getVerdi())
            .orElse(BigDecimal.ZERO);
    }

    private List<BeregningsgrunnlagPrStatusOgAndel> getBeregningsgrunnlagPerStatusOgAndelForArbeidsgivere(Long behandlingId) {
        //filtrere på de som har en arbeidsgiver
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingId)
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .stream()
            .findFirst()
            .stream()
            .flatMap(Collection::stream)
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .flatMap(Collection::stream)
            .filter(beregningsgrunnlagPrStatusOgAndel -> beregningsgrunnlagPrStatusOgAndel.getArbeidsgiver().isPresent())
            .toList();
    }

    private List<InntektPerioderRegister> mapInntekter(List<Inntektspost> inntekterIBeregningsperioden) {
        List<InntektPerioderRegister> inntekterFraRegister = new ArrayList<>();
        inntekterIBeregningsperioden.forEach(inntektspost -> inntekterFraRegister.add(new InntektPerioderRegister(inntektspost.getPeriode(), inntektspost.getBeløp().getVerdi())));
        return inntekterFraRegister;
    }

    record DiffInntektIMData(int antallUtendiff, int antallMedDiff, List<DetaljerMedDiff> listeOverAvvikeneIperioden) {
    }

    record DetaljerMedDiff(String saksnummer, UUID behandlingUuid, DatoIntervallEntitet beregningsperiode, BigDecimal gjennsnittAbakus, BigDecimal beløpFraIm, BigDecimal differanse, List<InntektPerioderRegister> inntPerioder) {
    }

    record InntektPerioderRegister(DatoIntervallEntitet periode, BigDecimal beløp) {
    }
}
