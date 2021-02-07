package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersonoversiktDto {

    private final Map<String, PersonopplysningBasisDto> personer = new HashMap<>();

    private String brukerId;
    private String annenpartId;
    private String ektefelleId;
    private List<String> barnMedId;
    private List<String> barnUtenId;
    private boolean brukerHarVerge;

    public PersonoversiktDto() {
    }

    public Map<String, PersonopplysningBasisDto> getPersoner() {
        return personer;
    }

    public void leggTilPerson(String identifikator, PersonopplysningBasisDto dto) {
        personer.put(identifikator, dto);
    }

    public String getBrukerId() {
        return brukerId;
    }

    public void setBrukerId(String brukerId) {
        this.brukerId = brukerId;
    }

    public String getAnnenpartId() {
        return annenpartId;
    }

    public void setAnnenpartId(String annenpartId) {
        this.annenpartId = annenpartId;
    }

    public String getEktefelleId() {
        return ektefelleId;
    }

    public void setEktefelleId(String ektefelleId) {
        this.ektefelleId = ektefelleId;
    }

    public List<String> getBarnMedId() {
        return barnMedId;
    }

    public void setBarnMedId(List<String> barnMedId) {
        this.barnMedId = barnMedId;
    }

    public List<String> getBarnUtenId() {
        return barnUtenId;
    }

    public void setBarnUtenId(List<String> barnUtenId) {
        this.barnUtenId = barnUtenId;
    }

    public boolean isBrukerHarVerge() {
        return brukerHarVerge;
    }

    public void setBrukerHarVerge(boolean brukerHarVerge) {
        this.brukerHarVerge = brukerHarVerge;
    }
}
