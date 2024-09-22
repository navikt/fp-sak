package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Entitetsklasse for opphold.
 *
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 *
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
@Entity(name = "OppgittLandOpphold")
@Table(name = "MEDLEMSKAP_OPPG_LAND")
public class MedlemskapOppgittLandOppholdEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_OPPG_LAND")
    private Long id;

    @ChangeTracked
    @Convert(converter = Landkoder.KodeverdiConverter.class)
    @Column(name="land", nullable = false)
    private Landkoder land = Landkoder.UDEFINERT;

    @ChangeTracked
    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "periode_fom"))
    @AttributeOverride(name = "tomDato", column = @Column(name = "periode_tom"))
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "tidligere_opphold", nullable = false)
    private boolean tidligereOpphold;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medlemskap_oppg_tilknyt_id", nullable = false, updatable = false)
    private MedlemskapOppgittTilknytningEntitet oppgittTilknytning;

    MedlemskapOppgittLandOppholdEntitet() {
        // Hibernate
    }

    public MedlemskapOppgittLandOppholdEntitet(MedlemskapOppgittLandOppholdEntitet utlandsopphold) {
        this.setLand(utlandsopphold.getLand());
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(
            utlandsopphold.getPeriodeFom(),
            utlandsopphold.getPeriodeTom()
                );
        this.tidligereOpphold = utlandsopphold.isTidligereOpphold();

        // kopier ikke oppgitt tilknytning. Det settes p.t. separat i builder (setOppgittTilknytning) for å knytte til OppgittTilknytningEntitet
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(this.land, periode);
    }


    public Landkoder getLand() {
        return Objects.equals(Landkoder.UDEFINERT, land) ? null : land;
    }


    public LocalDate getPeriodeFom() {
        return periode != null ? periode.getFomDato() : null;
    }


    public LocalDate getPeriodeTom() {
        return periode != null ? periode.getTomDato() : null;
    }


    public boolean isTidligereOpphold() {
        return tidligereOpphold;
    }

    void setLand(Landkoder land) {
        this.land = land == null ? Landkoder.UDEFINERT : land;
    }

    void setPeriode(LocalDate periodeFom, LocalDate periodeTom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeFom, periodeTom);
    }

    void setTidligereOpphold(boolean tidligereOpphold) {
        this.tidligereOpphold = tidligereOpphold;
    }

    void setOppgittTilknytning(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        this.oppgittTilknytning = oppgittTilknytning;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MedlemskapOppgittLandOppholdEntitet other)) {
            return false;
        }
        return Objects.equals(this.getLand(), other.getLand())
                && Objects.equals(this.periode, other.periode)
                && Objects.equals(this.tidligereOpphold, other.isTidligereOpphold());
    }


    @Override
    public int hashCode() {
        return Objects.hash(getLand(), periode, tidligereOpphold);
    }

    public static class Builder {
        private MedlemskapOppgittLandOppholdEntitet oppholdMal;

        public Builder() {
            oppholdMal = new MedlemskapOppgittLandOppholdEntitet();
        }

        public Builder(MedlemskapOppgittLandOppholdEntitet utlandsopphold) {
            if (utlandsopphold != null) {
                oppholdMal = new MedlemskapOppgittLandOppholdEntitet(utlandsopphold);
            } else {
                oppholdMal = new MedlemskapOppgittLandOppholdEntitet();
            }
        }

        public Builder medLand(Landkoder land) {
            oppholdMal.setLand(land);
            return this;
        }

        public Builder medPeriode(LocalDate periodeStartdato, LocalDate periodeSluttdato) {
            oppholdMal.periode = DatoIntervallEntitet.fraOgMedTilOgMed(periodeStartdato, periodeSluttdato);
            return this;
        }

        public Builder erTidligereOpphold(boolean tidligereOpphold) {
            oppholdMal.tidligereOpphold = tidligereOpphold;
            return this;
        }

        public MedlemskapOppgittLandOppholdEntitet build() {
            return oppholdMal;
        }
    }
}
