package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public record OmsorgsovertakelseDto(@NotNull Søknad søknad, @NotNull Register register, @NotNull Gjeldende gjeldende,
                                    SaksbehandlerVurdering saksbehandlerVurdering,
                                    List<OmsorgsovertakelseVilkårType> aktuelleDelvilkår,
                                    Map<OmsorgsovertakelseVilkårType, List<Avslagsårsak>> aktuelleDelvilkårAvslagsårsaker) {


    public record SaksbehandlerVurdering(@NotNull VilkårUtfallType vilkårUtfallType, Avslagsårsak avslagsårsak) {
    }

    public record BarnHendelseData(@NotNull LocalDate fødselsdato, LocalDate dødsdato, Integer barnNummer) {
    }

    public record Søknad(@NotNull List<BarnHendelseData> barn, LocalDate omsorgsovertakelseDato, Boolean erEktefellesBarn, LocalDate ankomstNorgeDato,
                         @NotNull int antallBarn, OmsorgsovertakelseVilkårType delvilkår) {
    }

    public record Register(@NotNull List<BarnHendelseData> barn) {
    }

    public record Gjeldende(@NotNull Omsorgsovertakelse omsorgsovertakelse, @NotNull AntallBarn antallBarn, @NotNull List<GjeldendeBarn> barn) {

        public record Omsorgsovertakelse(@NotNull Kilde kilde, LocalDate omsorgsovertakelseDato,
                                         OmsorgsovertakelseVilkårType delvilkår, Boolean ektefellesBarn, LocalDate ankomstNorgeDato) {
        }

        public record AntallBarn(@NotNull Kilde kilde, @NotNull int antall) {
        }

        public record GjeldendeBarn(@NotNull Kilde kilde, @NotNull BarnHendelseData barn, @NotNull boolean kanOverstyres) {
        }

    }
}
