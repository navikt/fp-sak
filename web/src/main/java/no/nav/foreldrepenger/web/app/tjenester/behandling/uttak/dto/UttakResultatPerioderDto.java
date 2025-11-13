package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakType;

public record UttakResultatPerioderDto(@NotNull List<UttakResultatPeriodeDto> perioderSøker,
                                       @NotNull List<UttakResultatPeriodeDto> perioderAnnenpart,
                                       @NotNull FilterDto årsakFilter,
                                       @NotNull LocalDate endringsdato,
                                       @NotNull List<PeriodeResultatÅrsakDto> muligeÅrsaker) {

    public record FilterDto(@NotNull LocalDate kreverSammenhengendeUttakTom, @NotNull boolean utenMinsterett, @NotNull boolean søkerErMor) {}

    public record PeriodeResultatÅrsakDto(@NotNull String kode,
                                          @NotNull String sortering,
                                          @NotNull PeriodeResultatÅrsak.UtfallType utfallType,
                                          @NotNull Set<PeriodeResultatÅrsak.LovEndring> gyldigForLovendringer,
                                          @NotNull Set<UttakType> uttakTyper,
                                          @NotNull Set<UttakPeriodeType> valgbarForKonto,
                                          @NotNull Set<PeriodeResultatÅrsak.SynligFor> synligForRolle) {
        public PeriodeResultatÅrsakDto(PeriodeResultatÅrsak periodeResultatÅrsak) {
            this(periodeResultatÅrsak.getKode(),
                 periodeResultatÅrsak.getSortering(),
                 periodeResultatÅrsak.getUtfallType(),
                 periodeResultatÅrsak.getGyldigForLovendringer(),
                 periodeResultatÅrsak.getUttakTyper(),
                 periodeResultatÅrsak.getValgbarForKonto(),
                 periodeResultatÅrsak.getSynligForRolle());
        }
    }
}
