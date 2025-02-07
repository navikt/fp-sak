package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

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
    private NavBruker bruker;

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "gyldig_fom"))
    @AttributeOverride(name = "tomDato", column = @Column(name = "gyldig_tom"))
    private DatoIntervallEntitet gyldigPeriode;

    @Convert(converter = VergeType.KodeverdiConverter.class)
    @Column(name = "verge_type", nullable = false)
    private VergeType vergeType;


    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "organisasjon_id")
    private VergeOrganisasjonEntitet vergeOrganisasjon;

    public VergeEntitet() {
    }

    // deep copy
    public VergeEntitet(VergeEntitet verge) {
        Objects.requireNonNull(verge, "verge");
        this.vergeType = verge.getVergeType();
        this.bruker = verge.getBruker().orElse(null);
        this.gyldigPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(verge.getGyldigFom(), verge.getGyldigTom());
        this.vergeOrganisasjon = verge.getVergeOrganisasjon().map(vo -> new VergeOrganisasjonEntitet(vo, this)).orElse(null);
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

    public Optional<NavBruker> getBruker() {
        return Optional.ofNullable(bruker);
    }

    public Long getId() {
        return id;
    }

    public Optional<VergeOrganisasjonEntitet> getVergeOrganisasjon() {
        return Optional.ofNullable(vergeOrganisasjon);
    }

    public static class Builder {
        private final VergeEntitet kladd;

        public Builder() {
            kladd = new VergeEntitet();
        }

        public Builder gyldigPeriode(LocalDate gyldigFom, LocalDate gyldigTom) {
            if (gyldigTom != null) {
                kladd.gyldigPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(gyldigFom, gyldigTom);
            } else {
                kladd.gyldigPeriode = DatoIntervallEntitet.fraOgMed(gyldigFom);
            }
            return this;
        }

        public Builder medVergeType(VergeType vergeType) {
            kladd.vergeType = vergeType;
            return this;
        }

        public Builder medBruker(NavBruker bruker) {
            kladd.bruker = bruker;
            return this;
        }

        public Builder medVergeOrganisasjon(VergeOrganisasjonEntitet vergeOrganisasjon) {
            vergeOrganisasjon.setVerge(kladd);
            kladd.vergeOrganisasjon = vergeOrganisasjon;
            return this;
        }

        public VergeEntitet build() {
            //verifiser oppbyggingen til objektet
            Objects.requireNonNull(kladd.vergeType, "vergeType");
            return kladd;
        }
    }
}
