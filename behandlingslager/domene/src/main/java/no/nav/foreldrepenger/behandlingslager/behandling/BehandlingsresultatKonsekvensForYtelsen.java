package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "BehandlingsresultatKonsekvensForYtelsen")
@Table(name = "BEHANDLING_RESULTAT_YT_KONSEK")
public class BehandlingsresultatKonsekvensForYtelsen extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_RESULTAT_YT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BEHANDLING_RESULTAT_ID", nullable = false, updatable = false, unique = true)
    private Behandlingsresultat behandlingsresultat;

    @Convert(converter = KonsekvensForYtelsen.KodeverdiConverter.class)
    @Column(name = "konsekvens_ytelse", nullable = false)
    private KonsekvensForYtelsen konsekvensForYtelsen;

    public Long getId() {
        return id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    KonsekvensForYtelsen getKonsekvensForYtelsen() {
        return konsekvensForYtelsen;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BehandlingsresultatKonsekvensForYtelsen that)) {
            return false;
        }
        return Objects.equals(behandlingsresultat, that.behandlingsresultat) &&
            Objects.equals(konsekvensForYtelsen, that.konsekvensForYtelsen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingsresultat, konsekvensForYtelsen);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BehandlingsresultatKonsekvensForYtelsen behandlingsresultatKonsekvensForYtelsen;

        public Builder() {
            behandlingsresultatKonsekvensForYtelsen = new BehandlingsresultatKonsekvensForYtelsen();
        }

        BehandlingsresultatKonsekvensForYtelsen.Builder medKonsekvensForYtelsen(KonsekvensForYtelsen konsekvens) {
            behandlingsresultatKonsekvensForYtelsen.konsekvensForYtelsen = konsekvens;
            return this;
        }

        public BehandlingsresultatKonsekvensForYtelsen build(Behandlingsresultat behandlingsresultat) {
            Objects.requireNonNull(behandlingsresultatKonsekvensForYtelsen.konsekvensForYtelsen, "Konsekvens kan ikke være null");
            behandlingsresultatKonsekvensForYtelsen.behandlingsresultat = behandlingsresultat;
            return behandlingsresultatKonsekvensForYtelsen;
        }
    }
}
