package no.nav.foreldrepenger.domene.entiteter;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity(name = "BeregningAktivitetOverstyringer")
@Table(name = "BG_AKTIVITET_OVERSTYRINGER")
public class BeregningAktivitetOverstyringerEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_AKTIVITET_OVERSTYRINGER")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "overstyringerEntitet")
    private List<BeregningAktivitetOverstyringEntitet> overstyringer = new ArrayList<>();

    public BeregningAktivitetOverstyringerEntitet(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringerEntitet) {
        beregningAktivitetOverstyringerEntitet.getOverstyringer().stream().map(BeregningAktivitetOverstyringEntitet::new).forEach(this::leggTilOverstyring);
    }

    public BeregningAktivitetOverstyringerEntitet() {

    }

    public Long getId() {
        return id;
    }

    public List<BeregningAktivitetOverstyringEntitet> getOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
    }

    void leggTilOverstyring(BeregningAktivitetOverstyringEntitet beregningAktivitetOverstyringEntitet) {
        if (!overstyringer.contains(beregningAktivitetOverstyringEntitet)) {
            beregningAktivitetOverstyringEntitet.setBeregningAktivitetOverstyringer(this);
            overstyringer.add(beregningAktivitetOverstyringEntitet);
        }
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningAktivitetOverstyringerEntitet kladd;

        private Builder() {
            kladd = new BeregningAktivitetOverstyringerEntitet();
        }

        public Builder leggTilOverstyring(BeregningAktivitetOverstyringEntitet beregningAktivitetOverstyring) {
            kladd.leggTilOverstyring(beregningAktivitetOverstyring);
            return this;
        }

        public BeregningAktivitetOverstyringerEntitet build() {
            return kladd;
        }
    }
}
