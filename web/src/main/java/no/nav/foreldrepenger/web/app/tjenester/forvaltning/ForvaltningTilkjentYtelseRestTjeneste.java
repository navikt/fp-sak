package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.EndreTilbaketrekkValgDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningTilkjentYtelse")
@ApplicationScoped
@Transactional
public class ForvaltningTilkjentYtelseRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningTilkjentYtelseRestTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private HistorikkRepository historikkRepository;

    @Inject
    public ForvaltningTilkjentYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                                 BeregningsresultatRepository beregningsresultatRepository,
                                                 BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                 BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                                 HistorikkRepository historikkRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.historikkRepository = historikkRepository;
    }

    public ForvaltningTilkjentYtelseRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/endreValgAvTilbaketrekk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer valg som er gjort i tilkjent ytelse angående tilbaketrekk for å endre revurderinger av gamle saker. " +
        "Tidligere kjent som aksjonspunkt 5090 og nå avviklet." +
        "True = tilbaketrekk hindres ved at tilkjent ytelse omfordeles." +
        "False = tilbaketrekk hindres ikke og det som er utregnet vil gå videre til oppdrag", tags = "FORVALTNING-tilkjentYtelse")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response endreTilbaketrekksvalg(@BeanParam @Valid EndreTilbaketrekkValgDto dto) {
        if (dto.getBehandlingUuid() == null) {
            return Response.noContent().build();
        }
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        Optional<BehandlingBeregningsresultatEntitet> eksisterendeAggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        Optional<Boolean> eksisterendeTilbaketrekkvalg = eksisterendeAggregat.flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
        // Sjekker at vi ikke har ugyldig tilstand for denne tjenesten
        if (behandling.erAvsluttet()) {
            return tekst("Behandlingen er avsluttet og kan ikke flyttes");
        } else if (behandling.isBehandlingPåVent()) {
            return tekst("Behandlingen er på vent og kan ikke flyttes");
        } else if (eksisterendeTilbaketrekkvalg.isEmpty()) {
            return tekst("Behandlingen har ikke et aktivt beregningsresultat eller har" +
                " ikke vurdert om tilbakekreving skal hindres og kan derfor ikke endres");
        } else if (eksisterendeTilbaketrekkvalg.get().equals(dto.getSkalHindreTilbaketrekk())) {
            return tekst("Ny og gammel besluttning for å utføre tilbaketrekk var likt, ikke nødvendig med endring");
        }

        LOG.info("Endrer tilbakekrevingsvalg for behandling {}, gammelt valg var {} mens nytt valg er {}",
            behandling.getId(), eksisterendeTilbaketrekkvalg.get(), dto.getSkalHindreTilbaketrekk());

        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, dto.getSkalHindreTilbaketrekk());
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        BehandlingStegType tilbaketrekkSteg = BehandlingStegType.HINDRE_TILBAKETREKK;
        lagHistorikkinnslag(behandling, tilbaketrekkSteg.getNavn());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilbaketrekkSteg);
        behandlingsprosessTjeneste.asynkKjørProsess(behandling);
        return Response.ok().build();
    }

    private Response tekst(String tekst) {
        return Response.ok(tekst, MediaType.TEXT_PLAIN_TYPE).build();
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
