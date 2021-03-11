package no.nav.foreldrepenger.domene.ytelsefordeling;


import java.util.List;

import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class BekreftFaktaForOmsorgVurderingAksjonspunktDto {

    private final Boolean aleneomsorg;
    private final Boolean omsorg;
    private final List<DatoIntervallEntitet> ikkeOmsorgPerioder;

    public BekreftFaktaForOmsorgVurderingAksjonspunktDto(Boolean aleneomsorg,
                                                         Boolean omsorg,
                                                         List<DatoIntervallEntitet> ikkeOmsorgPerioder) {
        this.aleneomsorg = aleneomsorg;
        this.omsorg = omsorg;
        this.ikkeOmsorgPerioder = ikkeOmsorgPerioder;
    }

    public Boolean getAleneomsorg() {
        return aleneomsorg;
    }

    public Boolean getOmsorg() {
        return omsorg;
    }

    public List<DatoIntervallEntitet> getIkkeOmsorgPerioder() {
        return ikkeOmsorgPerioder;
    }

}
