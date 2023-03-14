package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity
@Table(name = "PSB_PERIODER")
public class PleiepengerPerioderEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_PERIODER")
    private Long id;

    @ChangeTracked
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pleiepengerPerioder")
    private List<PleiepengerInnleggelseEntitet> innleggelser = new ArrayList<>();

    PleiepengerPerioderEntitet() {
    }

    PleiepengerPerioderEntitet(PleiepengerPerioderEntitet aggregat) {
        aggregat.getInnleggelser().forEach(e -> {
                var entitet = new PleiepengerInnleggelseEntitet(e);
                innleggelser.add(entitet);
                entitet.setPleiepengerPerioder(this);
            });
    }

    public List<PleiepengerInnleggelseEntitet> getInnleggelser() {
        return innleggelser != null ? Collections.unmodifiableList(innleggelser) : List.of();
    }

    void leggTilInnleggelse(PleiepengerInnleggelseEntitet innleggelse) {
        innleggelse.setPleiepengerPerioder(this);
        innleggelser.add(innleggelse);
    }

    public static class Builder {

        private final PleiepengerPerioderEntitet kladd;

        public Builder() {
            this.kladd = new PleiepengerPerioderEntitet();
        }

        public Builder leggTil(PleiepengerInnleggelseEntitet.Builder builder) {
            kladd.leggTilInnleggelse(builder.build());
            return this;
        }

        public PleiepengerPerioderEntitet build() {
            return kladd;
        }

        public boolean harPerioder() {
            return !this.kladd.getInnleggelser().isEmpty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (PleiepengerPerioderEntitet) o;
        return innleggelser.size() == that.innleggelser.size() && innleggelser.containsAll(that.innleggelser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(innleggelser);
    }
}
