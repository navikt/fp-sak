package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

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
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

@Entity(name = "BeregningRefusjonOverstyring")
@Table(name = "BG_REFUSJON_OVERSTYRING")
public class BeregningRefusjonOverstyringEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_REFUSJON_OVERSTYRING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Column(name = "fom", nullable = false)
    private LocalDate førsteMuligeRefusjonFom;

    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "br_overstyringer_id", nullable = false, updatable = false)
    private BeregningRefusjonOverstyringerEntitet refusjonOverstyringer;

    public BeregningRefusjonOverstyringEntitet() {
        // Hibernate
    }

    public BeregningRefusjonOverstyringEntitet(Arbeidsgiver arbeidsgiver, LocalDate førsteMuligeRefusjonFom) {
        Objects.requireNonNull(arbeidsgiver);
        Objects.requireNonNull(førsteMuligeRefusjonFom);
        this.førsteMuligeRefusjonFom = førsteMuligeRefusjonFom;
        this.arbeidsgiver = arbeidsgiver;
    }

    void setRefusjonOverstyringerEntitet(BeregningRefusjonOverstyringerEntitet refusjonOverstyringer) {
        this.refusjonOverstyringer = refusjonOverstyringer;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public LocalDate getFørsteMuligeRefusjonFom() {
        return førsteMuligeRefusjonFom;
    }
}
