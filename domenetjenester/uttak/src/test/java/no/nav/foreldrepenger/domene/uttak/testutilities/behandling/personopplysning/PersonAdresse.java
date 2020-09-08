package no.nav.foreldrepenger.domene.uttak.testutilities.behandling.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public final class PersonAdresse {

    private AktørId aktørId;
    private DatoIntervallEntitet periode;
    private AdresseType adresseType;
    private String adresselinje1;
    private String postnummer;
    private String land;

    public AktørId getAktørId() {
        return aktørId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public AdresseType getAdresseType() {
        return adresseType;
    }

    public String getAdresselinje1() {
        return adresselinje1;
    }

    public String getPostnummer() {
        return postnummer;
    }

    public String getLand() {
        return land;
    }

    private PersonAdresse(Builder builder) {
        this.aktørId = builder.aktørId;
        this.periode = builder.periode;
        this.adresseType = builder.adresseType;
        this.adresselinje1 = builder.adresselinje1;
        this.postnummer = builder.postnummer;
        this.land = builder.land;
    }

    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder {
        private AktørId aktørId;
        private DatoIntervallEntitet periode;
        private AdresseType adresseType;
        private String adresselinje1;
        private String postnummer;
        private String land;

        private Builder() {
        }

        public PersonAdresse build() {
            return new PersonAdresse(this);
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder periode(LocalDate fom, LocalDate tom) {
            this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder adresseType(AdresseType adresseType) {
            this.adresseType = adresseType;
            return this;
        }

        public Builder adresselinje1(String adresselinje1) {
            this.adresselinje1 = adresselinje1;
            return this;
        }

        public Builder postnummer(String postnummer) {
            this.postnummer = postnummer;
            return this;
        }

        public Builder land(Landkoder land) {
            this.land = land.getKode(); // TODO (FC) Skriv om hele veien til Landkoder
            return this;
        }

        public DatoIntervallEntitet getPeriode() {
            return periode;
        }
    }
}
