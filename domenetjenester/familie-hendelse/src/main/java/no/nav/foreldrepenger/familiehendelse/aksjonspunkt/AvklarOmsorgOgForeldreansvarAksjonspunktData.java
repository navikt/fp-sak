package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

public class AvklarOmsorgOgForeldreansvarAksjonspunktData {
    private String vilkarTypeKode;
    private LocalDate omsorgsovertakelseDato;
    private Integer antallBarn;
    private List<AvklartDataBarnAdapter> barn;
    private AksjonspunktDefinisjon aksjonspunktDefinisjon;
    private Map<Integer, LocalDate> fødselsdatoer;

    public AvklarOmsorgOgForeldreansvarAksjonspunktData(String vilkarTypeKode, AksjonspunktDefinisjon aksjonspunktDefinisjon, LocalDate omsorgsovertakelseDato,
                                                        Integer antallBarn, List<AvklartDataBarnAdapter> barn, Map<Integer, LocalDate> fødselsdatoer) {
        this.vilkarTypeKode = vilkarTypeKode;
        this.aksjonspunktDefinisjon = aksjonspunktDefinisjon;
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
        this.antallBarn = antallBarn;
        this.barn = barn;
        this.fødselsdatoer = fødselsdatoer;
    }

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return aksjonspunktDefinisjon;
    }

    public String getVilkarTypeKode() {
        return vilkarTypeKode;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

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
