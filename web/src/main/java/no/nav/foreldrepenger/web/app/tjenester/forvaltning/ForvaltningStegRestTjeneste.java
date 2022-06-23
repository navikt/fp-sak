package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.HoppTilbakeDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningSteg")
@ApplicationScoped
@Transactional
public class ForvaltningStegRestTjeneste {

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private HistorikkRepository historikkRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;

    @Inject
    public ForvaltningStegRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                       BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                       BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    }

    public ForvaltningStegRestTjeneste() {
        // CDI
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Generelt tilbakehopp", tags = "FORVALTNING-steg-hopp")
    @Path("/generell")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hoppTilbake(@BeanParam @Valid HoppTilbakeDto dto) {
        hoppTilbake(dto, dto.getBehandlingStegType());
        return Response.ok().build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        return behandlingsprosessTjeneste.hentBehandling(dto.getBehandlingUuid());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopp tilbake til 5085", tags = "FORVALTNING-steg-hopp")
    @Path("/5085")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hoppTilbakeTil5085(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        hoppTilbake(dto, KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING);

        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjern aktiviteter lagt til i opptjening", tags = "FORVALTNING-steg-hopp")
    @Path("/fjern-opptjening-extra-aktivitet")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response fjerneAlleNyeAktiviteterFraOpptjening(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandlingId = getBehandling(dto).getId();
        arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandlerOpptjening(behandlingId);

        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner overstyring av familiehendelse og hopper tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernFHValgHoppTilbake")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response fjernOverstyrtFH(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(dto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregat(kontekst.getBehandlingId());
        if (grunnlag.getOverstyrtVersjon().isPresent()) {
            familieHendelseRepository.slettAvklarteData(kontekst.getBehandlingId(), kontekst.getSkriveLås());
            hoppTilbake(dto, KONTROLLER_FAKTA);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner startpunkt i revurdering og går tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernStartpunkt")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response fjernStartpunkt(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        hoppTilbake(dto, KONTROLLER_FAKTA);
        return Response.noContent().build();
    }


    private void resetStartpunkt(Behandling behandling) {
        if (behandling.erRevurdering() && behandling.harSattStartpunkt()) {
            behandling.getOriginalBehandlingId()
                .filter(id -> opptjeningRepository.finnOpptjening(id).isPresent())
                .ifPresent(originalId -> {
                    var origBehandling = behandlingRepository.hentBehandling(originalId);
                    opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, behandling);

            });
            behandling.setStartpunkt(StartpunktType.UDEFINERT);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        }
    }

    private void hoppTilbake(ForvaltningBehandlingIdDto dto, BehandlingStegType tilSteg) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(dto.getBehandlingUuid());
        var behandling = getBehandling(dto);
        if (KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING.equals(tilSteg)) {
            arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(behandling.getId(), behandling.getAktørId());
            resetStartpunkt(behandling);
        }
        if (KONTROLLER_FAKTA.equals(tilSteg)) {
            resetStartpunkt(behandling);
        }
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        behandlingsprosessTjeneste.asynkKjørProsess(behandling);
    }

    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        var fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        var historieBuilder = new HistorikkInnslagTekstBuilder()
                .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
                .medBegrunnelse("Behandlingen er flyttet fra " + fraStegNavn + " tilbake til " + tilStegNavn);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }
}
