package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;

public class AvklarForeldreansvarAksjonspunktData {
    private LocalDate omsorgsovertakelseDato;
    private LocalDate foreldreansvarDato;

    public AvklarForeldreansvarAksjonspunktData(LocalDate omsorgsovertakelseDato, LocalDate foreldreansvarDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
        this.foreldreansvarDato = foreldreansvarDato;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public LocalDate getForeldreansvarDato() {return foreldreansvarDato;}

}
