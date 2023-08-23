package no.nav.foreldrepenger.familiehendelse.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;

import java.time.LocalDate;
import java.util.Map;

public class AvklartDataOmsorgDto extends FamiliehendelseDto {

    private LocalDate omsorgsovertakelseDato;

    @Valid
    private OmsorgsovertakelseVilkårType vilkarType;

    private Integer antallBarnTilBeregning;
    private LocalDate foreldreansvarDato;

    @Valid
    @Size(max = 9)
    private Map<Integer, LocalDate> fødselsdatoer;

    public AvklartDataOmsorgDto() {
        // trengs for deserialisering av JSON
        super();
    }

    public AvklartDataOmsorgDto(SøknadType søknadType) {
        super(søknadType);
    }

    public LocalDate getForeldreansvarDato() {
        return foreldreansvarDato;
    }

    public void setForeldreansvarDato(LocalDate foreldreansvarDato) {
        this.foreldreansvarDato = foreldreansvarDato;
    }

    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public OmsorgsovertakelseVilkårType getVilkarType() {
        return vilkarType;
    }

    void setVilkarType(OmsorgsovertakelseVilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public Integer getAntallBarnTilBeregning() {
        return antallBarnTilBeregning;
    }

    void setAntallBarnTilBeregning(Integer antallBarnTilBeregning) {
        this.antallBarnTilBeregning = antallBarnTilBeregning;
    }

    public Map<Integer, LocalDate> getFødselsdatoer() {
        return fødselsdatoer;
    }

    public void setFødselsdatoer(Map<Integer, LocalDate> fødselsdatoer) {
        this.fødselsdatoer = fødselsdatoer;
    }
}
