package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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

    @Column(name = "fom")
    private LocalDate førsteMuligeRefusjonFom;

    @JsonBackReference
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "br_overstyringer_id", nullable = false, updatable = false)
    private BeregningRefusjonOverstyringerEntitet refusjonOverstyringer;

    @OneToMany(mappedBy = "refusjonOverstyring")
    private List<BeregningRefusjonPeriodeEntitet> refusjonPerioder = new ArrayList<>();

    public BeregningRefusjonOverstyringEntitet() {
        // Hibernate
    }

    void setRefusjonOverstyringerEntitet(BeregningRefusjonOverstyringerEntitet refusjonOverstyringer) {
        this.refusjonOverstyringer = refusjonOverstyringer;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public Optional<LocalDate> getFørsteMuligeRefusjonFom() {
        return Optional.ofNullable(førsteMuligeRefusjonFom);
    }

    public List<BeregningRefusjonPeriodeEntitet> getRefusjonPerioder() {
        return refusjonPerioder;
    }

    public static BeregningRefusjonOverstyringEntitet.Builder builder() {
        return new BeregningRefusjonOverstyringEntitet.Builder();
    }

    public static class Builder {
        private final BeregningRefusjonOverstyringEntitet kladd;

        private Builder() {
            kladd = new BeregningRefusjonOverstyringEntitet();
        }

        public BeregningRefusjonOverstyringEntitet.Builder leggTilRefusjonPeriode(BeregningRefusjonPeriodeEntitet beregningRefusjonStart) {
            beregningRefusjonStart.setRefusjonOverstyringEntitet(kladd);
            kladd.refusjonPerioder.add(beregningRefusjonStart);
            return this;
        }

        public BeregningRefusjonOverstyringEntitet.Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }


        public BeregningRefusjonOverstyringEntitet.Builder medFørsteMuligeRefusjonFom(LocalDate førsteMuligeRefusjonFom) {
            kladd.førsteMuligeRefusjonFom = førsteMuligeRefusjonFom;
            return this;
        }


        public BeregningRefusjonOverstyringEntitet build() {
            kladd.verifiserTilstand();
            return kladd;
        }
    }

    private void verifiserTilstand() {
        Objects.requireNonNull(arbeidsgiver, "arbeidsgiver");
        if (førsteMuligeRefusjonFom == null && refusjonPerioder.isEmpty()) {
            throw new IllegalStateException("Objektet inneholder ingen informasjon om refusjon, ugyldig tilstand");
        }
    }

}
