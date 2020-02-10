package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;

public class Avstemming115Dto extends SporingDto {

    private String kodekomponent;
    private String nokkelAvstemming;
    private String tidspnktMelding;

    public Avstemming115Dto(Avstemming115 entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

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

    public static Avstemming115Dto fraDomene(Avstemming115 avstemming115) {
        Avstemming115Dto avstemming115Dto = new Avstemming115Dto(avstemming115);
        avstemming115Dto.kodekomponent = avstemming115.getKodekomponent();
        avstemming115Dto.nokkelAvstemming = avstemming115.getNokkelAvstemming();
        avstemming115Dto.tidspnktMelding = avstemming115.getTidspnktMelding();
        return avstemming115Dto;
    }
}
