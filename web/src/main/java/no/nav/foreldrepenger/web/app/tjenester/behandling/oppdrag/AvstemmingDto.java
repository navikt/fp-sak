package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;

public class AvstemmingDto {

    private String nokkelAvstemming;
    private String tidspnktMelding;

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
        var avstemmingDto = new AvstemmingDto();
        avstemmingDto.nokkelAvstemming = avstemming.getNøkkel();
        avstemmingDto.tidspnktMelding = avstemming.getTidspunkt();
        return avstemmingDto;
    }
}
