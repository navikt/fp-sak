package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;

class AksjonspunktDtoMapper {

    private AksjonspunktDtoMapper() {
    }

    static Set<AksjonspunktDto> lagAksjonspunktDto(Behandling behandling, Collection<Totrinnsvurdering> ttVurderinger) {
        return behandling.getAksjonspunkter().stream()
                .filter(aksjonspunkt -> !aksjonspunkt.erAvbrutt())
                .map(aksjonspunkt -> mapFra(aksjonspunkt, behandling, ttVurderinger))
                .collect(Collectors.toSet());
    }

    private static AksjonspunktDto mapFra(Aksjonspunkt aksjonspunkt, Behandling behandling, Collection<Totrinnsvurdering> ttVurderinger) {
        var aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();

        var dto = new AksjonspunktDto();
        dto.setDefinisjon(aksjonspunktDefinisjon);
        dto.setStatus(aksjonspunkt.getStatus());
        dto.setBegrunnelse(aksjonspunkt.getBegrunnelse());
        dto.setVilkarType(finnVilkårType(aksjonspunkt, behandling));
        dto.setToTrinnsBehandling(aksjonspunkt.isToTrinnsBehandling() || aksjonspunktDefinisjon.getDefaultTotrinnBehandling());
        dto.setFristTid(aksjonspunkt.getFristTid());
        dto.setEndretAv(aksjonspunkt.getEndretAv());
        dto.setEndretTidspunkt(aksjonspunkt.getEndretTidspunkt());

        var vurdering = ttVurderinger.stream().filter(v -> v.getAksjonspunktDefinisjon() == aksjonspunkt.getAksjonspunktDefinisjon()).findFirst();
        vurdering.ifPresent(ttVurdering -> {
            dto.setBesluttersBegrunnelse(ttVurdering.getBegrunnelse());
            dto.setToTrinnsBehandlingGodkjent(ttVurdering.isGodkjent());
            dto.setVurderPaNyttArsaker(ttVurdering.getVurderPåNyttÅrsaker().stream()
                .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType).collect(Collectors.toSet()));
            }
        );

        dto.setAksjonspunktType(aksjonspunktDefinisjon.getAksjonspunktType());
        dto.setKanLoses(kanLøses(aksjonspunktDefinisjon, behandling, aksjonspunkt.getStatus()));
        dto.setErAktivt(Boolean.TRUE);
        return dto;
    }

    // AKsjonspunkt 5031 og 5032 er ikke knyttet til et bestemt vilkår da de skal ha 5 forskjellige.
    //TODO(OJR) modellen burde utvides til å støtte dette...
    private static VilkårType finnVilkårType(Aksjonspunkt aksjonspunkt, Behandling behandling) {
        var aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();
        if (AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE.equals(aksjonspunktDefinisjon) ||
                AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE.equals(aksjonspunktDefinisjon)) {
            return behandling.getVilkårTypeForRelasjonTilBarnet().orElse(null);
        }
        return aksjonspunktDefinisjon.getVilkårType();
    }

    private static Boolean kanLøses(AksjonspunktDefinisjon def, Behandling behandling, AksjonspunktStatus status) {
        if (behandling.getBehandlingStegStatus() == null) {
            // Stegstatus ikke satt, kan derfor ikke sette noen aksjonspunkt som løsbart
            return false;
        }
        //Spesialbehandling av opprettelse av verge fordi det skal kunne gjelde alle behandlingstyper
        if (AksjonspunktDefinisjon.AVKLAR_VERGE.equals(def) && !AksjonspunktStatus.UTFØRT.equals(status)) {
            return true;
        }
        if (def.erAutopunkt()) {
            return false;
        }
        var aktivtBehandlingSteg = Optional.ofNullable(behandling.getAktivtBehandlingSteg());
        return aktivtBehandlingSteg.map(steg ->
                skalLøsesIStegKode(def, behandling.getBehandlingStegStatus().getKode(), steg))
                .orElse(false);
    }

    private static Boolean skalLøsesIStegKode(AksjonspunktDefinisjon def, String stegKode, BehandlingStegType steg) {
        if (BehandlingStegStatus.INNGANG.getKode().equals(stegKode)) {
            return steg.getAksjonspunktDefinisjonerInngang().contains(def);
        }
        return BehandlingStegStatus.UTGANG.getKode().equals(stegKode) && steg.getAksjonspunktDefinisjonerUtgang().contains(def);
    }
}
