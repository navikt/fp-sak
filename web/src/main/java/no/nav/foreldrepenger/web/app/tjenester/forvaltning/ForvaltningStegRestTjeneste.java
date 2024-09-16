package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.HoppTilbakeDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningSteg")
@ApplicationScoped
@Transactional
public class ForvaltningStegRestTjeneste {

    private EntityManager entityManager;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private HistorikkRepository historikkRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    @Inject
    public ForvaltningStegRestTjeneste(EntityManager entityManager,
                                       BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                       BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                       BehandlingRepositoryProvider repositoryProvider,
                                       ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.entityManager = entityManager;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    public ForvaltningStegRestTjeneste() {
        // CDI
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Generelt tilbakehopp", tags = "FORVALTNING-steg-hopp")
    @Path("/generell")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hoppTilbake(@BeanParam @Valid HoppTilbakeDto dto) {
        hoppTilbake(dto.getBehandlingUuid(), dto.getBehandlingStegType());
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopp tilbake til 5085", tags = "FORVALTNING-steg-hopp")
    @Path("/5085")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response hoppTilbakeTil5085(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        hoppTilbake(dto.getBehandlingUuid(), KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING);
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner overstyring av familiehendelse og hopper tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernFHValgHoppTilbake")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response fjernOverstyrtFH(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(dto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregat(kontekst.getBehandlingId());
        if (grunnlag.getOverstyrtVersjon().isPresent()) {
            familieHendelseRepository.slettAvklarteData(kontekst.getBehandlingId(), kontekst.getSkriveLås());
            hoppTilbake(dto.getBehandlingUuid(), KONTROLLER_FAKTA);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner startpunkt i revurdering og går tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernStartpunkt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response fjernStartpunkt(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        hoppTilbake(dto.getBehandlingUuid(), KONTROLLER_FAKTA);
        return Response.noContent().build();
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter medlemsrelaterte aksjonspunkt til avbrutt", tags = "FORVALTNING-steg-hopp")
    @Path("/avbrytMedlemsAksjonspunkt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response avbrytMedlemsAksjonspunkt() {
        var medlemsaksjonspunkt = List.of(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT,
            AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT, AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET);
        entityManager.createQuery("select behandling from Aksjonspunkt ap where ap.aksjonspunktDefinisjon in (:apdef) and ap.status = :opprettet", Behandling.class)
            .setParameter("apdef",medlemsaksjonspunkt)
            .setParameter("opprettet", AksjonspunktStatus.OPPRETTET)
            .getResultList()
            .forEach(behandling -> {
                var lås = behandlingRepository.taSkriveLås(behandling.getId());
                var aksjonspunkt = behandling.getÅpneAksjonspunkter(medlemsaksjonspunkt);
                var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
                behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), aksjonspunkt);
                behandlingRepository.lagre(behandling, lås);
                if (!behandling.isBehandlingPåVent()) {
                    behandlingsprosessTjeneste.asynkKjørProsess(behandling);
                }
            });

        return Response.ok().build();
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Setter fortsatt medlemsrelaterte aksjonspunkt til avbrutt", tags = "FORVALTNING-steg-hopp")
    @Path("/avbrytFortsattMedlemAksjonspunkt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response avbrytLøpendeMedlemsAksjonspunkt() {
        entityManager.createQuery("select behandling from Aksjonspunkt ap where ap.aksjonspunktDefinisjon = :apdef and ap.status = :opprettet", Behandling.class)
            .setParameter("apdef", AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP)
            .setParameter("opprettet", AksjonspunktStatus.OPPRETTET)
            .getResultList()
            .forEach(behandling -> {
                var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
                hoppTilbake(kontekst, behandling, KONTROLLER_FAKTA);
            });

        return Response.ok().build();
    }


    private void resetStartpunkt(Behandling behandling) {
        if (behandling.erRevurdering() && behandling.harSattStartpunkt()) {
            behandling.setStartpunkt(StartpunktType.UDEFINERT);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        }
    }

    private void hoppTilbake(UUID behandlingUuid, BehandlingStegType tilSteg) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingUuid);
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingUuid);
        hoppTilbake(kontekst, behandling, tilSteg);
    }

    private void hoppTilbake(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingStegType tilSteg) {
        if (KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING.equals(tilSteg)) {
            arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkAlleValgPåBehandling(BehandlingReferanse.fra(behandling));
            arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(behandling.getId());
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
