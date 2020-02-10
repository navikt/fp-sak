package no.nav.foreldrepenger.historikk.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;

public class HistorikkinnslagHendelseDto {
    private HistorikkinnslagType navn;
    private String verdi;

    public HistorikkinnslagType getNavn() {
        return navn;
    }

    public void setNavn(HistorikkinnslagType navn) {
        this.navn = navn;
    }

    public String getVerdi() {
        return verdi;
    }

    public void setVerdi(String verdi) {
        this.verdi = verdi;
    }

    public static HistorikkinnslagHendelseDto mapFra(HistorikkinnslagFelt hendelse) {
        HistorikkinnslagHendelseDto dto = new HistorikkinnslagHendelseDto();
        dto.navn = HistorikkinnslagType.fraKode(hendelse.getNavn());
        dto.verdi = hendelse.getTilVerdi();
        return dto;
    }
}
