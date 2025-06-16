package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.HoppTilbakeDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningSteg")
@ApplicationScoped
@Transactional
public class ForvaltningStegRestTjeneste {

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    @Inject
    public ForvaltningStegRestTjeneste(BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                       BehandlingRepositoryProvider repositoryProvider,
                                       ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
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
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response hoppTilbake(@BeanParam @Valid HoppTilbakeDto dto) {
        hoppTilbake(dto.getBehandlingUuid(), dto.getBehandlingStegType());
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopp tilbake til 5085", tags = "FORVALTNING-steg-hopp")
    @Path("/5085")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response hoppTilbakeTil5085(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        hoppTilbake(dto.getBehandlingUuid(), KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING);
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner overstyring av familiehendelse og hopper tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernFHValgHoppTilbake")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response fjernOverstyrtFH(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var lås = behandlingRepository.taSkriveLås(dto.getBehandlingUuid());
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        if (grunnlag.getOverstyrtVersjon().isPresent()) {
            familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);
            hoppTilbake(dto.getBehandlingUuid(), KONTROLLER_FAKTA);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Fjerner startpunkt i revurdering og går tilbake til KOFAK", tags = "FORVALTNING-steg-hopp")
    @Path("/fjernStartpunkt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response fjernStartpunkt(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        hoppTilbake(dto.getBehandlingUuid(), KONTROLLER_FAKTA);
        return Response.noContent().build();
    }

    private void resetStartpunkt(Behandling behandling) {
        if (behandling.erRevurdering() && behandling.harSattStartpunkt()) {
            behandling.setStartpunkt(StartpunktType.UDEFINERT);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling.getId()));
        }
    }

    private void hoppTilbake(UUID behandlingUuid, BehandlingStegType tilSteg) {
        var lås = behandlingRepository.taSkriveLås(behandlingUuid);
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        hoppTilbake(behandling, lås, tilSteg);
    }

    private void hoppTilbake(Behandling behandling, BehandlingLås lås, BehandlingStegType tilSteg) {
        if (KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING.equals(tilSteg)) {
            arbeidsforholdInntektsmeldingMangelTjeneste.ryddVekkAlleValgPåBehandling(BehandlingReferanse.fra(behandling));
            arbeidsforholdAdministrasjonTjeneste.fjernOverstyringerGjortAvSaksbehandler(behandling.getId());
            resetStartpunkt(behandling);
        }
        if (KONTROLLER_FAKTA.equals(tilSteg)) {
            resetStartpunkt(behandling);
        }
        behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        if (behandlingProsesseringTjeneste.finnesTasksForPolling(behandling).isEmpty()) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
    }

    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        var fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Behandlingen er flyttet")
            .addLinje(String.format("Behandlingen er flyttet fra __%s__ tilbake til __%s__", fraStegNavn, tilStegNavn))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
