package no.nav.foreldrepenger.historikk.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;

import java.util.ArrayList;
import java.util.List;

public class HistorikkinnslagEndretFeltDto {

    private HistorikkEndretFeltType endretFeltNavn;
    private String navnVerdi;
    private String klNavn;
    private Object fraVerdi;
    private Object tilVerdi;
    private String klFraVerdi;
    private String klTilVerdi;

    public HistorikkEndretFeltType getEndretFeltNavn() {
        return endretFeltNavn;
    }

    public void setEndretFeltNavn(HistorikkEndretFeltType endretFeltNavn) {
        this.endretFeltNavn = endretFeltNavn;
    }

    public String getNavnVerdi() {
        return navnVerdi;
    }

    public void setNavnVerdi(String navnVerdi) {
        this.navnVerdi = navnVerdi;
    }

    public String getKlNavn() {
        return klNavn;
    }

    public void setKlNavn(String klNavn) {
        this.klNavn = klNavn;
    }

    public Object getFraVerdi() {
        return fraVerdi;
    }

    public void setFraVerdi(Object fraVerdi) {
        this.fraVerdi = fraVerdi;
    }

    public Object getTilVerdi() {
        return tilVerdi;
    }

    public String getKlFraVerdi() {
        return klFraVerdi;
    }

    public void setKlFraVerdi(String klFraVerdi) {
        this.klFraVerdi = klFraVerdi;
    }

    public String getKlTilVerdi() {
        return klTilVerdi;
    }

    public void setKlTilVerdi(String klTilVerdi) {
        this.klTilVerdi = klTilVerdi;
    }

    public void setTilVerdi(Object tilVerdi) {
        this.tilVerdi = tilVerdi;
    }

    static List<HistorikkinnslagEndretFeltDto> mapFra(List<HistorikkinnslagFelt> endretFeltList) {
        List<HistorikkinnslagEndretFeltDto> dto = new ArrayList<>();
        for (var felt : endretFeltList) {
            dto.add(mapFra(felt));
        }
        return dto;
    }

    private static HistorikkinnslagEndretFeltDto mapFra(HistorikkinnslagFelt endretFelt) {
        var dto = new HistorikkinnslagEndretFeltDto();
        var endretFeltNavn = HistorikkEndretFeltType.fraKode(endretFelt.getNavn());
        dto.setEndretFeltNavn(endretFeltNavn);
        dto.setNavnVerdi(endretFelt.getNavnVerdi());
        dto.setKlNavn(endretFelt.getKlNavn());
        dto.setFraVerdi(tilObject(endretFelt.getFraVerdi()));
        dto.setTilVerdi(tilObject(endretFelt.getTilVerdi()));
        dto.setKlFraVerdi(endretFelt.getKlFraVerdi());
        dto.setKlTilVerdi(endretFelt.getKlTilVerdi());
        return dto;
    }

    private static Object tilObject(String verdi) {
        if ("true".equals(verdi)) {
            return Boolean.TRUE;
        }
        if ("false".equals(verdi)) {
            return Boolean.FALSE;
        }
        return verdi;
    }
}
