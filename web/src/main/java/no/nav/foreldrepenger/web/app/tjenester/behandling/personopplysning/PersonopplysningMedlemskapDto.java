package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;

public class PersonopplysningMedlemskapDto {

    private final String identifikator;

    private Landkoder statsborgerskap;
    private Region region;

    private AvklartPersonstatus avklartPersonstatus;
    private PersonstatusType personstatus;

    public PersonopplysningMedlemskapDto(String identifikator) {
        this.identifikator = identifikator;
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public Landkoder getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(Landkoder statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
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

}
