package no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning.PersonRelasjon;

public final class PersonRelasjon implements HarAktørId {

    private AktørId fraAktørId;
    private AktørId tilAktørId;
    private RelasjonsRolleType relasjonsrolle;
    private Boolean harSammeBosted;

    @Override
    public AktørId getAktørId() {
        return fraAktørId;
    }

    public AktørId getTilAktørId() {
        return tilAktørId;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    private PersonRelasjon(Builder builder) {
        this.fraAktørId = builder.fraAktørId;
        this.tilAktørId = builder.tilAktørId;
        this.relasjonsrolle = builder.relasjonsrolle;
        this.harSammeBosted = builder.harSammeBosted;
    }

    public static Builder builder() {
        return new Builder();
    }


    static final class Builder {
        private AktørId fraAktørId;
        private AktørId tilAktørId;
        private RelasjonsRolleType relasjonsrolle;
        private Boolean harSammeBosted;

        private Builder() {
        }

        PersonRelasjon build() {
            return new PersonRelasjon(this);
        }

        Builder fraAktørId(AktørId fraAktørId) {
            this.fraAktørId = fraAktørId;
            return this;
        }

        Builder tilAktørId(AktørId tilAktørId) {
            this.tilAktørId = tilAktørId;
            return this;
        }

        Builder relasjonsrolle(RelasjonsRolleType relasjonsrolle) {
            this.relasjonsrolle = relasjonsrolle;
            return this;
        }

        Builder harSammeBosted(Boolean harSammeBosted) {
            this.harSammeBosted = harSammeBosted;
            return this;
        }
    }
}
