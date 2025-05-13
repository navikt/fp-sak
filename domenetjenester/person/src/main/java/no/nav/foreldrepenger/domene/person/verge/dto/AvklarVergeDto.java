package no.nav.foreldrepenger.domene.person.verge.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_VERGE_KODE)
public class AvklarVergeDto extends BekreftetAksjonspunktDto {

    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String navn;
    @Pattern(regexp = "^\\d{11}$")
    private String fnr;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    @NotNull
    @ValidKodeverk
    private VergeType vergeType;

    @Pattern(regexp = "^\\d{9}$")
    private String organisasjonsnummer;


    public void setNavn(String navn) {
        this.navn = navn;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public void setGyldigFom(LocalDate gyldigFom) {
        this.gyldigFom = gyldigFom;
    }

    public void setGyldigTom(LocalDate gyldigTom) {
        this.gyldigTom = gyldigTom;
    }

    public void setVergeType(VergeType vergeType) {
        this.vergeType = vergeType;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getNavn() {
        return navn;
    }

    public String getFnr() {
        return fnr;
    }

    public LocalDate getGyldigFom() {
        return gyldigFom;
    }

    public LocalDate getGyldigTom() {
        return gyldigTom;
    }

    public VergeType getVergeType() {
        return vergeType;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    @Override
    public String toString() {
        return "VergeDto{" + "vergeType=" + vergeType + ", gyldigFom=" + gyldigFom + ", gyldigTom=" + gyldigTom + '}';
    }


}
