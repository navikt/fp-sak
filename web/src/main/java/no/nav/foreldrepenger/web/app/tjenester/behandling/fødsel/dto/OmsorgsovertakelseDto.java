package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public record OmsorgsovertakelseDto(@NotNull Omsorgsovertakelse søknad,
                                    @NotNull Register register,
                                    @NotNull Kilde kildeGjeldende,
                                    @NotNull Omsorgsovertakelse gjeldende,
                                    SaksbehandlerVurdering saksbehandlerVurdering,
                                    @NotNull Map<OmsorgsovertakelseVilkårType, List<Avslagsårsak>> aktuelleDelvilkårAvslagsårsaker) {


    public record SaksbehandlerVurdering(@NotNull VilkårUtfallType vilkårUtfallType, Avslagsårsak avslagsårsak) {
    }

    public record BarnHendelseData(@NotNull LocalDate fødselsdato, LocalDate dødsdato, @NotNull Integer barnNummer) {
    }

    public record Omsorgsovertakelse(@NotNull List<BarnHendelseData> barn, LocalDate omsorgsovertakelseDato, @NotNull int antallBarn,
                                     OmsorgsovertakelseVilkårType delvilkår, Boolean erEktefellesBarn,LocalDate ankomstNorgeDato) {
    }

    public record Register(@NotNull List<BarnHendelseData> barn) {
    }
}
