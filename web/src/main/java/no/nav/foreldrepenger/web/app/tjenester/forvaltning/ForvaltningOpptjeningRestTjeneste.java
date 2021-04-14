package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.foreldrepenger.domene.abakus.mapping.KodeverkMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.LeggTilOppgittFrilansDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.LeggTilOppgittNæringDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningOpptjening")
@ApplicationScoped
@Transactional
public class ForvaltningOpptjeningRestTjeneste {

    private static Map<String, VirksomhetType> næringTypeKodeMap = Map.of(
            "A", VirksomhetType.ANNEN,
            "D", VirksomhetType.DAGMAMMA,
            "F", VirksomhetType.FISKE,
            "J", VirksomhetType.JORDBRUK_SKOGBRUK);

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public ForvaltningOpptjeningRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                             VirksomhetTjeneste virksomhetTjeneste,
                                             ProsessTaskRepository prosessTaskRepository) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public ForvaltningOpptjeningRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/leggTilOppgittFrilans")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Legg til innslag for oppgitt frilansaktivitet", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response leggTilOppgittFrilans(@BeanParam @Valid LeggTilOppgittFrilansDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        if (iayGrunnlag.getOppgittOpptjening().isPresent() || behandling.erSaksbehandlingAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var nyoppstartet = dto.getStpOpptjening().minusMonths(3).isBefore(dto.getFrilansFom());
        var periode = dto.getFrilansTom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.getFrilansFom(), dto.getFrilansTom())
                : DatoIntervallEntitet.fraOgMed(dto.getFrilansFom());
        var ooBuilder = OppgittOpptjeningBuilder.ny(iayGrunnlag.getEksternReferanse(), iayGrunnlag.getOpprettetTidspunkt())
                .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, ArbeidType.FRILANSER))
                .leggTilFrilansOpplysninger(new OppgittFrilans(false, nyoppstartet, false));
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandlingId, ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/leggTilOppgittNæring")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Legg til innslag for oppgitt næring som fisker", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response leggTilOppgittNæring(@BeanParam @Valid LeggTilOppgittNæringDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        if (iayGrunnlag.getOppgittOpptjening().isPresent() || behandling.erSaksbehandlingAvsluttet()
                || næringTypeKodeMap.get(dto.getTypeKode()) == null
                || ("J".equals(dto.getVarigEndring()) && dto.getEndringsDato() == null)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Optional<Virksomhet> virksomhet = dto.getOrgnummer() != null ? virksomhetTjeneste.finnOrganisasjon(dto.getOrgnummer()) : Optional.empty();
        var brutto = new BigDecimal(Long.parseLong(dto.getBruttoBeløp()));
        var periode = dto.getTom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.getFom(), dto.getTom())
                : DatoIntervallEntitet.fraOgMed(dto.getFom());
        var enBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medVirksomhetType(næringTypeKodeMap.get(dto.getTypeKode()))
                .medPeriode(periode)
                .medBruttoInntekt(brutto)
                .medNærRelasjon(false)
                .medNyIArbeidslivet(false)
                .medNyoppstartet(false)
                .medVarigEndring(false)
                .medRegnskapsførerNavn(dto.getRegnskapNavn())
                .medRegnskapsførerTlf(dto.getRegnskapTlf());
        virksomhet.ifPresent(v -> enBuilder.medVirksomhet(v.getOrgnr()));
        if ("J".equals(dto.getVarigEndring())) {
            enBuilder.medVarigEndring(true).medBegrunnelse(dto.getBegrunnelse()).medEndringDato(dto.getEndringsDato());
        }
        if ("J".equals(dto.getNyoppstartet())) {
            enBuilder.medNyoppstartet(true);
        }
        var ooBuilder = OppgittOpptjeningBuilder.ny(iayGrunnlag.getEksternReferanse(), iayGrunnlag.getOpprettetTidspunkt())
                .leggTilEgneNæringer(List.of(enBuilder));
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandlingId, ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/reInnhentAlleIAYRegisterData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Tvinger full registeroppdatering av IAY på åpen behandling", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response reInnhentAlleIAYRegisterData(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        if (behandling.erSaksbehandlingAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var prosessTaskData = new ProsessTaskData(InnhentIAYIAbakusTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandlingId, behandling.getAktørId().getId());
        prosessTaskData.setProperty(InnhentIAYIAbakusTask.OVERSTYR_KEY, InnhentIAYIAbakusTask.OVERSTYR_VALUE);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
        return Response.noContent().build();
    }

    @GET
    @Path("/hentOppgittOpptjening")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent oppgitt opptjening for behandling", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public OppgittOpptjeningDto hentOppgittOpptjening(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        return new IAYTilDtoMapper(behandling.getAktørId(), KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()),
                iayGrunnlag.getEksternReferanse(), behandling.getUuid()).mapTilDto(iayGrunnlag)
                        .getOppgittOpptjening();

    }
}
