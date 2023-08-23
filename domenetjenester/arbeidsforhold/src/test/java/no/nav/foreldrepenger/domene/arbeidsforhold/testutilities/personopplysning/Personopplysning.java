package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.personopplysning;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.time.LocalDate;

public final class Personopplysning {

    private final AktørId aktørId;
    private final SivilstandType sivilstand;
    private final String navn;
    private final LocalDate dødsdato;
    private final LocalDate fødselsdato;

    public AktørId getAktørId() {
        return aktørId;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    private Personopplysning(Builder builder) {
        this.aktørId = builder.aktørId;
        this.sivilstand = builder.sivilstand;
        this.navn = builder.navn;
        this.dødsdato = builder.dødsdato;
        this.fødselsdato = builder.fødselsdato;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AktørId aktørId;
        private SivilstandType sivilstand;
        private String navn;
        private LocalDate dødsdato;
        private LocalDate fødselsdato;

        private Builder() {
        }

        public Personopplysning build() {
            return new Personopplysning(this);
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder sivilstand(SivilstandType sivilstand) {
            this.sivilstand = sivilstand;
            return this;
        }

        public Builder navn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder dødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public Builder fødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }
    }
}
