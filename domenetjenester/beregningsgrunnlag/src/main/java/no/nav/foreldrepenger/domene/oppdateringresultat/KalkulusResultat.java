package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class KalkulusResultat {

    private List<BeregningAvklaringsbehovResultat> beregningAvklaringsbehovResultat;
    private Boolean vilkårOppfylt;
    private Avslagsårsak avslagsårsak;
    private Map<DatoIntervallEntitet, BeregningVilkårResultat> vilkårResultatPrPeriode = new HashMap<>();

    public KalkulusResultat(List<BeregningAvklaringsbehovResultat> beregningAvklaringsbehovResultat) {
        this.beregningAvklaringsbehovResultat = beregningAvklaringsbehovResultat;
    }

    public KalkulusResultat medVilkårResultat(boolean vilkårOppfylt) {
        this.vilkårOppfylt = vilkårOppfylt;
        return this;
    }

    public KalkulusResultat medAvslåttVilkår(Avslagsårsak avslagsårsak) {
        this.vilkårOppfylt = false;
        this.avslagsårsak = avslagsårsak;
        return this;
    }

    public KalkulusResultat leggTilVilkårResultat(DatoIntervallEntitet periode, boolean vilkårOppfylt, Avslagsårsak avslagsårsak) {
        this.vilkårResultatPrPeriode.put(periode, new BeregningVilkårResultat(vilkårOppfylt, avslagsårsak));
        return this;
    }

    public List<BeregningAvklaringsbehovResultat> getBeregningAksjonspunktResultat() {
        return beregningAvklaringsbehovResultat;
    }

    public Map<DatoIntervallEntitet, BeregningVilkårResultat> getVilkårResultatPrPeriode() {
        return vilkårResultatPrPeriode;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<vilkårOppfylt=" + vilkårOppfylt +
            ", avslagsårsak=" + avslagsårsak +
            ", beregningAksjonspunktResultat=" + beregningAvklaringsbehovResultat +
            ">";
    }

}
