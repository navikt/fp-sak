package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

@Entity(name = "Behandlingsresultat")
@Table(name = "BEHANDLING_RESULTAT")
public class Behandlingsresultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_RESULTAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne
    @JoinColumn(name = "inngangsvilkar_resultat_id"
    /* , updatable = false // får ikke satt denne til false, men skal aldri kunne endres dersom satt tidligere */
    /* , nullable=false // kan være null, men når den er satt kan ikke oppdateres */
    )
    private VilkårResultat vilkårResultat;

    @ManyToOne
    @JoinColumn(name = "beregning_resultat_id"
    /* , updatable = false // får ikke satt denne til false, men skal aldri kunne endres dersom satt tidligere */
    /* , nullable=false // kan være null, men når den er satt kan ikke oppdateres */
    )
    private LegacyESBeregningsresultat beregningResultat;

    /* bruker @ManyToOne siden JPA ikke støtter OneToOne join på non-PK column. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false)
    private Behandling behandling;

    @Convert(converter = BehandlingResultatType.KodeverdiConverter.class)
    @Column(name = "behandling_resultat_type", nullable = false)
    private BehandlingResultatType behandlingResultatType = BehandlingResultatType.IKKE_FASTSATT;

    @Convert(converter = Avslagsårsak.KodeverdiConverter.class)
    @Column(name = "avslag_arsak", nullable = false)
    private Avslagsårsak avslagsårsak = Avslagsårsak.UDEFINERT;

    @Convert(converter = RettenTil.KodeverdiConverter.class)
    @Column(name = "retten_til", nullable = false)
    private RettenTil rettenTil = RettenTil.UDEFINERT;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "behandlingsresultat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BehandlingsresultatKonsekvensForYtelsen> konsekvenserForYtelsen = new ArrayList<>();

    @Convert(converter = Vedtaksbrev.KodeverdiConverter.class)
    @Column(name = "vedtaksbrev", nullable = false)
    private Vedtaksbrev vedtaksbrev = Vedtaksbrev.UDEFINERT;

    protected Behandlingsresultat() {
        // for hibernate
    }

    public VilkårResultat medOppdatertVilkårResultat(VilkårResultat nyttResultat) {
        if (nyttResultat != null && vilkårResultat != null && nyttResultat != vilkårResultat) {
            if (!nyttResultat.erLik(vilkårResultat)) {
                this.vilkårResultat = nyttResultat;
            }
        } else {
            this.vilkårResultat = nyttResultat;
        }
        return this.vilkårResultat;
    }

    /**
     * @deprecated Lagre separat, ikke gjennom referanse her
     */
    @Deprecated
    public void medOppdatertBeregningResultat(LegacyESBeregningsresultat nyttResultat) {
        this.beregningResultat = nyttResultat;
    }

    public Long getId() {
        return id;
    }

    /**
     * @deprecated Ikke hent behandling herfra - bruk {@link #getBehandlingId()}
     */
    @Deprecated
    public Behandling getBehandling() {
        return behandling;
    }

    public Long getBehandlingId() {
        return behandling.getId();
    }

    public VilkårResultat getVilkårResultat() {
        return vilkårResultat;
    }

    /**
     * @deprecated Hent {@link LegacyESBeregningsresultat} selv, ikke gjennom ref her fra
     */
    @Deprecated
    public LegacyESBeregningsresultat getBeregningResultat() {
        return beregningResultat;
    }

    /**
     * NB: ikke eksponer settere fra modellen. Skal ha package-scope.
     */
    void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    public BehandlingResultatType getBehandlingResultatType() {
        return behandlingResultatType;
    }

    public Avslagsårsak getAvslagsårsak() {
        return Objects.equals(avslagsårsak, Avslagsårsak.UDEFINERT) ? null : avslagsårsak;
    }

    public void setAvslagsårsak(Avslagsårsak avslagsårsak) {
        this.avslagsårsak = Optional.ofNullable(avslagsårsak).orElse(Avslagsårsak.UDEFINERT);
    }

    public RettenTil getRettenTil() {
        return rettenTil;
    }

    public List<KonsekvensForYtelsen> getKonsekvenserForYtelsen() {
        return konsekvenserForYtelsen.stream().map(BehandlingsresultatKonsekvensForYtelsen::getKonsekvensForYtelsen).toList();
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<>";
    }

    public static Behandlingsresultat opprettFor(Behandling behandling) {
        return builder().buildFor(behandling);
    }

    public static Builder builderForInngangsvilkår() {
        return new Builder(VilkårResultat.builder());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builderForBeregningResultat() {
        return new Builder(LegacyESBeregningsresultat.builder());
    }

    public static Builder builderFraEksisterende(Behandlingsresultat behandlingsresultat) {
        return new Builder(behandlingsresultat, false);
    }

    public static Builder builderEndreEksisterende(Behandlingsresultat behandlingsresultat) {
        return new Builder(behandlingsresultat, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Behandlingsresultat that)) {
            return false;
        }
        // Behandlingsresultat skal p.t. kun eksisterere dersom parent Behandling allerede er persistert.
        // Det syntaktisk korrekte vil derfor være at subaggregat Behandlingsresultat med 1:1-forhold til parent
        // Behandling har også sin id knyttet opp mot Behandling alene.
        return getBehandlingId().equals(that.getBehandlingId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandlingId());
    }

    public static class Builder {

        private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        private LegacyESBeregningsresultat.Builder beregningResultatBuilder;
        private VilkårResultat.Builder vilkårResultatBuilder;
        private boolean built;

        Builder(VilkårResultat.Builder builder) {
            this.vilkårResultatBuilder = builder;
        }

        Builder(LegacyESBeregningsresultat.Builder builder) {
            this.beregningResultatBuilder = builder;
        }

        Builder(Behandlingsresultat gammeltResultat, boolean endreEksisterende) {
            if (endreEksisterende) {
                behandlingsresultat = gammeltResultat;
            }
            if (gammeltResultat != null && gammeltResultat.getVilkårResultat() != null) {
                this.vilkårResultatBuilder = VilkårResultat
                    .builderFraEksisterende(gammeltResultat.getVilkårResultat());
            }
            if (gammeltResultat != null && gammeltResultat.getBeregningResultat() != null) {
                this.beregningResultatBuilder = LegacyESBeregningsresultat
                    .builderFraEksisterende(gammeltResultat.getBeregningResultat());
            }
        }

        public Builder() {
            // empty builder
        }

        private void validerKanModifisere() {
            if (built)
                throw new IllegalStateException("Kan ikke bygge to ganger med samme builder");
        }

        public Builder medBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
            validerKanModifisere();
            this.behandlingsresultat.behandlingResultatType = behandlingResultatType;
            return this;
        }

        public Builder medRettenTil(RettenTil rettenTil) {
            validerKanModifisere();
            this.behandlingsresultat.rettenTil = rettenTil;
            return this;
        }

        public Builder leggTilKonsekvensForYtelsen(KonsekvensForYtelsen konsekvensForYtelsen) {
            validerKanModifisere();
            var behandlingsresultatKonsekvensForYtelsen = BehandlingsresultatKonsekvensForYtelsen.builder()
                .medKonsekvensForYtelsen(konsekvensForYtelsen).build(behandlingsresultat);
            this.behandlingsresultat.konsekvenserForYtelsen.add(behandlingsresultatKonsekvensForYtelsen);
            return this;
        }

        public Builder fjernKonsekvenserForYtelsen() {
            validerKanModifisere();
            this.behandlingsresultat.konsekvenserForYtelsen.clear();
            return this;
        }

        public Builder medVedtaksbrev(Vedtaksbrev vedtaksbrev) {
            validerKanModifisere();
            this.behandlingsresultat.vedtaksbrev = vedtaksbrev;
            return this;
        }

        public Builder medAvslagsårsak(Avslagsårsak avslagsårsak) {
            validerKanModifisere();
            this.behandlingsresultat.avslagsårsak = Optional.ofNullable(avslagsårsak).orElse(Avslagsårsak.UDEFINERT);
            return this;
        }

        public Behandlingsresultat build() {
            if (vilkårResultatBuilder != null) {
                var vilkårResultat = vilkårResultatBuilder.buildFor(behandlingsresultat);
                behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
            }
            if (beregningResultatBuilder != null) {
                throw new IllegalStateException("Kan ikke lagre LegacyESBeregningsresultat gjennom denne - håndter separat");
            }
            built = true;
            return behandlingsresultat;
        }

        /**
         * @deprecated bruk #build() og lagre gjennom BehandlingsresultatRepository i stedet
         */
        @Deprecated
        public Behandlingsresultat buildFor(Behandling behandling) {
            behandling.setBehandlingresultat(behandlingsresultat);
            if (vilkårResultatBuilder != null) {
                var vilkårResultat = vilkårResultatBuilder.buildFor(behandlingsresultat);
                behandlingsresultat.medOppdatertVilkårResultat(vilkårResultat);
            }
            if (beregningResultatBuilder != null) {
                var beregningResultat = beregningResultatBuilder.buildFor(behandling, behandlingsresultat);
                behandlingsresultat.medOppdatertBeregningResultat(beregningResultat);
            }
            built = true;
            return behandlingsresultat;
        }
    }

    public boolean isBehandlingHenlagt() {
        return BehandlingResultatType.getAlleHenleggelseskoder().contains(behandlingResultatType);
    }

    public boolean isBehandlingsresultatAvslåttOrOpphørt() {
        return BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType)
            || BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatAvslått() {
        return BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatOpphørt() {
        return BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    public boolean isBehandlingsresultatIkkeEndret() {
        return BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType);
    }

    public boolean isVilkårAvslått() {
        return vilkårResultat.getVilkårene().stream().map(Vilkår::getGjeldendeVilkårUtfall).anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals);
    }

    public boolean isInngangsVilkårAvslått() {
        return vilkårResultat.getVilkårene().stream()
            .filter(v -> v.getVilkårType().erInngangsvilkår())
            .map(Vilkår::getGjeldendeVilkårUtfall)
            .anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals);
    }
}
