package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;

public class AvstemmingDto {

    private String kodekomponent;
    private String nokkelAvstemming;
    private String tidspnktMelding;

    public AvstemmingDto() {}

    public String getKodekomponent() {
        return kodekomponent;
    }

    public void setKodekomponent(String kodekomponent) {
        this.kodekomponent = kodekomponent;
    }

    public String getNokkelAvstemming() {
        return nokkelAvstemming;
    }

    public void setNokkelAvstemming(String nokkelAvstemming) {
        this.nokkelAvstemming = nokkelAvstemming;
    }

    public String getTidspnktMelding() {
        return tidspnktMelding;
    }

    public void setTidspnktMelding(String tidspnktMelding) {
        this.tidspnktMelding = tidspnktMelding;
    }

    public static AvstemmingDto fraDomene(Avstemming avstemming) {
        AvstemmingDto avstemmingDto = new AvstemmingDto();
        avstemmingDto.kodekomponent = avstemming.getKodekomponent();
        avstemmingDto.nokkelAvstemming = avstemming.getNøkkel();
        avstemmingDto.tidspnktMelding = avstemming.getTidspunkt();
        return avstemmingDto;
    }
}
