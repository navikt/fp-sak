package no.nav.foreldrepenger.domene.entiteter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    public BeregningRefusjonOverstyringerEntitet(BeregningRefusjonOverstyringerEntitet beregningRefusjonOverstyringerEntitet) {
        beregningRefusjonOverstyringerEntitet.getRefusjonOverstyringer().stream().map(BeregningRefusjonOverstyringEntitet::new)
            .forEach(this::leggTilRefusjonOverstyring);
    }

    public List<BeregningRefusjonOverstyringEntitet> getRefusjonOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
    }

    void leggTilRefusjonOverstyring(BeregningRefusjonOverstyringEntitet beregningRefusjonOverstyringEntitet) {
        if (!overstyringer.contains(beregningRefusjonOverstyringEntitet)) {
            beregningRefusjonOverstyringEntitet.setRefusjonOverstyringerEntitet(this);
            overstyringer.add(beregningRefusjonOverstyringEntitet);
        }
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
            kladd.leggTilRefusjonOverstyring(beregningRefusjonOverstyring);
            return this;
        }

        public BeregningRefusjonOverstyringerEntitet build() {
            return kladd;
        }
    }
}
