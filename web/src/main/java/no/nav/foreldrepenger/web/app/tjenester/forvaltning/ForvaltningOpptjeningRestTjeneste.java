package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.LeggTilOppgittNæringDto.Utfall.JA;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.math.BigDecimal;
import java.util.List;
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
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response leggTilOppgittFrilans(@BeanParam @Valid LeggTilOppgittFrilansDto dto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        if (iayGrunnlag.getOppgittOpptjening().isPresent() || behandling.erSaksbehandlingAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var nyoppstartet = dto.getStpOpptjening().minusMonths(3).isBefore(dto.getFrilansFom());
        var periode = dto.getFrilansTom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.getFrilansFom(), dto.getFrilansTom())
                : DatoIntervallEntitet.fraOgMed(dto.getFrilansFom());
        var ooBuilder = OppgittOpptjeningBuilder.ny(iayGrunnlag.getEksternReferanse(), iayGrunnlag.getOpprettetTidspunkt())
                .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, ArbeidType.FRILANSER))
                .leggTilFrilansOpplysninger(new OppgittFrilans(false, nyoppstartet, false));
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/leggTilOppgittNæring")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Legg til innslag for oppgitt næring som fisker", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response leggTilOppgittNæring(@Valid LeggTilOppgittNæringDto dto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(dto.behandlingUuid());
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        if (iayGrunnlag.getOppgittOpptjening().isPresent() || behandling.erSaksbehandlingAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Optional<Virksomhet> virksomhet = dto.orgnummer() != null ? virksomhetTjeneste.finnOrganisasjon(dto.orgnummer()) : Optional.empty();
        var brutto = new BigDecimal(dto.bruttoBeløp());
        var periode = dto.tom() != null ? DatoIntervallEntitet.fraOgMedTilOgMed(dto.fom(), dto.tom())
                : DatoIntervallEntitet.fraOgMed(dto.fom());
        var enBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                .medVirksomhetType(VirksomhetType.fraKode(dto.typeKode()))
                .medPeriode(periode)
                .medBruttoInntekt(brutto)
                .medNærRelasjon(false)
                .medNyIArbeidslivet(false)
                .medNyoppstartet(false)
                .medVarigEndring(false)
                .medRegnskapsførerNavn(dto.regnskapNavn())
                .medRegnskapsførerTlf(dto.regnskapTlf());
        virksomhet.ifPresent(v -> enBuilder.medVirksomhet(v.getOrgnr()));
        if (JA.equals(dto.varigEndring())) {
            enBuilder.medVarigEndring(true).medBegrunnelse(dto.begrunnelse()).medEndringDato(dto.endringsDato());
        }
        if (JA.equals(dto.nyoppstartet())) {
            enBuilder.medNyoppstartet(true);
        }
        var ooBuilder = OppgittOpptjeningBuilder.ny(iayGrunnlag.getEksternReferanse(), iayGrunnlag.getOpprettetTidspunkt())
                .leggTilEgneNæringer(List.of(enBuilder));
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), ooBuilder);

        return Response.noContent().build();
    }

    @POST
    @Path("/reInnhentAlleIAYRegisterData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Tvinger full registeroppdatering av IAY på åpen behandling", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response reInnhentAlleIAYRegisterData(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        if (behandling.erSaksbehandlingAvsluttet()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var prosessTaskData = ProsessTaskData.forProsessTask(InnhentIAYIAbakusTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setProperty(InnhentIAYIAbakusTask.OVERSTYR_KEY, InnhentIAYIAbakusTask.OVERSTYR_VALUE);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
        return Response.noContent().build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
    }

    @GET
    @Path("/hentOppgittOpptjening")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent oppgitt opptjening for behandling", tags = "FORVALTNING-opptjening")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public OppgittOpptjeningDto hentOppgittOpptjening(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagKontrakt(behandling.getId());
        return iayGrunnlag.getOppgittOpptjening();
    }
}
