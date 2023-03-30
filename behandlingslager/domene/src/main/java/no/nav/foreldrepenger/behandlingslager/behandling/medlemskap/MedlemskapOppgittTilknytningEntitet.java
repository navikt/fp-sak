package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Entitetsklasse for oppgitt tilknytning.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */

@Entity(name = "OppgittTilknytning")
@Table(name = "MEDLEMSKAP_OPPG_TILKNYT")
public class MedlemskapOppgittTilknytningEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_OPPG_TILKNYT")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "opphold_naa", nullable = false)
    private boolean oppholdNå;

    @Column(name = "oppgitt_dato", nullable = false)
    private LocalDate oppgittDato;


    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "oppgittTilknytning")
    private Set<MedlemskapOppgittLandOppholdEntitet> opphold = new HashSet<>(2);

    public MedlemskapOppgittTilknytningEntitet() {
        // Hibernate
    }

    /**
     * Deep copy
     */
    public MedlemskapOppgittTilknytningEntitet(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        this.oppholdNå = oppgittTilknytning.isOppholdNå();
        this.oppgittDato = oppgittTilknytning.getOppgittDato();
        for (var utl : oppgittTilknytning.getOpphold()) {
            var ue = new MedlemskapOppgittLandOppholdEntitet(utl);
            ue.setOppgittTilknytning(this);
            this.opphold.add(ue);
        }
    }


    public boolean isOppholdNå() {
        return oppholdNå;
    }

    void setOppholdNå(boolean oppholdNorgeNå) {
        this.oppholdNå = oppholdNorgeNå;
    }


    public LocalDate getOppgittDato() {
        return oppgittDato;
    }

    void setOppgittDato(LocalDate oppgittDato) {
        this.oppgittDato = oppgittDato;
    }


    public Set<MedlemskapOppgittLandOppholdEntitet> getOpphold() {
        return Collections.unmodifiableSet(opphold);
    }


    public boolean isOppholdINorgeSistePeriode() {
        return opphold.stream()
                .noneMatch(o -> !o.getLand().equals(Landkoder.NOR) && o.isTidligereOpphold());
    }


    public boolean isOppholdINorgeNestePeriode() {
        return opphold.stream()
                .noneMatch(o -> !o.getLand().equals(Landkoder.NOR) && !o.isTidligereOpphold());
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MedlemskapOppgittTilknytningEntitet other)) {
            return false;
        }
        return Objects.equals(this.oppholdNå, other.oppholdNå)
                && Objects.equals(this.opphold, other.opphold);
    }


    @Override
    public int hashCode() {
        return Objects.hash(oppholdNå, opphold);
    }

    public static class Builder {
        private MedlemskapOppgittTilknytningEntitet mal;

        public Builder() {
            mal = new MedlemskapOppgittTilknytningEntitet();
        }

        public Builder(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
            if (oppgittTilknytning != null) {
                mal = new MedlemskapOppgittTilknytningEntitet(oppgittTilknytning);
            } else {
                mal = new MedlemskapOppgittTilknytningEntitet();
            }
        }

        public Builder medOppholdNå(boolean oppholdNorgeNå) {
            mal.setOppholdNå(oppholdNorgeNå);
            return this;
        }

        public Builder medOppgittDato(LocalDate oppgittDato) {
            mal.setOppgittDato(oppgittDato);
            return this;
        }

        public Builder leggTilOpphold(MedlemskapOppgittLandOppholdEntitet oppholdUtland) {
            var ue = new MedlemskapOppgittLandOppholdEntitet(oppholdUtland);
            ue.setOppgittTilknytning(mal);
            mal.opphold.add(ue);
            return this;
        }

        public Builder medOpphold(List<MedlemskapOppgittLandOppholdEntitet> opphold) {
            var oppholdEntiteter = opphold.stream().map(o -> new MedlemskapOppgittLandOppholdEntitet(o)).collect(Collectors.toCollection(LinkedHashSet::new));
            mal.opphold = oppholdEntiteter;
            return this;
        }

        public MedlemskapOppgittTilknytningEntitet build() {
            return mal;
        }
    }
}
