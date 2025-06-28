package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


public class AvklartDataFodselDto extends FamiliehendelseDto {
    private List<AvklartBarnDto> avklartBarn;
    private Boolean brukAntallBarnFraTps; //TPS = PDL
    private Boolean erOverstyrt;
    private LocalDate termindato;
    private Integer antallBarnTermin;
    private LocalDate utstedtdato;
    private Boolean morForSykVedFodsel;
    private Long vedtaksDatoSomSvangerskapsuke;

    AvklartDataFodselDto() {
        super(SøknadType.FØDSEL);
    }

    void setAvklartBarn(List<AvklartBarnDto> avklartBarn) {
        this.avklartBarn = avklartBarn;
    }

    void setBrukAntallBarnFraTps(Boolean brukAntallBarnFraTps) {
        this.brukAntallBarnFraTps = brukAntallBarnFraTps;
    }

    void setDokumentasjonForligger(Boolean erOverstyrt) {
        this.erOverstyrt = erOverstyrt;
    }

    void setTermindato(LocalDate termindato) {
        this.termindato = termindato;
    }

    public void setMorForSykVedFodsel(Boolean morForSykVedFodsel) {
        this.morForSykVedFodsel = morForSykVedFodsel;
    }

    //TODO(OJR) burde fjerne enten denne eller setAntallBarnFødt

    void setAntallBarnTermin(Integer antallBarnTermin) {
        this.antallBarnTermin = antallBarnTermin;
    }

    void setUtstedtdato(LocalDate utstedtdato) {
        this.utstedtdato = utstedtdato;
    }

    @JsonProperty("avklartBarn")
    public List<AvklartBarnDto> getAvklartBarn() {
        return avklartBarn;
    }

    @JsonProperty("brukAntallBarnFraTps")
    public Boolean getBrukAntallBarnFraTps() {
        return brukAntallBarnFraTps;
    }

    @JsonProperty("termindato")
    public LocalDate getTermindato() {
        return termindato;
    }

    @JsonProperty("antallBarnTermin")
    public Integer getAntallBarnTermin() {
        return antallBarnTermin;
    }

    @JsonProperty("utstedtdato")
    public LocalDate getUtstedtdato() {
        return utstedtdato;
    }

    public Boolean getDokumentasjonForeligger() {
        return erOverstyrt;
    }

    @JsonProperty("morForSykVedFodsel")
    public Boolean getMorForSykVedFodsel() {
        return morForSykVedFodsel;
    }

    @JsonProperty("vedtaksDatoSomSvangerskapsuke")
    public Long getVedtaksDatoSomSvangerskapsuke() {
        return vedtaksDatoSomSvangerskapsuke;
    }

    void setVedtaksDatoSomSvangerskapsuke(Long vedtaksDatoSomSvangerskapsuke) {
        this.vedtaksDatoSomSvangerskapsuke = vedtaksDatoSomSvangerskapsuke;
    }
}
