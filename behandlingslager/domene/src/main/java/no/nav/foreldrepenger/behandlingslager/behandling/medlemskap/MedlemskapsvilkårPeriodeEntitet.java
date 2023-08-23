package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "MedlemskapsvilkårPeriode")
@Table(name = "MEDLEMSKAP_VILKAR_PERIODE")
public class MedlemskapsvilkårPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_VILKAR_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "rot")
    @ChangeTracked
    private Set<MedlemskapsvilkårPerioderEntitet> perioder = new HashSet<>();

    @Embedded
    @AttributeOverride(name = "overstyringsdato", column = @Column(name = "overstyringsdato"))
    @AttributeOverride(name = "vilkårUtfall", column = @Column(name = "overstyrt_utfall"))
    @AttributeOverride(name = "avslagsårsak", column = @Column(name = "avslagsarsak"))
    @ChangeTracked
    private OverstyrtLøpendeMedlemskap overstyrtLøpendeMedlemskap = new OverstyrtLøpendeMedlemskap();


    MedlemskapsvilkårPeriodeEntitet() {
        // For Hibernate
    }

    private MedlemskapsvilkårPeriodeEntitet(MedlemskapsvilkårPeriodeEntitet kladd) {
        perioder = kladd.getPerioder().stream()
                .map(MedlemskapsvilkårPerioderEntitet::new)
                .peek(pr -> pr.setRot(this))
                .collect(Collectors.toSet());
        if (kladd.getOverstyring().getOverstyringsdato().isPresent()) {
            var overstyring = kladd.getOverstyring();
            overstyrtLøpendeMedlemskap = new OverstyrtLøpendeMedlemskap(overstyring.getOverstyringsdato().orElseThrow(),
                overstyring.getVilkårUtfall(), overstyring.getAvslagsårsak());
        }
    }


    public Set<MedlemskapsvilkårPerioderEntitet> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }


    public OverstyrtLøpendeMedlemskap getOverstyring() {
        return overstyrtLøpendeMedlemskap;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var that = (MedlemskapsvilkårPeriodeEntitet) o;
        return Objects.equals(perioder, that.perioder);
    }


    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }

    MedlemskapsvilkårPerioderEntitet.Builder getBuilderFor(LocalDate vurderingsdato) {
        var medlemOpt = perioder.stream()
                .filter(medlem -> vurderingsdato.equals(medlem.getVurderingsdato()))
                .findFirst();
        return MedlemskapsvilkårPerioderEntitet.Builder.oppdater(medlemOpt, vurderingsdato);
    }

    void leggTil(MedlemskapsvilkårPerioderEntitet entitet) {
        entitet.setRot(this);
        perioder.add(entitet);
    }

    void opprettOverstyringFor(LocalDate overstryingsdato, VilkårUtfallType utfall, Avslagsårsak avslagsårsak) {
        this.overstyrtLøpendeMedlemskap = new OverstyrtLøpendeMedlemskap(overstryingsdato, utfall, avslagsårsak);
    }

    public static class Builder {
        private MedlemskapsvilkårPeriodeEntitet kladd;

        private Builder() {
            this.kladd = new MedlemskapsvilkårPeriodeEntitet();
        }

        private Builder(MedlemskapsvilkårPeriodeEntitet kladd) {
            this.kladd = new MedlemskapsvilkårPeriodeEntitet(kladd);
        }

        private static MedlemskapsvilkårPeriodeEntitet.Builder oppdatere(MedlemskapsvilkårPeriodeEntitet aggregat) {
            return new MedlemskapsvilkårPeriodeEntitet.Builder(aggregat);
        }

        public static MedlemskapsvilkårPeriodeEntitet.Builder oppdatere(Optional<MedlemskapsvilkårPeriodeEntitet> aggregat) {
            return aggregat.map(MedlemskapsvilkårPeriodeEntitet.Builder::oppdatere).orElseGet(MedlemskapsvilkårPeriodeEntitet.Builder::new);
        }

        public MedlemskapsvilkårPeriodeEntitet.Builder leggTil(MedlemskapsvilkårPerioderEntitet.Builder builder) {
            if (!builder.erOppdatering()) {
                kladd.leggTil(builder.build());
            }
            return this;
        }

        public MedlemskapsvilkårPeriodeEntitet.Builder opprettOverstryingAvslag(LocalDate overstryingsdato, Avslagsårsak avslagsårsak) {
            kladd.opprettOverstyringFor(overstryingsdato, VilkårUtfallType.IKKE_OPPFYLT, avslagsårsak);
            return this;
        }

        public MedlemskapsvilkårPeriodeEntitet.Builder opprettOverstryingOppfylt(LocalDate overstryingsdato) {
            kladd.opprettOverstyringFor(overstryingsdato, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);
            return this;
        }

        public MedlemskapsvilkårPerioderEntitet.Builder getBuilderForVurderingsdato(LocalDate vurderingsdato) {
            return kladd.getBuilderFor(vurderingsdato);
        }

        public MedlemskapsvilkårPeriodeEntitet build() {
            return kladd;
        }
    }
}
