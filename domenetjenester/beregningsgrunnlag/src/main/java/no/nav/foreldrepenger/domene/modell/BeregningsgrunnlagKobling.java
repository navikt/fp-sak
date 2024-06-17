package no.nav.foreldrepenger.domene.modell;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class BeregningsgrunnlagKobling {

    private LocalDate skjæringstidspunkt;
    private UUID referanse;

    public BeregningsgrunnlagKobling(LocalDate skjæringstidspunkt, UUID referanse) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.referanse = referanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (BeregningsgrunnlagKobling) o;
        return Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt) && Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt, referanse);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagKobling{" + "skjæringstidspunkt=" + skjæringstidspunkt + ", referanse=" + referanse + '}';
    }
}
