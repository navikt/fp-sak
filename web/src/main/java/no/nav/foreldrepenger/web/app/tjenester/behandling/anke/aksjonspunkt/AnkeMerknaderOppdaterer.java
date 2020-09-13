package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AnkeMerknaderResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AnkeMerknaderOppdaterer implements AksjonspunktOppdaterer<AnkeMerknaderResultatAksjonspunktDto> {

    private static final AksjonspunktDefinisjon VENT_TRYGDERETT = AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;

    private BehandlingRepository behandlingRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;

    AnkeMerknaderOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AnkeMerknaderOppdaterer(BehandlingRepository behandlingRepository,
                                   AnkeVurderingTjeneste ankeVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AnkeMerknaderResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        håndterAnkeVurdering(behandling, dto);
        if (!Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde())) {
            lagBrevTilTrygderettenBasertPåUtfall(dto);
        }
        var builder = OppdateringResultat.utenTransisjon();
        if (!behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN)) {
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENT_TRYGDERETT, Venteårsak.ANKE_OVERSENDT_TIL_TRYGDERETTEN,
                LocalDateTime.now().plus(Objects.requireNonNull(VENT_TRYGDERETT.getFristPeriod())));
            builder.medEkstraAksjonspunktResultat(apVent, AksjonspunktStatus.OPPRETTET);
        }
        return builder.build();
    }

    private void lagBrevTilTrygderettenBasertPåUtfall(AnkeMerknaderResultatAksjonspunktDto dto) { // NOSONAR - Fjern denne når TFP-1152 implementeres
        if (dto.erMerknaderMottatt()) {
            //TODO (Addams): TFP-1152 Ankebrev: Merknader innkommet
            // Gitt at det er kommet inn merknader. Så skal det lages et brev til Trygderetten: "mottatt merknader". Og kopi av dette skal sendes til bruker.
            // det sendes et informasjonsbrev om ytterligere merknader til bruker
        } else {
            //TODO (Addams): TFP-1152 Ankebrev: Merknader ikke innkommet
            // Gitt at det ikke er kommet inn merknader. Så skal det lages et brev til Trygderetten: "ikke mottatt merknader". Og kopi av dette skal sendes til bruker
        }
    }

    private void håndterAnkeVurdering(Behandling behandling, AnkeMerknaderResultatAksjonspunktDto dto) {
        ankeVurderingTjeneste.oppdaterBekreftetMerknaderAksjonspunkt(behandling, dto.erMerknaderMottatt(), dto.getMerknadKommentar());
    }

}
