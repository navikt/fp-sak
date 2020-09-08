package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.anke.impl.AnkeVurderingAdapter;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AnkeMerknaderResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AnkeMerknaderOppdaterer implements AksjonspunktOppdaterer<AnkeMerknaderResultatAksjonspunktDto> {
    private BehandlingRepository behandlingRepository;
    private AnkeRepository ankeRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private static final AksjonspunktDefinisjon AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN = AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;
    private static final long FRIST_VENT_PAA_ANKE_OVERSENDT_TIL_TRYGDERETTEN = 2;

    AnkeMerknaderOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AnkeMerknaderOppdaterer(BehandlingRepository behandlingRepository,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   AnkeRepository ankeRepository,
                                   AnkeVurderingTjeneste ankeVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ankeRepository = ankeRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AnkeMerknaderResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        håndterAnkeVurdering(behandling, dto);
        if (!Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde())) {
            lagBrevTilTrygderettenBasertPåUtfall(dto);
        }
        if (!behandling.harAksjonspunktMedType(AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN)) {
            settAnkebehandlingPåVentFraTrygderettenOgLagHistorikkInnslag(behandling);
        }

        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagBrevTilTrygderettenBasertPåUtfall(AnkeMerknaderResultatAksjonspunktDto dto) { // NOSONAR - Fjern denne når TFP-1152 implementeres
        if(dto.erMerknaderMottatt()) {
            //TODO (Addams): TFP-1152 Ankebrev: Merknader innkommet
            // Gitt at det er kommet inn merknader. Så skal det lages et brev til Trygderetten: "mottatt merknader". Og kopi av dette skal sendes til bruker.
            // det sendes et informasjonsbrev om ytterligere merknader til bruker
        } else {
            //TODO (Addams): TFP-1152 Ankebrev: Merknader ikke innkommet
            // Gitt at det ikke er kommet inn merknader. Så skal det lages et brev til Trygderetten: "ikke mottatt merknader". Og kopi av dette skal sendes til bruker
        }
    }

    private void settAnkebehandlingPåVentFraTrygderettenOgLagHistorikkInnslag(Behandling behandling) {
        LocalDate settPåVentTom = LocalDate.now().plusYears(FRIST_VENT_PAA_ANKE_OVERSENDT_TIL_TRYGDERETTEN);
        LocalDateTime frist = LocalDateTime.of(settPåVentTom, LocalTime.MIDNIGHT);
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN, BehandlingStegType.ANKE_MERKNADER,
            frist, Venteårsak.ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER);
    }

    private void håndterAnkeVurdering(Behandling behandling, AnkeMerknaderResultatAksjonspunktDto dto) {
        Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        if (ankeVurderingResultat.isPresent()) {
            boolean erGodkjentAvMedunderskriver = ankeVurderingResultat.get().godkjentAvMedunderskriver();
            AnkeVurderingResultatEntitet vurderingResultat = ankeVurderingResultat.get();

            final AnkeVurderingAdapter adapter = new AnkeVurderingAdapter.Builder()
                .medAnkeVurderingKode(vurderingResultat.getAnkeVurdering().getKode())
                .medBegrunnelse(vurderingResultat.getBegrunnelse())
                .medAnkeOmgjoerArsakKode(getAnkeOmgjørÅrsak(vurderingResultat))
                .medFritekstTilBrev(vurderingResultat.getFritekstTilBrev())
                .medAnkeVurderingOmgjoer(getAnkeVurderingOmgjør(vurderingResultat))
                .medErSubsidiartRealitetsbehandles(vurderingResultat.erSubsidiartRealitetsbehandles())
                .medErGodkjentAvMedunderskriver(erGodkjentAvMedunderskriver)
                .medErAnkerIkkePart(vurderingResultat.erAnkerIkkePart())
                .medErFristIkkeOverholdt(vurderingResultat.erFristIkkeOverholdt())
                .medErIkkeKonkret(vurderingResultat.erIkkeKonkret())
                .medErIkkeSignert(vurderingResultat.erIkkeSignert())
                .medPaaAnketBehandlingId(getPåAnketBehandlingId(vurderingResultat))
                .medMerknaderFraBruker(dto.getMerknadKommentar())
                .medErMerknaderMottatt(dto.erMerknaderMottatt())
                .build();

            ankeVurderingTjeneste.oppdater(behandling, adapter);
        }

    }

    private long getPåAnketBehandlingId(AnkeVurderingResultatEntitet vurderingResultat) {
        return vurderingResultat.getAnkeResultat().getPåAnketBehandling().map(Behandling :: getId).orElse(null);
    }

    private String getAnkeOmgjørÅrsak(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        return ankeVurderingResultat.getAnkeOmgjørÅrsak() == null ? null : ankeVurderingResultat.getAnkeOmgjørÅrsak().getKode();
    }

    private String getAnkeVurderingOmgjør(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        return ankeVurderingResultat.getAnkeVurderingOmgjør() == null ? null : ankeVurderingResultat.getAnkeVurderingOmgjør().getKode();
    }
}
