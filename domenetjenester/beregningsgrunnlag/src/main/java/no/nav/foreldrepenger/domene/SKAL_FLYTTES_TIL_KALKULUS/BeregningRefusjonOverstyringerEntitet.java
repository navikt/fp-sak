package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "BeregningRefusjonOverstyringer")
@Table(name = "BG_REFUSJON_OVERSTYRINGER")
public class BeregningRefusjonOverstyringerEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_REFUSJON_OVERSTYRINGER")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "refusjonOverstyringer")
    private List<BeregningRefusjonOverstyringEntitet> overstyringer = new ArrayList<>();

    public BeregningRefusjonOverstyringerEntitet() {
        // Hibernate
    }

    public List<BeregningRefusjonOverstyringEntitet> getRefusjonOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningRefusjonOverstyringerEntitet kladd;

        private Builder() {
            kladd = new BeregningRefusjonOverstyringerEntitet();
        }

        public Builder leggTilOverstyring(BeregningRefusjonOverstyringEntitet beregningRefusjonOverstyring) {
            BeregningRefusjonOverstyringEntitet entitet = beregningRefusjonOverstyring;
            entitet.setRefusjonOverstyringerEntitet(kladd);
            kladd.overstyringer.add(entitet);
            return this;
        }

        public BeregningRefusjonOverstyringerEntitet build() {
            return kladd;
        }
    }
}
