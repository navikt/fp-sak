package no.nav.foreldrepenger.behandlingslager.aktør.historikk;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Personhistorikkinfo {

    private String aktørId;
    private List<PersonstatusPeriode> personstatushistorikk = new ArrayList<>();
    private List<OppholdstillatelsePeriode> oppholdstillatelsehistorikk = new ArrayList<>();
    private List<StatsborgerskapPeriode> statsborgerskaphistorikk = new ArrayList<>();
    private List<AdressePeriode> adressehistorikk = new ArrayList<>();

    private Personhistorikkinfo() {
    }

    public String getAktørId() {
        return this.aktørId;
    }

    public List<PersonstatusPeriode> getPersonstatushistorikk() {
        return this.personstatushistorikk;
    }

    public List<StatsborgerskapPeriode> getStatsborgerskaphistorikk() {
        return this.statsborgerskaphistorikk;
    }

    public List<AdressePeriode> getAdressehistorikk() {
        return this.adressehistorikk;
    }

    public List<OppholdstillatelsePeriode> getOppholdstillatelsehistorikk() {
        return oppholdstillatelsehistorikk;
    }

    @Override
    public String toString() {
        return "Personhistorikkinfo{" +
            "personstatushistorikk=" + personstatushistorikk +
            ", oppholdstillatelsehistorikk=" + oppholdstillatelsehistorikk +
            ", statsborgerskaphistorikk=" + statsborgerskaphistorikk +
            ", adressehistorikk=" + adressehistorikk +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Personhistorikkinfo) o;
        return Objects.equals(aktørId, that.aktørId) &&
            Objects.equals(personstatushistorikk, that.personstatushistorikk) &&
            Objects.equals(oppholdstillatelsehistorikk, that.oppholdstillatelsehistorikk) &&
            Objects.equals(statsborgerskaphistorikk, that.statsborgerskaphistorikk) &&
            Objects.equals(adressehistorikk, that.adressehistorikk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, personstatushistorikk, oppholdstillatelsehistorikk, statsborgerskaphistorikk, adressehistorikk);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Personhistorikkinfo kladd;

        private Builder() {
            this.kladd = new Personhistorikkinfo();
        }

        public Builder medAktørId(String aktørId) {
            this.kladd.aktørId = aktørId;
            return this;
        }

        public Builder leggTil(PersonstatusPeriode personstatus) {
            this.kladd.personstatushistorikk.add(personstatus);
            return this;
        }

        public Builder leggTil(OppholdstillatelsePeriode tillatelse) {
            this.kladd.oppholdstillatelsehistorikk.add(tillatelse);
            return this;
        }

        public Builder leggTil(StatsborgerskapPeriode statsborgerskap) {
            this.kladd.statsborgerskaphistorikk.add(statsborgerskap);
            return this;
        }

        public Builder leggTil(AdressePeriode adresse) {
            this.kladd.adressehistorikk.add(adresse);
            return this;
        }

        public Personhistorikkinfo build() {
            requireNonNull(kladd.aktørId, "Personhistorikkinfo må ha aktørId");
            // TODO PK-49366 andre non-null?
            return kladd;
        }
    }
}
