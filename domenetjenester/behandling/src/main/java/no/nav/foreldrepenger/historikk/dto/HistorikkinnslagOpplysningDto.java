package no.nav.foreldrepenger.historikk.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;

public class HistorikkinnslagOpplysningDto {

    private HistorikkOpplysningType opplysningType;
    private String tilVerdi;

    public HistorikkinnslagOpplysningDto() {
    }

    public HistorikkOpplysningType getOpplysningType() {
        return opplysningType;
    }

    public void setOpplysningType(HistorikkOpplysningType opplysningType) {
        this.opplysningType = opplysningType;
    }

    public String getTilVerdi() {
        return tilVerdi;
    }

    public void setTilVerdi(String tilVerdi) {
        this.tilVerdi = tilVerdi;
    }

    static List<HistorikkinnslagOpplysningDto> mapFra(List<HistorikkinnslagFelt> opplysninger) {
        return opplysninger.stream().map(o -> mapFra(o)).toList();
    }

    private static HistorikkinnslagOpplysningDto mapFra(HistorikkinnslagFelt opplysning) {
        var dto = new HistorikkinnslagOpplysningDto();
        var opplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());
        dto.setOpplysningType(opplysningType);
        dto.setTilVerdi(opplysning.getTilVerdi());
        return dto;
    }
}
