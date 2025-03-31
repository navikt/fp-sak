package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity
@Table(name = "UTTAK_RESULTAT_DOK_REGEL")
public class UttakResultatDokRegelEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_RESULTAT_DOK_REGEL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    /**
     * Er egentlig en-til-en, men gjort om til onetomany for å force loading. Funket ikke med lazy og en-til-en
     */
    @ManyToOne
    @JoinColumn(name = "uttak_resultat_periode_id", updatable = false, nullable = false)
    private UttakResultatPeriodeEntitet periode;

    @Lob
    @Column(name = "regel_input", updatable = false)
    private String regelInput;

    @Lob
    @Column(name = "regel_evaluering", updatable = false)
    private String regelEvaluering;

    @Column(name = "regel_versjon")
    private String regelVersjon;

    public Long getId() {
        return id;
    }

    public String getRegelInput() {
        return regelInput;
    }

    public String getRegelEvaluering() {
        return regelEvaluering;
    }

    public String getRegelVersjon() {
        return regelVersjon;
    }

    @Override
    public String toString() {
        return "UttakResultatDokRegelEntitet{" + "versjon=" + versjon + ", id=" + id + '}';
    }

    public void setPeriode(UttakResultatPeriodeEntitet periode) {
        this.periode = periode;
    }

    public static class Builder {

        private final UttakResultatDokRegelEntitet kladd = new UttakResultatDokRegelEntitet();

        public Builder medRegelInput(String regelInput) {
            kladd.regelInput = regelInput;
            return this;
        }

        public Builder medRegelEvaluering(String regelEvaluering) {
            kladd.regelEvaluering = regelEvaluering;
            return this;
        }

        public Builder medRegelVersjon(String regelVersjon) {
            kladd.regelVersjon = regelVersjon;
            return this;
        }

        public UttakResultatDokRegelEntitet build() {
            return kladd;
        }
    }
}
