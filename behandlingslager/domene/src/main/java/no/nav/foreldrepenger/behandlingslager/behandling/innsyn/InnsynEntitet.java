package no.nav.foreldrepenger.behandlingslager.behandling.innsyn;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "Innsyn")
@Table(name = "INNSYN")
public class InnsynEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNSYN")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "mottatt_dato", nullable = false)
    private LocalDate mottattDato;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = InnsynResultatType.KodeverdiConverter.class)
    @Column(name = "innsyn_resultat_type", nullable = false)
    private InnsynResultatType innsynResultatType = InnsynResultatType.UDEFINERT;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true, mappedBy = "innsyn")
    private Set<InnsynDokumentEntitet> innsynDokumenter = new HashSet<>(1);

    protected InnsynEntitet() {
        // for hibernate
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public InnsynResultatType getInnsynResultatType() {
        return innsynResultatType;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    /**
     * @deprecated FIXME: Bør ikke returnere mutable collection her
     */
    @Deprecated
    public Set<InnsynDokumentEntitet> getInnsynDokumenterOld() {
        return innsynDokumenter;
    }

    public Set<InnsynDokumentEntitet> getInnsynDokumenter() {
        return Collections.unmodifiableSet(innsynDokumenter);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public String toString() {
        return "InnsynEntitet{" + "id=" + id + ", innsynResultatType=" + innsynResultatType + ", innsynDokumenter=" + innsynDokumenter + '}';
    }

    public static class InnsynBuilder {

        private InnsynEntitet kladd;

        InnsynBuilder(InnsynEntitet innsyn) {
            this.kladd = innsyn;
        }

        public static InnsynBuilder builder() {
            return new InnsynBuilder(new InnsynEntitet());
        }

        public static InnsynBuilder builder(InnsynEntitet update) {
            return new InnsynBuilder(Objects.requireNonNullElseGet(update, InnsynEntitet::new));
        }

        public InnsynBuilder medMottattDato(LocalDate mottattDato) {
            this.kladd.mottattDato = mottattDato;
            return this;
        }

        public InnsynBuilder medInnsynResultatType(InnsynResultatType innsynResultatType) {
            this.kladd.innsynResultatType = innsynResultatType;
            return this;
        }

        public InnsynBuilder medBegrunnelse(String begrunnelse) {
            this.kladd.begrunnelse = begrunnelse;
            return this;
        }

        public InnsynBuilder medBehandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public InnsynEntitet build() {
            verifyStateForBuild();
            var r = kladd;
            kladd = null; // destroy builder
            return r;
        }

        public InnsynBuilder medInnsynDokument(InnsynDokumentEntitet dok) {
            kladd.innsynDokumenter.add(dok);
            return this;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.getMottattDato(), "mottatt dato må være satt");
            Objects.requireNonNull(kladd.getInnsynResultatType(), "innsynresultat må være satt");
            Objects.requireNonNull(kladd.getBegrunnelse(), "begrunnelse må være satt");
            Objects.requireNonNull(kladd.getBehandlingId(), "BehandlingId må være satt");
        }
    }

}
