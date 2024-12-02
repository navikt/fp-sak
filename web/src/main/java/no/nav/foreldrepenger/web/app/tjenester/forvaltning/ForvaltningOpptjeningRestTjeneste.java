package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.LeggTilOppgittNæringDto.Utfall.JA;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.abakus.iaygrunnlag.oppgittopptjening.v1.OppgittOpptjeningDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningOpptjening")
@ApplicationScoped
@Transactional
public class ForvaltningOpptjeningRestTjeneste {

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public ForvaltningOpptjeningRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                             VirksomhetTjeneste virksomhetTjeneste,
                                             ProsessTaskTjeneste taskTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.taskTjeneste = taskTjeneste;
    }

    public ForvaltningOpptjeningRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/leggTilOppgittFrilans")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Legg til innslag for oppgitt frilansaktivitet", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response leggTilOppgittFrilans(@BeanParam @Valid LeggTilOppgittFrilansDto dto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
        var oppgittOpptjening = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId()).getGjeldendeOppgittOpptjening();
        var nyoppstartet = dto.getStpOpptjening().minusMonths(3).isBefore(dto.getFrilansFom());
        var periode = dto.getFrilansTom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.getFrilansFom(), dto.getFrilansTom())
                : DatoIntervallEntitet.fraOgMed(dto.getFrilansFom());
        var ooBuilder = OppgittOpptjeningBuilder.oppdater(oppgittOpptjening)
            .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, ArbeidType.FRILANSER))
            .leggTilFrilansOpplysninger(new OppgittFrilans(false, nyoppstartet, false));

        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(behandling.getId(), ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/leggTilOppgittNæring")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Legg til innslag for oppgitt næring som fisker", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response leggTilOppgittNæring(@BeanParam @Valid LeggTilOppgittNæringDto dto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
        var oppgittOpptjening = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId()).getGjeldendeOppgittOpptjening();
        Optional<Virksomhet> virksomhet = dto.getOrgnummer() != null ? virksomhetTjeneste.finnOrganisasjon(dto.getOrgnummer()) : Optional.empty();
        var brutto = new BigDecimal(dto.getBruttoBeløp());
        var periode = dto.getTom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.getFom(), dto.getTom())
                : DatoIntervallEntitet.fraOgMed(dto.getFom());
        var enBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medVirksomhetType(VirksomhetType.fraKode(dto.getTypeKode()))
                .medPeriode(periode)
                .medBruttoInntekt(brutto)
                .medNærRelasjon(tilBoolsk(dto.getErRelasjon()))
                .medNyIArbeidslivet(tilBoolsk(dto.getNyIArbeidslivet()))
                .medNyoppstartet(tilBoolsk(dto.getNyoppstartet()))
                .medVarigEndring(tilBoolsk(dto.getVarigEndring()))
                .medRegnskapsførerNavn(dto.getRegnskapNavn())
                .medBegrunnelse(dto.getBegrunnelse())
                .medEndringDato(dto.getEndringsDato())
                .medRegnskapsførerTlf(dto.getRegnskapTlf());
        virksomhet.ifPresent(v -> enBuilder.medVirksomhet(v.getOrgnr()));
        // Ønsker å erstatte eksisterende egen næring om orgnr er likt
        var ooBuilder = OppgittOpptjeningBuilder.oppdater(oppgittOpptjening)
            .leggTilEllerErstattEgenNæring(enBuilder.build());
        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(behandling.getId(), ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/leggTilOppgittNæringFjerneAndreOppgitteOrgnummer")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Operation(description = "Legg til innslag for oppgitt næring og fjern andre orgnummer", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response leggTilOppgittNæringFjerneAndreOppgitteOrgnummer(@BeanParam @Valid LeggTilOppgittNæringDto dto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
        var oppgittOpptjening = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId()).getGjeldendeOppgittOpptjening();
        Optional<Virksomhet> virksomhet = dto.getOrgnummer() != null ? virksomhetTjeneste.finnOrganisasjon(dto.getOrgnummer()) : Optional.empty();
        var brutto = new BigDecimal(dto.getBruttoBeløp());
        var periode = dto.getTom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.getFom(), dto.getTom())
            : DatoIntervallEntitet.fraOgMed(dto.getFom());
        var enBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhetType(VirksomhetType.fraKode(dto.getTypeKode()))
            .medPeriode(periode)
            .medBruttoInntekt(brutto)
            .medNærRelasjon(tilBoolsk(dto.getErRelasjon()))
            .medNyIArbeidslivet(tilBoolsk(dto.getNyIArbeidslivet()))
            .medNyoppstartet(tilBoolsk(dto.getNyoppstartet()))
            .medVarigEndring(tilBoolsk(dto.getVarigEndring()))
            .medRegnskapsførerNavn(dto.getRegnskapNavn())
            .medBegrunnelse(dto.getBegrunnelse())
            .medEndringDato(dto.getEndringsDato())
            .medRegnskapsførerTlf(dto.getRegnskapTlf());
        virksomhet.ifPresent(v -> enBuilder.medVirksomhet(v.getOrgnr()));
        // Ønsker å erstatte eksisterende egen næring om orgnr er likt
        var ooBuilder = OppgittOpptjeningBuilder.oppdater(oppgittOpptjening)
            .leggTilEllerErstattEgenNæringFjernAndreOrgnummer(enBuilder.build());
        inntektArbeidYtelseTjeneste.lagreOverstyrtOppgittOpptjening(behandling.getId(), ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/reInnhentAlleIAYRegisterData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Tvinger full registeroppdatering av IAY på åpen behandling", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response reInnhentAlleIAYRegisterData(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        if (behandling.erSaksbehandlingAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var prosessTaskData = ProsessTaskData.forProsessTask(InnhentIAYIAbakusTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setProperty(InnhentIAYIAbakusTask.OVERSTYR_KEY, InnhentIAYIAbakusTask.OVERSTYR_VALUE);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
        return Response.noContent().build();
    }

    @GET
    @Path("/hentOppgittOpptjening")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent oppgitt opptjening for behandling", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public OppgittOpptjeningDto hentOppgittOpptjening(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagKontrakt(behandling.getId());
        return iayGrunnlag.getOppgittOpptjening();
    }

    private boolean tilBoolsk(LeggTilOppgittNæringDto.Utfall erRelasjon) {
        return JA.equals(erRelasjon);
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
    }
}
