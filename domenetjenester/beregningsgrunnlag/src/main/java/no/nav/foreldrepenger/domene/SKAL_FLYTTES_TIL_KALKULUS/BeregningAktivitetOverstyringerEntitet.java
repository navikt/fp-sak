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

    public Long getId() {
        return id;
    }

    public List<BeregningAktivitetOverstyringEntitet> getOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
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
            BeregningAktivitetOverstyringEntitet entitet = beregningAktivitetOverstyring;
            kladd.overstyringer.add(entitet);
            entitet.setBeregningAktivitetOverstyringer(kladd);
            return this;
        }

        public BeregningAktivitetOverstyringerEntitet build() {
            return kladd;
        }
    }
}
