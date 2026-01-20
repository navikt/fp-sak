package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@Entity
@Table(name = "UTTAK_AKTIVITET")
public class UttakAktivitetEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_AKTIVITET")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    @ChangeTracked
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "UTTAK_ARBEID_TYPE", nullable = false, updatable = false)
    private UttakArbeidType uttakArbeidType;

    public Long getId() {
        return id;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    @Override
    public String toString() {
        return "UttakAktivitetEntitet{" +
            "arbeidsgiver=" + arbeidsgiver +
            ", arbeidsforholdRef=" + arbeidsforholdRef +
            ", uttakArbeidType=" + uttakArbeidType.getKode() +
            '}';
    }

    @Override
    public boolean equals(Object annen) {
        if (annen == this) {
            return true;
        }
        if (!(annen instanceof UttakAktivitetEntitet uttakAktivitet)) {
            return false;
        }

        return Objects.equals(this.getArbeidsforholdRef(), uttakAktivitet.getArbeidsforholdRef()) &&
            Objects.equals(this.getArbeidsgiver(), uttakAktivitet.getArbeidsgiver()) &&
            Objects.equals(this.getUttakArbeidType(), uttakAktivitet.getUttakArbeidType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArbeidsforholdRef(), getArbeidsgiver(), getUttakArbeidType());
    }

    public static class Builder {

        private final UttakAktivitetEntitet kladd = new UttakAktivitetEntitet();

        public Builder medArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsgiver = arbeidsgiver;
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medUttakArbeidType(UttakArbeidType uttakArbeidType) {
            kladd.uttakArbeidType = uttakArbeidType;
            return this;
        }

        public UttakAktivitetEntitet build() {
            Objects.requireNonNull(kladd.uttakArbeidType, "uttakArbeidType");
            return kladd;
        }
    }
}
