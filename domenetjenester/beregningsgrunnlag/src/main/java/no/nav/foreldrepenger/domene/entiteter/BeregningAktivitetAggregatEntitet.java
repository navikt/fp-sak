package no.nav.foreldrepenger.domene.entiteter;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity(name = "BeregningAktiviteter")
@Table(name = "BG_AKTIVITETER")
public class BeregningAktivitetAggregatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_AKTIVITETER")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "beregningAktiviteter")
    private List<BeregningAktivitetEntitet> aktiviteter = new ArrayList<>();

    @Column(name = "skjaringstidspunkt_opptjening", nullable = false)
    private LocalDate skjæringstidspunktOpptjening;

    public BeregningAktivitetAggregatEntitet(BeregningAktivitetAggregatEntitet beregningAktivitetAggregatEntitet) {
        this.skjæringstidspunktOpptjening = beregningAktivitetAggregatEntitet.getSkjæringstidspunktOpptjening();
        beregningAktivitetAggregatEntitet.getBeregningAktiviteter().stream().map(BeregningAktivitetEntitet::new)
            .forEach(this::leggTilAktivitet);
    }

    public BeregningAktivitetAggregatEntitet() {

    }

    public Long getId() {
        return id;
    }

    public List<BeregningAktivitetEntitet> getBeregningAktiviteter() {
        return Collections.unmodifiableList(aktiviteter);
    }

    public LocalDate getSkjæringstidspunktOpptjening() {
        return skjæringstidspunktOpptjening;
    }

    private void leggTilAktivitet(BeregningAktivitetEntitet beregningAktivitet) {
        beregningAktivitet.setBeregningAktiviteter(this);
        aktiviteter.add(beregningAktivitet);
    }

    @Override
    public String toString() {
        return "BeregningAktivitetAggregatEntitet{" +
                "id=" + id +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningAktivitetAggregatEntitet kladd;

        private Builder() {
            kladd = new BeregningAktivitetAggregatEntitet();
        }

        public Builder medSkjæringstidspunktOpptjening(LocalDate skjæringstidspunktOpptjening) {
            kladd.skjæringstidspunktOpptjening = skjæringstidspunktOpptjening;
            return this;
        }

        public Builder leggTilAktivitet(BeregningAktivitetEntitet beregningAktivitet) {
            kladd.leggTilAktivitet(beregningAktivitet);
            return this;
        }

        public BeregningAktivitetAggregatEntitet build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.skjæringstidspunktOpptjening, "skjæringstidspunktOpptjening");
        }
    }
}
