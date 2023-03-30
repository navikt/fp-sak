package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Entitet som representerer Opptjening. Denne har også et sett med {@link OpptjeningAktivitet}.
 * Grafen her er immutable og tillater ikke endring av data elementer annet enn metadata (aktiv flagg osv.)
 * {@link OpptjeningRepository} besørger riktig oppdatering og skriving, og oppretting av nytt innslag ved hver endring.
 *
 */
@Entity(name = "Opptjening")
@Table(name = "OPPTJENING")
public class Opptjening extends BaseEntitet {

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @ChangeTracked
    @Embedded
    private DatoIntervallEntitet opptjeningPeriode;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPTJENING")
    private Long id;

    /* Mapper kun fra denne og ikke bi-directional, gjør vedlikehold enklere. */
    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true /* ok siden aktiviteter er eid av denne */)
    @JoinColumn(name = "OPPTJENINGSPERIODE_ID", nullable = false, updatable = false)
    private List<OpptjeningAktivitet> opptjeningAktivitet = new ArrayList<>();

    @ChangeTracked
    @Column(name = "opptjent_periode")
    private String opptjentPeriode;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToOne(optional = false)
    @JoinColumn(name = "vilkar_resultat_id", nullable = false, updatable = false)
    private VilkårResultat vilkårResultat;

    public Opptjening(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom, "opptjeningsperiodeFom");
        Objects.requireNonNull(tom, "opptjeningsperiodeTom");
        this.opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    /** copy-constructor. */
    public Opptjening(Opptjening annen) {
        this.opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(annen.getFom(), annen.getTom());
        this.opptjentPeriode = annen.getOpptjentPeriode() == null ? null : annen.getOpptjentPeriode().toString();
        this.opptjeningAktivitet
                .addAll(annen.getOpptjeningAktivitet().stream().map(OpptjeningAktivitet::new).toList());
        // kopierer ikke data som ikke er relevante (aktiv, versjon, id, etc)

    }

    Opptjening() {
        // for hibernate
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Opptjening other)) {
            return false;
        }
        return Objects.equals(this.getFom(), other.getFom())
                && Objects.equals(this.getTom(), other.getTom());
    }

    public Boolean getAktiv() {
        return aktiv;
    }

    public LocalDate getFom() {
        return opptjeningPeriode.getFomDato();
    }

    public Long getId() {
        return id;
    }

    public List<OpptjeningAktivitet> getOpptjeningAktivitet() {
        // alle returnerte data fra denne klassen skal være immutable
        return Collections.unmodifiableList(opptjeningAktivitet);
    }

    public Period getOpptjentPeriode() {
        return opptjentPeriode == null ? null : Period.parse(opptjentPeriode);
    }

    public LocalDate getTom() {
        return opptjeningPeriode.getTomDato();
    }

    public VilkårResultat getVilkårResultat() {
        return vilkårResultat;
    }

    /** fom/tom opptjening er gjort. */
    public DatoIntervallEntitet getOpptjeningPeriode() {
        return opptjeningPeriode;
    }

    public boolean erOpptjeningPeriodeVilkårOppfylt() {
        var opptjeningVilkårUtfall = getVilkårResultat().getVilkårene().stream()
            .filter(v -> VilkårType.OPPTJENINGSPERIODEVILKÅR.equals(v.getVilkårType()))
            .map(Vilkår::getGjeldendeVilkårUtfall)
            .findFirst().orElse(VilkårUtfallType.IKKE_VURDERT);
        return VilkårUtfallType.OPPFYLT.equals(opptjeningVilkårUtfall);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningPeriode);
    }

    public void setInaktiv() {
        if (aktiv) {
            this.aktiv = false;
        }
        // else - can never go back
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
                "id=" + id + ", "
            + "opptjeningsperiodeFom=" + opptjeningPeriode.getFomDato() + ", "
            + "opptjeningsperiodeTom=" + opptjeningPeriode.getTomDato() + ", "
                + (opptjentPeriode == null ? "" : ", opptjentPeriode=" + opptjentPeriode)
                + ">";
    }

    void setOpptjeningAktivitet(Collection<OpptjeningAktivitet> opptjeningAktivitet) {
        this.opptjeningAktivitet.clear();
        this.opptjeningAktivitet.addAll(opptjeningAktivitet);
    }

    void setOpptjentPeriode(Period opptjentPeriode) {
        this.opptjentPeriode = opptjentPeriode == null ? null : opptjentPeriode.toString();
    }

    void setVilkårResultat(VilkårResultat vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }
}
