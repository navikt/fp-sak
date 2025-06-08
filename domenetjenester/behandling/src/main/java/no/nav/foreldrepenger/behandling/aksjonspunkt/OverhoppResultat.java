package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OverhoppResultat {
    Set<OppdateringResultat> oppdatereResultater = new LinkedHashSet<>();

    public static OverhoppResultat tomtResultat() {
        return new OverhoppResultat();
    }

    public void leggTil(OppdateringResultat delresultat) {
        oppdatereResultater.add(delresultat);
    }

    public boolean skalOppdatereGrunnlag() {
        return oppdatereResultater.stream().anyMatch(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.OPPDATER));
    }

    public boolean finnTotrinn() {
        return oppdatereResultater.stream().anyMatch(OppdateringResultat::kreverTotrinnsKontroll);
    }

    public Optional<AksjonspunktOppdateringTransisjon> finnFremoverTransisjon() {
        return oppdatereResultater.stream()
                .filter(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.FREMOVERHOPP))
                .map(OppdateringResultat::getTransisjon)
                .findFirst();
    }

    public Set<OppdateringAksjonspunktResultat> finnEkstraAksjonspunktResultat() {
        return oppdatereResultater.stream()
            .map(OppdateringResultat::getEkstraAksjonspunktResultat)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "OverhoppResultat{" +
                "oppdatereResultater=" + oppdatereResultater +
                '}';
    }
}
