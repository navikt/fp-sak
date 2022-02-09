package no.nav.foreldrepenger.domene.entiteter;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@Entity(name = "BeregningRefusjonPeriode")
@Table(name = "BG_REFUSJON_PERIODE")
public class BeregningRefusjonPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_REFUSJON_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Column(name = "fom", nullable = false)
    private LocalDate startdatoRefusjon;

    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "bg_refusjon_overstyring_id", nullable = false, updatable = false)
    private BeregningRefusjonOverstyringEntitet refusjonOverstyring;

    public BeregningRefusjonPeriodeEntitet() {
        // Hibernate
    }

    public BeregningRefusjonPeriodeEntitet(InternArbeidsforholdRef ref, LocalDate startdatoRefusjon) {
        Objects.requireNonNull(startdatoRefusjon, "startdatoRefusjon");
        this.arbeidsforholdRef = ref;
        this.startdatoRefusjon = startdatoRefusjon;
    }

    public BeregningRefusjonPeriodeEntitet(LocalDate startdatoRefusjon) {
        Objects.requireNonNull(startdatoRefusjon, "startdatoRefusjon");
        this.startdatoRefusjon = startdatoRefusjon;
    }

    public BeregningRefusjonPeriodeEntitet(BeregningRefusjonPeriodeEntitet beregningRefusjonPeriodeEntitet) {
        this.arbeidsforholdRef = beregningRefusjonPeriodeEntitet.getArbeidsforholdRef();
        this.startdatoRefusjon = beregningRefusjonPeriodeEntitet.getStartdatoRefusjon();
    }

    void setRefusjonOverstyringEntitet(BeregningRefusjonOverstyringEntitet refusjonOverstyring) {
        this.refusjonOverstyring = refusjonOverstyring;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public LocalDate getStartdatoRefusjon() {
        return startdatoRefusjon;
    }
}
