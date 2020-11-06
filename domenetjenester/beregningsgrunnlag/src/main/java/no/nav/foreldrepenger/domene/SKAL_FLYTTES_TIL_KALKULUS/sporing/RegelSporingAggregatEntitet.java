package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.sporing;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.folketrygdloven.kalkulus.felles.jpa.BaseEntitet;

@Entity(name = "RegelSporingAggregatEntitet")
@Table(name = "BG_REGEL_SPORING_AGGREGAT")
public class RegelSporingAggregatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_REGEL_SPORING_AGGREGAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "regelSporingAggregat")
    private List<RegelSporingGrunnlagEntitet> regelSporingGrunnlagListe = new ArrayList<>();

    @OneToMany(mappedBy = "regelSporingAggregat")
    private List<RegelSporingPeriodeEntitet> regelSporingPeriodeListe = new ArrayList<>();

    public RegelSporingAggregatEntitet() {
    }

    public Long getId() {
        return id;
    }

    public List<RegelSporingGrunnlagEntitet> getRegelSporingGrunnlagListe() {
        return regelSporingGrunnlagListe;
    }

    public List<RegelSporingPeriodeEntitet> getRegelSporingPeriodeListe() {
        return regelSporingPeriodeListe;
    }

    private void leggTilRegelSporingGrunnlag(RegelSporingGrunnlagEntitet regelSporingGrunnlag) {
        regelSporingGrunnlag.setRegelSporingAggregat(this);
        regelSporingGrunnlagListe.add(regelSporingGrunnlag);
    }

    private void leggTilRegelSporingPeriode(RegelSporingPeriodeEntitet regelSporingPeriode) {
        regelSporingPeriode.setRegelSporingAggregat(this);
        regelSporingPeriodeListe.add(regelSporingPeriode);
    }


    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {

        private RegelSporingAggregatEntitet kladd;

        Builder() {
            kladd = new RegelSporingAggregatEntitet();
        }

        public Builder leggTilRegelSporingGrunnlag(RegelSporingGrunnlagEntitet regelSporingGrunnlag) { // NOSONAR
            kladd.leggTilRegelSporingGrunnlag(regelSporingGrunnlag);
            return this;
        }

        public Builder leggTilRegelSporingPeriode(RegelSporingPeriodeEntitet regelSporingPeriode) { // NOSONAR
            kladd.leggTilRegelSporingPeriode(regelSporingPeriode);
            return this;
        }

        public RegelSporingAggregatEntitet build() {
            return kladd;
        }

    }


}
