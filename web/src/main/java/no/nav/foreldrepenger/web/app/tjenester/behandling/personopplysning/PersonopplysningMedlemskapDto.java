package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;

public class PersonopplysningMedlemskapDto {

    private String aktoerId;
    private String navn;
    private LocalDate dodsdato;
    private LocalDate fodselsdato;

    private RelasjonsRolleType relasjonsRolle;
    private SivilstandType sivilstand;

    private LandkoderDto statsborgerskap;
    private AvklartPersonstatus avklartPersonstatus;
    private PersonstatusType personstatus;
    private List<PersonadresseDto> adresser = new ArrayList<>();

    private Region region;
    private PersonopplysningMedlemskapDto annenPart;

    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
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

    public RelasjonsRolleType getRelasjonsRolle() {
        return relasjonsRolle;
    }

    public void setRelasjonsRolle(RelasjonsRolleType relasjonsRolle) {
        this.relasjonsRolle = relasjonsRolle;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
    }

    public LandkoderDto getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(LandkoderDto statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public AvklartPersonstatus getAvklartPersonstatus() {
        return avklartPersonstatus;
    }

    public void setAvklartPersonstatus(AvklartPersonstatus avklartPersonstatus) {
        this.avklartPersonstatus = avklartPersonstatus;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public void setPersonstatus(PersonstatusType personstatus) {
        this.personstatus = personstatus;
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public PersonopplysningMedlemskapDto getAnnenPart() {
        return annenPart;
    }

    public void setAnnenPart(PersonopplysningMedlemskapDto annenPart) {
        this.annenPart = annenPart;
    }
}
