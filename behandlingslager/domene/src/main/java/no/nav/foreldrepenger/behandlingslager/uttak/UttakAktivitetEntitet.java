package no.nav.foreldrepenger.behandlingslager.uttak;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
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

    @Convert(converter = UttakArbeidType.KodeverdiConverter.class)
    @Column(name = "UTTAK_ARBEID_TYPE", nullable = false, updatable = false)
    private UttakArbeidType uttakArbeidType;

    public Long getId() {
        return id;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforholdRef);
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
        if (!(annen instanceof UttakAktivitetEntitet)) {
            return false;
        }

        UttakAktivitetEntitet uttakAktivitet = (UttakAktivitetEntitet) annen;
        return Objects.equals(this.getArbeidsforholdRef().orElse(null), uttakAktivitet.getArbeidsforholdRef().orElse(null)) &&
            Objects.equals(this.getArbeidsgiver(), uttakAktivitet.getArbeidsgiver()) &&
            Objects.equals(this.getUttakArbeidType(), uttakAktivitet.getUttakArbeidType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArbeidsforholdRef().orElse(null), getArbeidsgiver(), getUttakArbeidType());
    }

    public static class Builder {

        private UttakAktivitetEntitet kladd = new UttakAktivitetEntitet();

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
