package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;


/** NAV 14-05.07 – Søknad om engangsstønad ved fødsel */
public record Nav140507Data(
    @NotNull Boolean jegHarLestOgForstattDetSomStarPaNavNoRettogplikt,
    @NotNull String fornavnSoker,
    @NotNull String etternavnSoker,
    @NotNull String fodselsnummerDNummerSoker,
    @NotNull HvaSokerDuOm hvaSokerDuOm,
    @NotNull NarErBarnetFodt narErBarnetFodt,
    @NotNull @Min(1) @Max(9) Integer antallBarn,
    List<@Valid LeggTilBarnetEllerBarnasFodselsdatoRow> leggTilBarnetEllerBarnasFodselsdato,
    LocalDate termindatoDdMmAaaa,
    LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa,
    @NotNull JaNei planleggerDuAVaereINorgePaFodselstidspunktet1,
    @NotNull HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene,
    List<@Valid UtenlandsoppholdRow> utenlandsopphold,
    @NotNull HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene,
    List<@Valid Utenlandsopphold1Row> utenlandsopphold1,
    @NotNull JaNei harDuTilleggsopplysningerSomErRelevantForSoknaden,
    String tilleggsopplysninger,
    @NotNull Boolean deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad
) {

    public enum HvaSokerDuOm {
        @JsonProperty("engangsstonadVedFodsel")
        ENGANGSSTONAD_VED_FODSEL,
        @JsonProperty("engangsstonadVedOvertakelseAvForeldreansvaretEllerOmsorgen")
        ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN,
        @JsonEnumDefaultValue
        UDEFINERT;
    }

    public enum NarErBarnetFodt {
        @JsonProperty("tilbakeITid")
        TILBAKE_I_TID,
        @JsonProperty("fremITid")
        FREM_I_TID,
        @JsonEnumDefaultValue
        UDEFINERT;
    }

    public enum HvorSkalDuBoDeNeste12Manedene {
        @JsonProperty("boIUtlandetHeltEllerDelvis")
        BO_I_UTLANDET_HELT_ELLER_DELVIS,
        @JsonProperty("kunBoINorge")
        KUN_BO_I_NORGE,
        @JsonEnumDefaultValue
        UDEFINERT;
    }

    public enum HvorHarDuBoddDeSiste12Manedene {
        @JsonProperty("boddIUtlandetHeltEllerDelvis")
        BODD_I_UTLANDET_HELT_ELLER_DELVIS,
        @JsonProperty("kunBoddINorge")
        KUN_BODD_I_NORGE,
        @JsonEnumDefaultValue
        UDEFINERT;
    }

    public record LeggTilBarnetEllerBarnasFodselsdatoRow(LocalDate fodselsdatoDdMmAaaa) {
    }

    public record UtenlandsoppholdRow(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @NotNull String hvilketLandSkalDuBoI
    ) {
    }

    public record Utenlandsopphold1Row(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @NotNull String hvilketLandBoddeDuI
    ) {
    }
}
