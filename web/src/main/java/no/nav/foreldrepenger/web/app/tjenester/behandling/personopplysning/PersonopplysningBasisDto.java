package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;

public class PersonopplysningBasisDto extends PersonIdentDto {

    private final String identifikator;
    private NavBrukerKjønn navBrukerKjonn;
    private SivilstandType sivilstand;
    private String navn;
    private LocalDate dodsdato;
    private LocalDate fodselsdato;
    private List<PersonadresseDto> adresser = new ArrayList<>();

    public PersonopplysningBasisDto(String identifikator) {
        this.identifikator = identifikator;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public NavBrukerKjønn getNavBrukerKjonn() {
        return navBrukerKjonn;
    }

    public void setNavBrukerKjonn(NavBrukerKjønn navBrukerKjonn) {
        this.navBrukerKjonn = navBrukerKjonn;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public LocalDate getDodsdato() {
        return dodsdato;
    }

    public void setDodsdato(LocalDate dodsdato) {
        this.dodsdato = dodsdato;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public void setFodselsdato(LocalDate fodselsdato) {
        this.fodselsdato = fodselsdato;
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }
}
