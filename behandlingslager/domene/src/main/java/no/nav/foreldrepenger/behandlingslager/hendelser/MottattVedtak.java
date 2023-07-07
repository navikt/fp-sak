package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;

@Entity(name = "MottattVedtak")
@Table(name = "MOTTATT_VEDTAK")
public class MottattVedtak extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MOTTATT_VEDTAK")
    private Long id;

    // Må kunne håndtere saksnummer fra eksterne system
    @Column(name = "SAKSNUMMER", nullable = false)
    private String saksnummer;

    @Column(name = "FAGSYSTEM", nullable = false)
    private String fagsystem;

    @Column(name = "YTELSE", nullable = false)
    private String ytelse;

    @Column(name = "REFERANSE", nullable = false)
    private String referanse;

    MottattVedtak() {
        //for hibernate
    }

    public Long getId() {
        return id;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getFagsystem() {
        return fagsystem;
    }

    public String getYtelse() {
        return ytelse;
    }

    public String getReferanse() {
        return referanse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (MottattVedtak) o;
        return Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(fagsystem, that.fagsystem) &&
            Objects.equals(ytelse, that.ytelse) &&
            Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, fagsystem, ytelse, referanse);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MottattVedtak kladd;

        private Builder() {
            kladd = new MottattVedtak();
        }

        public Builder medSaksnummer(String saksnummer) {
            this.kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medFagsystem(String fagsystem) {
            this.kladd.fagsystem = fagsystem;
            return this;
        }

        public Builder medYtelse(String ytelse) {
            this.kladd.ytelse = ytelse;
            return this;
        }

        public Builder medReferanse(String referanse) {
            this.kladd.referanse = referanse;
            return this;
        }

        public MottattVedtak build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.saksnummer);
            Objects.requireNonNull(kladd.fagsystem);
            Objects.requireNonNull(kladd.ytelse);
            Objects.requireNonNull(kladd.referanse);
        }
    }
}
