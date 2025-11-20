package no.nav.foreldrepenger.web.app.tjenester.registrering.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;

import no.nav.vedtak.util.InputValideringRegex;

public class AnnenForelderDto {
    @Size(max = 11, min = 11)
    @Digits(integer = 11, fraction = 0)
    private String fødselsnummer;
    private Boolean kanIkkeOppgiAnnenForelder;
    @Valid
    private KanIkkeOppgiBegrunnelse kanIkkeOppgiBegrunnelse;

    private boolean søkerHarAleneomssorg;

    private boolean denAndreForelderenHarRettPåForeldrepenger;

    private Boolean morMottarUføretrygd;
    private Boolean annenForelderRettEØS;

    @JsonAlias("foedselsnummer")
    public String getFødselsnummer() {
        return fødselsnummer;
    }

    public void setFødselsnummer(String fødselsnummer) {
        this.fødselsnummer = fødselsnummer;
    }

    public Boolean getKanIkkeOppgiAnnenForelder() {
        return kanIkkeOppgiAnnenForelder;
    }

    public void setKanIkkeOppgiAnnenForelder(Boolean kanIkkeOppgiAnnenForelder) {
        this.kanIkkeOppgiAnnenForelder = kanIkkeOppgiAnnenForelder;
    }

    public KanIkkeOppgiBegrunnelse getKanIkkeOppgiBegrunnelse() {
        return kanIkkeOppgiBegrunnelse;
    }

    public void setKanIkkeOppgiBegrunnelse(KanIkkeOppgiBegrunnelse kanIkkeOppgiBegrunnelse) {
        this.kanIkkeOppgiBegrunnelse = kanIkkeOppgiBegrunnelse;
    }

    @JsonAlias("sokerHarAleneomsorg")
    public boolean getSøkerHarAleneomssorg() {
        return søkerHarAleneomssorg;
    }

    public void setSøkerHarAleneomssorg(Boolean søkerHarAleneomssorg) {
        this.søkerHarAleneomssorg = søkerHarAleneomssorg;
    }

    @JsonAlias("denAndreForelderenHarRettPaForeldrepenger")
    public boolean getDenAndreForelderenHarRettPåForeldrepenger() {
        return denAndreForelderenHarRettPåForeldrepenger;
    }

    public void setDenAndreForelderenHarRettPåForeldrepenger(Boolean denAndreForelderenHarRettPåForeldrepenger) {
        this.denAndreForelderenHarRettPåForeldrepenger = denAndreForelderenHarRettPåForeldrepenger;
    }

    public Boolean getMorMottarUføretrygd() {
        return morMottarUføretrygd;
    }

    public void setMorMottarUføretrygd(Boolean morMottarUføretrygd) {
        this.morMottarUføretrygd = morMottarUføretrygd;
    }

    public Boolean getAnnenForelderRettEØS() {
        return annenForelderRettEØS;
    }

    public void setAnnenForelderRettEØS(Boolean annenForelderRettEØS) {
        this.annenForelderRettEØS = annenForelderRettEØS;
    }

    public static class KanIkkeOppgiBegrunnelse {
        @NotNull
        @Size(min = 1, max = 100)
        @Pattern(regexp = InputValideringRegex.KODEVERK)
        private String årsak;
        @Size(max = 20)
        @Pattern(regexp = InputValideringRegex.FRITEKST)
        private String utenlandskFødselsnummer;
        @Size(max = 100)
        @Pattern(regexp = InputValideringRegex.NAVN)
        private String land;

        @JsonAlias("arsak")
        public String getÅrsak() {
            return årsak;
        }

        public void setÅrsak(String årsak) {
            this.årsak = årsak;
        }

        @JsonAlias("utenlandskFoedselsnummer")
        public String getUtenlandskFødselsnummer() {
            return utenlandskFødselsnummer;
        }

        public void setUtenlandskFødselsnummer(String utenlandskFødselsnummer) {
            this.utenlandskFødselsnummer = utenlandskFødselsnummer;
        }

        public String getLand() {
            return land;
        }

        public void setLand(String land) {
            this.land = land;
        }
    }
}
