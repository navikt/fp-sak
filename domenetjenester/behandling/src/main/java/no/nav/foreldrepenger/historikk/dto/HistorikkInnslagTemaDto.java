package no.nav.foreldrepenger.historikk.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;

class HistorikkInnslagTemaDto {
    private HistorikkEndretFeltType endretFeltNavn;
    private String navnVerdi;
    private String klNavn;

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

    static HistorikkInnslagTemaDto mapFra(HistorikkinnslagFelt felt) {
        var dto = new HistorikkInnslagTemaDto();
        var endretFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());
        dto.setEndretFeltNavn(endretFeltNavn);
        dto.setNavnVerdi(felt.getNavnVerdi());
        dto.setKlNavn(felt.getKlNavn());
        return dto;
    }

}
