package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class Personinfo {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private PersonstatusType personstatus;
    private NavBrukerKjønn kjønn;
    private Set<FamilierelasjonVL> familierelasjoner = Collections.emptySet();
    private List<Landkoder> landkoder = List.of();

    private List<Adresseinfo> adresseInfoList = new ArrayList<>();
    private SivilstandType sivilstand;

    private Personinfo() {
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public String getNavn() {
        return navn;
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Set<FamilierelasjonVL> getFamilierelasjoner() {
        return Collections.unmodifiableSet(familierelasjoner);
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public List<Adresseinfo> getAdresseInfoList() {
        return adresseInfoList;
    }

    public SivilstandType getSivilstandType() {
        return sivilstand;
    }

    public List<Landkoder> getLandkoder() {
        return landkoder;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<aktørId=" + aktørId + ">";
    }

    public static class Builder {
        private Personinfo personinfoMal;

        public Builder() {
            personinfoMal = new Personinfo();
        }

        public Builder medAktørId(AktørId aktørId) {
            personinfoMal.aktørId = aktørId;
            return this;
        }

        public Builder medNavn(String navn) {
            personinfoMal.navn = navn;
            return this;
        }

        public Builder medPersonIdent(PersonIdent fnr) {
            personinfoMal.personIdent = fnr;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            personinfoMal.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            personinfoMal.dødsdato = dødsdato;
            return this;
        }

        public Builder medPersonstatusType(PersonstatusType personstatus) {
            personinfoMal.personstatus = personstatus;
            return this;
        }

        public Builder medNavBrukerKjønn(NavBrukerKjønn kjønn) {
            personinfoMal.kjønn = kjønn;
            return this;
        }

        public Builder medFamilierelasjon(Set<FamilierelasjonVL> familierelasjon) {
            personinfoMal.familierelasjoner = familierelasjon;
            return this;
        }

        public Builder medAdresseInfoList(List<Adresseinfo> adresseinfoArrayList) {
            personinfoMal.adresseInfoList = adresseinfoArrayList;
            return this;
        }

        public Builder medSivilstandType(SivilstandType sivilstandType) {
            personinfoMal.sivilstand = sivilstandType;
            return this;
        }

        public Builder medLandkoder(List<Landkoder> landkoder) {
            personinfoMal.landkoder = landkoder;
            return this;
        }

        public Personinfo build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId");
            requireNonNull(personinfoMal.personIdent, "Navbruker må ha fødselsnummer");
            requireNonNull(personinfoMal.navn, "Navbruker må ha navn");
            requireNonNull(personinfoMal.fødselsdato, "Navbruker må ha fødselsdato");
            requireNonNull(personinfoMal.kjønn, "Navbruker må ha kjønn");
            return personinfoMal;
        }

    }

}
