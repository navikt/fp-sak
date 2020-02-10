package no.nav.foreldrepenger.domene.medlem.api;

import java.util.List;

public class AvklarFortsattMedlemskapAksjonspunktDto {
    private List<BekreftedePerioderAdapter> perioder;


    public AvklarFortsattMedlemskapAksjonspunktDto(List<BekreftedePerioderAdapter> perioder) {

        this.perioder = perioder;
    }

    public List<BekreftedePerioderAdapter> getPerioder() {
        return perioder;
    }
}
