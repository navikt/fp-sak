package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.vedtak.util.Tuple;

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

    public Optional<TransisjonIdentifikator> finnFremoverTransisjon() {
        return oppdatereResultater.stream()
            .filter(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.FREMOVERHOPP))
            .map(OppdateringResultat::getTransisjon)
            .findFirst(); // TODO (essv): Sorter steg ut fra deres rekkefølge
    }

    public Optional<OppdateringResultat> finnHenleggelse() {
        return oppdatereResultater.stream()
            .filter(delresultat -> delresultat.getOverhoppKontroll().equals(OverhoppKontroll.HENLEGGELSE))
            .findFirst();
    }

    public Set<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> finnEkstraAksjonspunktResultat() {
        Set<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> resultater = new HashSet<>();
        oppdatereResultater.stream().forEach(res -> resultater.addAll(res.getEkstraAksjonspunktResultat()));
        return resultater;
    }

    @Override
    public String toString() {
        return "OverhoppResultat{" +
            "oppdatereResultater=" + oppdatereResultater +
            '}';
    }
}
