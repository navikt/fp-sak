package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity
@Table(name = "SVP_TILRETTELEGGINGER")
public class SvpTilretteleggingerEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_TILRETTELEGGINGER")
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tilrettelegginger")
    @BatchSize(size = 25)
    private List<SvpTilretteleggingEntitet> tilretteleggingListe = new ArrayList<>();

    public List<SvpTilretteleggingEntitet> getTilretteleggingListe() {
        return Collections.unmodifiableList(tilretteleggingListe);
    }

    @Override
    public String toString() {
        return "SvpTilretteleggingerEntitet{" + "tilretteleggingListe=" + tilretteleggingListe + '}';
    }

    public static class Builder {

        private List<SvpTilretteleggingEntitet> tilretteleggingListe = new ArrayList<>();

        public SvpTilretteleggingerEntitet build() {
            var entitet = new SvpTilretteleggingerEntitet();
            for (var tilrettelegging : this.tilretteleggingListe) {
                var svpTilretteleggingEntitet = new SvpTilretteleggingEntitet(tilrettelegging, entitet);
                entitet.tilretteleggingListe.add(svpTilretteleggingEntitet);
            }
            return entitet;
        }

        public Builder medTilretteleggingListe(List<SvpTilretteleggingEntitet> tilretteleggingListe) {
            this.tilretteleggingListe = tilretteleggingListe;
            return this;
        }
    }
}
