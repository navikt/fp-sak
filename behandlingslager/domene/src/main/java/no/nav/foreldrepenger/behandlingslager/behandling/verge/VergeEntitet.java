package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "Verge")
@Table(name = "VERGE")
public class VergeEntitet extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VERGE")
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "bruker_id")
    NavBruker bruker;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "fomDato", column = @Column(name = "gyldig_fom")), @AttributeOverride(name = "tomDato", column = @Column(name = "gyldig_tom"))})
    DatoIntervallEntitet gyldigPeriode;

    @Convert(converter = VergeType.KodeverdiConverter.class)
    @Column(name = "verge_type", nullable = false)
    VergeType vergeType;


    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "organisasjon_id")
    VergeOrganisasjonEntitet vergeOrganisasjon;

    public VergeEntitet() {
    }

    // deep copy
    public VergeEntitet(VergeEntitet verge) {
        Objects.requireNonNull(verge, "verge");
        this.vergeType = verge.getVergeType();
        this.bruker = verge.getBruker();
        this.gyldigPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(verge.getGyldigFom(), verge.getGyldigTom());
        if (verge.getVergeOrganisasjon().isPresent()) {
            this.vergeOrganisasjon = new VergeOrganisasjonEntitet(verge.getVergeOrganisasjon().get(), this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (VergeEntitet) o;
        return Objects.equals(bruker, that.bruker) && Objects.equals(gyldigPeriode, that.gyldigPeriode) && Objects.equals(vergeType, that.vergeType)
            && Objects.equals(vergeOrganisasjon, that.vergeOrganisasjon);
    }

    @Override
    public int hashCode() {

        return Objects.hash(bruker, gyldigPeriode, vergeType);
    }

    public VergeType getVergeType() {
        return vergeType;
    }

    public LocalDate getGyldigFom() {
        return gyldigPeriode.getFomDato();
    }

    public LocalDate getGyldigTom() {
        return gyldigPeriode.getTomDato();
    }

    public NavBruker getBruker() {
        return bruker;
    }

    public Long getId() {
        return id;
    }

    public Optional<VergeOrganisasjonEntitet> getVergeOrganisasjon() {
        return Optional.ofNullable(vergeOrganisasjon);
    }
}
