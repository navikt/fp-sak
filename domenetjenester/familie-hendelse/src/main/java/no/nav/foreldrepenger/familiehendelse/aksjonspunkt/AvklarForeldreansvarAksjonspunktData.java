package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class AvklarForeldreansvarAksjonspunktData {
    private LocalDate omsorgsovertakelseDato;
    private LocalDate foreldreansvarDato;
    private Integer antallBarn;
    private List<AvklartDataBarnAdapter> barn;
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private Map<Integer, LocalDate> fødselsdatoer;

    public AvklarForeldreansvarAksjonspunktData(AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDate omsorgsovertakelseDato, LocalDate foreldreansvarDato,
                                                Integer antallBarn, List<AvklartDataBarnAdapter> barn, Map<Integer, LocalDate> fødselsdatoer) {
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
        this.foreldreansvarDato = foreldreansvarDato;
        this.antallBarn = antallBarn;
        this.barn = barn;
        this.fødselsdatoer = fødselsdatoer;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public LocalDate getForeldreansvarDato() {return foreldreansvarDato;}

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public List<AvklartDataBarnAdapter> getBarn() {
        return barn;
    }

    public Map<Integer, LocalDate> getFødselsdatoer() {
        return fødselsdatoer;
    }
}
