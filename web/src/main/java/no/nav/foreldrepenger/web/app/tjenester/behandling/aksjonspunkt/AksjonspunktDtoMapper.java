package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public class AksjonspunktDtoMapper {

    private AksjonspunktDtoMapper() {
    }

    public static Optional<AksjonspunktDto> lagAksjonspunktDtoFor(Behandling behandling, Behandlingsresultat behandlingsresultat, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return behandling.getAksjonspunkter().stream()
            .filter(a -> aksjonspunktDefinisjon.equals(a.getAksjonspunktDefinisjon()))
            .filter(aksjonspunkt -> !aksjonspunkt.erAvbrutt())
            .map(aksjonspunkt -> mapFra(aksjonspunkt, behandling, behandlingsresultat, List.of()))
            .findFirst();
    }

    public static Set<AksjonspunktDto> lagAksjonspunktDto(Behandling behandling, Behandlingsresultat behandlingsresultat, Collection<Totrinnsvurdering> ttVurderinger) {
        return behandling.getAksjonspunkter().stream()
                .filter(aksjonspunkt -> !aksjonspunkt.erAvbrutt())
                .map(aksjonspunkt -> mapFra(aksjonspunkt, behandling, behandlingsresultat, ttVurderinger))
                .collect(Collectors.toSet());
    }

    private static AksjonspunktDto mapFra(Aksjonspunkt aksjonspunkt, Behandling behandling, Behandlingsresultat behandlingsresultat, Collection<Totrinnsvurdering> ttVurderinger) {
        var aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();

        var dto = new AksjonspunktDto();
        dto.setDefinisjon(aksjonspunktDefinisjon);
        dto.setStatus(aksjonspunkt.getStatus());
        dto.setBegrunnelse(aksjonspunkt.getBegrunnelse());
        dto.setVilkarType(finnVilkårType(aksjonspunkt, behandlingsresultat));
        dto.setToTrinnsBehandling(aksjonspunkt.isToTrinnsBehandling());
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

    // AKsjonspunkt 5031 er ikke knyttet til et bestemt vilkår da de skal ha 5 forskjellige.
    //TODO(OJR) modellen burde utvides til å støtte dette...
    private static VilkårType finnVilkårType(Aksjonspunkt aksjonspunkt, Behandlingsresultat behandlingsresultat) {
        var aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();
        if (AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE.equals(aksjonspunktDefinisjon)) {
            return Optional.ofNullable(behandlingsresultat)
                .map(Behandlingsresultat::getVilkårResultat)
                .flatMap(VilkårResultat::getVilkårForRelasjonTilBarn).orElse(null);
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
