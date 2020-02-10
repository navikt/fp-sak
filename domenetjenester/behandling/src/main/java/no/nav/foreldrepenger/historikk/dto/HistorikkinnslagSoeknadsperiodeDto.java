package no.nav.foreldrepenger.historikk.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;

public class HistorikkinnslagSoeknadsperiodeDto {

    private HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType;
    private String navnVerdi;
    private String tilVerdi;

    public HistorikkinnslagSoeknadsperiodeDto() {
    }

    public HistorikkAvklartSoeknadsperiodeType getSoeknadsperiodeType() {
        return soeknadsperiodeType;
    }

    public void setSoeknadsperiodeType(HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType) {
        this.soeknadsperiodeType = soeknadsperiodeType;
    }

    public String getTilVerdi() {
        return tilVerdi;
    }

    public void setTilVerdi(String tilVerdi) {
        this.tilVerdi = tilVerdi;
    }

    public String getNavnVerdi() {
        return navnVerdi;
    }

    public void setNavnVerdi(String navnVerdi) {
        this.navnVerdi = navnVerdi;
    }

    static HistorikkinnslagSoeknadsperiodeDto mapFra(HistorikkinnslagFelt soeknadsperiode) {
        HistorikkinnslagSoeknadsperiodeDto dto = new HistorikkinnslagSoeknadsperiodeDto();
        HistorikkAvklartSoeknadsperiodeType soeknadsperiodeType = HistorikkAvklartSoeknadsperiodeType.fraKode(soeknadsperiode.getNavn());
        dto.setSoeknadsperiodeType(soeknadsperiodeType);
        dto.setNavnVerdi(soeknadsperiode.getNavnVerdi());
        dto.setTilVerdi(soeknadsperiode.getTilVerdi());
        return dto;
    }


}
