package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Embeddable
public class OverstyrtLøpendeMedlemskap {

    @ChangeTracked
    @Column(name = "overstyringsdato")
    private LocalDate overstyringsdato;

    @Convert(converter = VilkårUtfallType.KodeverdiConverter.class)
    @Column(name="overstyrt_utfall", nullable = false)
    private VilkårUtfallType vilkårUtfall = VilkårUtfallType.UDEFINERT;

    @ChangeTracked
    @Convert(converter = Avslagsårsak.KodeverdiConverter.class)
    @Column(name="avslagsarsak", nullable = false)
    private Avslagsårsak avslagsårsak = Avslagsårsak.UDEFINERT;

    OverstyrtLøpendeMedlemskap() {
        //hibernate
    }

    public OverstyrtLøpendeMedlemskap(LocalDate overstyringsdato, VilkårUtfallType vilkårUtfall, Avslagsårsak avslagsårsak) {
        this.overstyringsdato = overstyringsdato;
        this.vilkårUtfall = vilkårUtfall;
        this.avslagsårsak = avslagsårsak == null ? Avslagsårsak.UDEFINERT : avslagsårsak;
    }

    public Optional<LocalDate> getOverstyringsdato() {
        return Optional.ofNullable(overstyringsdato);
    }

    public VilkårUtfallType getVilkårUtfall() {
        return vilkårUtfall;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
