package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

/** NAV 14-05.07 – Søknad om engangsstønad ved fødsel */
public record Nav140507Data(
    @NotNull String fornavnSoker,
    @NotNull String etternavnSoker,
    @NotNull String fodselsnummerDNummerSoker,
    @NotNull HvaSokerDuOm hvaSokerDuOm,
    @NotNull @Min(1) @Max(9) Integer antallBarn,
    JaNei erBarnetFodt,
    LocalDate fodselsdatoDdMmAaaa,
    JaNei erBarnaFodt,
    LocalDate fodselsdatoDdMmAaaa1,
    LocalDate termindatoDdMmAaaa,
    LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa,
    LocalDate datoForOmsorgsovertakelsenAvBarnaDdMmAaaa,
    @NotNull JaNei planleggerDuAVaereINorgePaFodselstidspunktet1,
    @NotNull HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene,
    List<@Valid UtenlandsoppholdRow> utenlandsopphold,
    @NotNull HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene,
    List<@Valid Utenlandsopphold1Row> utenlandsopphold1
) {

    public enum HvaSokerDuOm {
        @JsonProperty("engangsstonadVedFodsel")
        ENGANGSSTONAD_VED_FODSEL,
        @JsonProperty("engangsstonadVedOvertakelseAvForeldreansvaretEllerOmsorgen")
        ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN;
    }

    public record UtenlandsoppholdRow(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @Valid @NotNull Landvalg hvilketLandSkalDuBoI
    ) {
    }

    public record Utenlandsopphold1Row(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @Valid @NotNull Landvalg hvilketLandBoddeDuI
    ) {
    }
}
