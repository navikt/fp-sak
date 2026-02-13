package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningBasisDto {

    @NotNull private NavBrukerKjønn kjønn;
    @NotNull private SivilstandType sivilstand;
    private LocalDate dødsdato;
    @NotNull private LocalDate fødselsdato;
    @NotNull private List<PersonadresseDto> adresser = new ArrayList<>();
    private String fnr;
    private String aktoerId;
    private AktørId aktørId;
    private Diskresjonskode diskresjonskode;
    private String navn;

    public PersonopplysningBasisDto(AktørId aktørId) {
        this.setAktoerId(aktørId);
        this.setAktørId(aktørId);
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public void setKjønn(NavBrukerKjønn kjønn) {
        this.kjønn = kjønn;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public AktørId getAktoerId() {
        return aktoerId == null ? null : new AktørId(aktoerId);
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    void setAktoerId(AktørId aktoerId) {
        if (aktoerId != null) {
            this.aktoerId = aktoerId.getId();
        }
    }

    public String getFnr() {
        return fnr;
    }

    void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }
}
