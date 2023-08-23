package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
