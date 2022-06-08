package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "VilkarResultat")
@Table(name = "VILKAR_RESULTAT")
public class VilkårResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR_RESULTAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = VilkårResultatType.KodeverdiConverter.class)
    @Column(name="vilkar_resultat", nullable = false)
    private VilkårResultatType vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;

    // CascadeType.ALL + orphanRemoval=true må til for at Vilkår skal bli slettet fra databasen ved fjerning fra HashSet
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "vilkårResultat")
    private Set<Vilkår> vilkårne = new LinkedHashSet<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "original_behandling_id", updatable = false
    /* , nullable=false // får ikke satt false pga binary relasjon mellom Behandling og VilkårResultat */
    )
    private Behandling originalBehandling;

    /**
     * Hvorvidt hele vilkårresultatet er overstyrt av Saksbehandler. (fra SF3).
     */
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "overstyrt", nullable = false)
    private boolean erOverstyrt = false;

    private VilkårResultat() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public VilkårResultatType getVilkårResultatType() {
        return Objects.equals(VilkårResultatType.UDEFINERT, vilkårResultatType) ? null : vilkårResultatType;
    }

    void setVilkårResultatType(VilkårResultatType vilkårResultatType) {
        this.vilkårResultatType = vilkårResultatType == null ? VilkårResultatType.UDEFINERT : vilkårResultatType;
    }

    /**
     * Returnerer kopi liste av vilkår slik at denne ikke kan modifiseres direkte av klåfingrede utviklere.
     */
    public List<Vilkår> getVilkårene() {
        return Collections.unmodifiableList(new ArrayList<>(vilkårne));
    }

    public boolean erOverstyrt() {
        return erOverstyrt;
    }

    void setVilkårene(Set<Vilkår> nyeVilkår) {
        this.vilkårne.clear();
        nyeVilkår.forEach(v -> v.setVilkårResultat(this));
        this.vilkårne.addAll(nyeVilkår);
    }

    void setOriginalBehandling(Behandling originalBehandling) {
        this.originalBehandling = originalBehandling;
    }

    Behandlingsresultat getOriginalBehandlingsresultat() {
        return originalBehandling == null ? null : originalBehandling.getBehandlingsresultat();
    }

    /**
     * Original behandling der denne instansen av {@link VilkårResultat} resultat først ble behandlet. Senere
     * endringer som ikke påvirker innngangsvilkårresutlate vil bare gjenbruke denne i et {@link Behandlingsresultat}.
     */
    public Behandling getOriginalBehandling() {
        return originalBehandling;
    }

    /**
     * Original behandling der denne instansen av {@link VilkårResultat} resultat først ble behandlet. Senere
     * endringer som ikke påvirker innngangsvilkårresutlate vil bare gjenbruke denne i et {@link Behandlingsresultat}.
     */
    public Long getOriginalBehandlingId() {
        return originalBehandling != null ? originalBehandling.getId() : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id=" + id +
            ", versjon=" + versjon +
            ", vilkårResultatType=" + getVilkårResultatType() +
            ", vilkårne=" + vilkårne +
            ", originalBehandling=" + originalBehandling +
            ", vilkår={" + vilkårne.stream().map(Vilkår::toString).collect(joining("},{")) + "}" +
            '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VilkårResultat)) {
            return false;
        }
        var other = (VilkårResultat) obj;
        return Objects.equals(getVilkårResultatType(), other.getVilkårResultatType()) &&
            Objects.equals(vilkårne, other.vilkårne);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVilkårResultatType(), getVilkårene());
    }

    public static Builder builder() {
        return Builder.ny();
    }

    public static Builder builderFraEksisterende(VilkårResultat eksisterendeResultat) {
        return Builder.oppdatere(eksisterendeResultat);
    }

    public boolean erLik(VilkårResultat annen) {
        // equals for collections out-of-order
        // diff her, baserer oss ikke på equals da den matcher kun på vilkårtype (som den skal)

        // det gir en deep equals av vilkårene inklusiv alle felter som er mappet til tuple list.
        var vilkårThis = toTuples(this.vilkårne);
        var vilkårAnnen = toTuples(annen.vilkårne);
        return vilkårThis.equals(vilkårAnnen);
    }

    private static Map<VilkårType, ?> toTuples(Set<Vilkår> vilkårene) {
        // Mapperut hvert vilkår til en liste av verdier for senere sammenligning (tuples) slik at en hver endring
        // vil slå ut som forskjell.
        return vilkårene.stream()
            .map(v -> new SimpleEntry<>(v.getVilkårType(), v.tuples()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Set<VilkårUtfallType> hentAlleGjeldendeVilkårsutfall() {
        return vilkårne.stream()
            .map(Vilkår::getGjeldendeVilkårUtfall)
            .collect(toSet());
    }

    /**
     * Builder for å modifisere et vilkårResultat.
     */
    public static class Builder {

        private static final Logger LOG = LoggerFactory.getLogger(VilkårResultat.Builder.class);
        private static final boolean PROD = Environment.current().isProd();
        private static String MISSING_VILKÅR_ARGS = "Mangler vilkårtype, utfall eller årsak";

        private Map<VilkårType, Vilkår> vilkårene = new EnumMap<>(VilkårType.class);

        private VilkårResultat resultatKladd = new VilkårResultat();
        private VilkårResultat eksisterendeResultat;
        private VilkårResultatType vilkårResultatType;
        private boolean modifisert;
        private boolean built;

        private Builder() {
        }

        private Builder(VilkårResultat vilkårResultat) {
            this.eksisterendeResultat = vilkårResultat;
            if (vilkårResultat != null) {
                vilkårResultat.getVilkårene().forEach(v -> this.vilkårene.put(v.getVilkårType(), v));
            }
        }

        static Builder ny() {
            return new Builder();
        }

        static Builder oppdatere(VilkårResultat vilkårResultat) {
            return new Builder(vilkårResultat);
        }

        private void validerKanModifisere() {
            if(built) throw new IllegalStateException("Kan ikke bygge to ganger med samme builder");
        }

        public VilkårBuilder getVilkårBuilderFor(VilkårType vilkårType) {
            return getBuilderFor(vilkårType);
        }

        private VilkårBuilder getBuilderFor(VilkårType vilkårType) {
            var eksisterende = Optional.ofNullable(vilkårene.get(vilkårType));
            return VilkårBuilder.oppdatere(eksisterende).medVilkårType(vilkårType);
        }

        public Builder leggTilVilkår(VilkårBuilder vilkårBuilder) {
            var vilkår = vilkårBuilder.build();
            vilkårene.put(vilkår.getVilkårType(), vilkår);
            modifisert = true;
            return this;
        }

        public Builder leggTilVilkårIkkeVurdert(VilkårType vilkårType) {
            return leggTilVilkår(vilkårType, VilkårUtfallType.IKKE_VURDERT, VilkårUtfallMerknad.UDEFINERT);
        }

        public Builder leggTilVilkårOppfylt(VilkårType vilkårType) {
            return leggTilVilkår(vilkårType, VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.UDEFINERT);
        }

        public Builder leggTilVilkårAvslått(VilkårType vilkårType, VilkårUtfallMerknad merknad) {
            return leggTilVilkår(vilkårType, VilkårUtfallType.IKKE_OPPFYLT, merknad);
        }

        public Builder leggTilVilkår(VilkårType vilkårType, VilkårUtfallType utfallType, VilkårUtfallMerknad merknad) {
            if (vilkårType == null || utfallType == null || merknad == null) throw new IllegalArgumentException(MISSING_VILKÅR_ARGS);
            if (VilkårUtfallType.IKKE_OPPFYLT.equals(utfallType) && VilkårUtfallMerknad.UDEFINERT.equals(merknad))
                throw new IllegalArgumentException(MISSING_VILKÅR_ARGS);
            var builder = getBuilderFor(vilkårType)
                .medVilkårUtfall(utfallType, merknad);
            vilkårene.put(vilkårType, builder.build());
            modifisert = true;
            return this;
        }

        public Builder manueltVilkår(VilkårType vilkårType, VilkårUtfallType utfallType, Avslagsårsak avslagsårsak) {
            if (vilkårType == null || utfallType == null || avslagsårsak == null) throw new IllegalArgumentException(MISSING_VILKÅR_ARGS);
            if (VilkårUtfallType.IKKE_OPPFYLT.equals(utfallType) && Avslagsårsak.UDEFINERT.equals(avslagsårsak))
                throw new IllegalArgumentException(MISSING_VILKÅR_ARGS);
            var builder = getBuilderFor(vilkårType)
                .medUtfallManuell(utfallType, avslagsårsak);
            vilkårene.put(vilkårType, builder.build());
            modifisert = true;
            return this;
        }

        public Builder overstyrVilkår(VilkårType vilkårType, VilkårUtfallType utfallType, Avslagsårsak avslagsårsak) {
            if (vilkårType == null || utfallType == null || avslagsårsak == null) throw new IllegalArgumentException(MISSING_VILKÅR_ARGS);
            if (VilkårUtfallType.IKKE_OPPFYLT.equals(utfallType) && Avslagsårsak.UDEFINERT.equals(avslagsårsak))
                throw new IllegalArgumentException(MISSING_VILKÅR_ARGS);
            var builder = getBuilderFor(vilkårType)
                .medUtfallOverstyrt(utfallType, avslagsårsak);
            vilkårene.put(vilkårType, builder.build());
            modifisert = true;
            return this;
        }

        public Builder nullstillVilkår(Vilkår vilkår, boolean nullstillManuelt) {
            if (VilkårUtfallType.erFastsatt(vilkår.getVilkårUtfallOverstyrt())) {
                throw new IllegalStateException("Utviklerfeil - vilkåret er overstyrt");
            }
            if (!nullstillManuelt && VilkårUtfallType.erFastsatt(vilkår.getVilkårUtfallManuelt())) {
                LOG.info("VILKÅR: Nullstiller ikke vilkår {} som er manuelt vurdert {}", vilkår.getVilkårType(), vilkår.getVilkårUtfallManuelt());
            }
            var builder = getBuilderFor(vilkår.getVilkårType())
                .medVilkårUtfall(VilkårUtfallType.IKKE_VURDERT, VilkårUtfallMerknad.UDEFINERT);
            if (nullstillManuelt) {
                builder.medUtfallManuell(VilkårUtfallType.UDEFINERT, Avslagsårsak.UDEFINERT);
            }
            vilkårene.put(vilkår.getVilkårType(), builder.build());
            modifisert = true;
            return this;
        }

        public Builder fjernVilkår(VilkårType vilkårType) {
            validerKanModifisere();
            vilkårene.remove(vilkårType);
            modifisert = true;
            return this;
        }

        public Builder medVilkårResultatType(VilkårResultatType vilkårResultatType) {
            validerKanModifisere();
            Objects.requireNonNull(vilkårResultatType, "vilkårResultatType");
            this.vilkårResultatType = vilkårResultatType;
            modifisert = true;
            return this;
        }

        public Builder kopierVilkårFraAnnenBehandling(Vilkår vilkår, boolean settTilIkkeVurdert, boolean nullstillManuellVurdering) {
            var skalKopiereManuellVurdering = !nullstillManuellVurdering && !vilkår.erOverstyrt() && vilkår.erManueltVurdert();
            var builder = VilkårBuilder.ny()
                .medVilkårType(vilkår.getVilkårType())
                .medUtfallOverstyrt(vilkår.getVilkårUtfallOverstyrt(), vilkår.getAvslagsårsak())
                .medUtfallManuell(skalKopiereManuellVurdering ? vilkår.getVilkårUtfallManuelt() : VilkårUtfallType.UDEFINERT,
                    skalKopiereManuellVurdering ? vilkår.getAvslagsårsak() : Avslagsårsak.UDEFINERT)
                .medMerknadParametere(vilkår.getMerknadParametere())
                .medRegelEvaluering(vilkår.getRegelEvaluering())
                .medRegelInput(vilkår.getRegelInput());
            if (settTilIkkeVurdert) {
                builder.medVilkårUtfall(VilkårUtfallType.IKKE_VURDERT, VilkårUtfallMerknad.UDEFINERT);
            } else {
                builder.medVilkårUtfall(vilkår.getVilkårUtfall(), vilkår.getVilkårUtfallMerknad());
            }
            vilkårene.put(vilkår.getVilkårType(), builder.build());
            modifisert = true;
            return this;
        }

        /**
         * Bygg nytt resultat for angitt behandlingsresultat.
         *
         * @return Returner nytt resultat HVIS det opprettes.
         */
        public VilkårResultat buildFor(Behandlingsresultat behandlingsresultat) {
            if (eksisterendeResultat != null
                && Objects.equals(behandlingsresultat.getId(), eksisterendeResultat.getOriginalBehandlingsresultat().getId())) {
                // samme behandling som originalt, oppdaterer original
                oppdaterVilkår(eksisterendeResultat);
                built = true;
                return eksisterendeResultat; // samme som før
            }
            if (eksisterendeResultat != null && !modifisert) {
                built = true;
                return eksisterendeResultat;
            }
            oppdaterVilkår(resultatKladd);
            resultatKladd.setOriginalBehandling(behandlingsresultat.getBehandling());
            behandlingsresultat.medOppdatertVilkårResultat(resultatKladd);
            built = true;
            return resultatKladd;
        }

        public VilkårResultat buildFor(Behandling behandling) {
            // Må opprette Behandlingsresultat på Behandling hvis det ikke finnes, før man bygger VilkårResultat
            var behandlingsresultat = behandling.getBehandlingsresultat();
            if (behandlingsresultat == null) {
                behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
            }
            return buildFor(behandlingsresultat);
        }

        /** OBS: Testbruk. Returnerer alltid nytt vilkårresultat. */
        public VilkårResultat build() {
            oppdaterVilkår(resultatKladd);
            built = true;
            return resultatKladd;
        }

        private void oppdaterVilkår(VilkårResultat resultat) {
            validerKanModifisere();
            var vilkårSet = new HashSet<>(vilkårene.values());

            resultat.setVilkårene(vilkårSet);

            if (vilkårSet.stream().anyMatch(Vilkår::erOverstyrt)) {
                resultat.erOverstyrt = true;
            }
            if (vilkårResultatType != null) {
                resultat.setVilkårResultatType(vilkårResultatType);
                var utledetVilkårResultat = VilkårResultatType.utledInngangsvilkårUtfall(resultat.hentAlleGjeldendeVilkårsutfall(), false);
                if (PROD && !resultat.getVilkårResultatType().equals(utledetVilkårResultat)) {
                    // TODO: Vurder exception i prod
                    LOG.info("Vilkårbuilder: Mismatch mellom satt {} og utledet vilkårsresultattype {}", resultat.getVilkårResultatType(), utledetVilkårResultat);
                }
            }
        }

    }

    public Optional<Vilkår> hentIkkeOppfyltVilkår() {
        return vilkårne.stream().filter(v -> VilkårUtfallType.IKKE_OPPFYLT.equals(v.getGjeldendeVilkårUtfall())).findFirst();
    }
}
