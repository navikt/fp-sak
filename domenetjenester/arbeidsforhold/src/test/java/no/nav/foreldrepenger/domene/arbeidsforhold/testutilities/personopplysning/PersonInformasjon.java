package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.personopplysning;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;

public final class PersonInformasjon {

    private PersonopplysningVersjonType type;
    private List<Personstatus> personstatuser = new ArrayList<>();
    private List<Statsborgerskap> statsborgerskap = new ArrayList<>();
    private List<Personopplysning> personopplysninger = new ArrayList<>();
    private List<PersonRelasjon> relasjoner = new ArrayList<>();

    public PersonopplysningVersjonType getType() {
        return type;
    }

    public List<Personstatus> getPersonstatuser() {
        return personstatuser;
    }

    public List<Statsborgerskap> getStatsborgerskap() {
        return statsborgerskap;
    }

    public List<Personopplysning> getPersonopplysninger() {
        return personopplysninger;
    }

    public List<PersonRelasjon> getRelasjoner() {
        return relasjoner;
    }

    public static Builder builder(PersonopplysningVersjonType type) {
        return new Builder(type);
    }

    public static final class Builder {
        private PersonInformasjon kladd = new PersonInformasjon();

        private Builder(PersonopplysningVersjonType type) {
            kladd.type = type;
        }

        public Builder leggTilPersonopplysninger(Personopplysning.Builder builder) {
            kladd.personopplysninger.add(builder.build());
            return this;
        }

        public PersonInformasjon build() {
            return kladd;
        }

        public Personas medPersonas() {
            return new Personas(this);
        }

    }

    public static Builder buildOverstyrt() {
        return new Builder(PersonopplysningVersjonType.OVERSTYRT);
    }

    public static Builder buildRegistrert() {
        return new Builder(PersonopplysningVersjonType.REGISTRERT);
    }
}
