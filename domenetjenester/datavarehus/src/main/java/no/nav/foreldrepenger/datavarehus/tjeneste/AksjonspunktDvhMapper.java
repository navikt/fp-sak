package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.datavarehus.domene.AksjonspunktDvh;

import java.time.LocalDateTime;
import java.util.Optional;

class AksjonspunktDvhMapper {

    private AksjonspunktDvhMapper() {
    }

    static AksjonspunktDvh map(Aksjonspunkt aksjonspunkt, Behandling behandling, Optional<BehandlingStegTilstand> behandlingStegTilstand, boolean aksjonspunktGodkjennt) {
        return AksjonspunktDvh.builder()
            .aksjonspunktDef(aksjonspunkt.getAksjonspunktDefinisjon().getKode())
            .aksjonspunktId(aksjonspunkt.getId())
            .aksjonspunktStatus(aksjonspunkt.getStatus().getKode())
            .ansvarligBeslutter(behandling.getAnsvarligBeslutter())
            .ansvarligSaksbehandler(behandling.getAnsvarligSaksbehandler())
            .behandlendeEnhetKode(behandling.getBehandlendeEnhet())
            .behandlingId(behandling.getId())
            .behandlingStegId(behandlingStegTilstand.map(BehandlingStegTilstand::getId).orElse(null))
            .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(aksjonspunkt))
            .funksjonellTid(LocalDateTime.now())
            .toTrinnsBehandling(aksjonspunkt.isToTrinnsBehandling())
            .toTrinnsBehandlingGodkjent(aksjonspunktGodkjennt)
            .fristTid(aksjonspunkt.getFristTid())
            .venteårsak(Venteårsak.UDEFINERT.equals(aksjonspunkt.getVenteårsak()) ? null : aksjonspunkt.getVenteårsak().getKode())
            .build();
    }

}
