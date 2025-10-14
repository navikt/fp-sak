package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public record VilkårDto(@NotNull VilkårType vilkarType, String lovReferanse, @NotNull List<Avslagsårsak> aktuelleAvslagsårsaker,
                        @NotNull VilkårUtfallType vilkarStatus, String avslagKode, @NotNull Boolean overstyrbar) {
}
