package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;


/** NAV 14-05.08 – Søknad om engangsstønad ved adopsjon */
public record Nav140508Data(
    @NotNull Boolean jegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger,
    @NotNull String fornavnSoker,
    @NotNull String etternavnSoker,
    @NotNull String fodselsnummerDNummerSoker,
    @NotNull HvaSokerDuOm hvaSokerDuOm,
    @NotNull LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa,
    List<@Valid LeggTilBarnsFodselsdatoRow> leggTilBarnsFodselsdato,
    @NotNull JaNei borDuINorge,
    @NotNull HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene,
    List<@Valid LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow> leggTilNyttUtenlandsoppholdDeNeste12Manedene,
    @NotNull HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene,
    List<@Valid LeggTilNyttUtenlandsoppholdRow> leggTilNyttUtenlandsopphold,
    @NotNull JaNei harDuTilleggsopplysningerSomErRelevantForSoknaden,
    String tilleggsopplysninger,
    @NotNull Boolean deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad
) {

    public enum HvaSokerDuOm {
        @JsonProperty("engangsstonadVedAdopsjon")
        ENGANGSSTONAD_VED_ADOPSJON,
        @JsonProperty("engangsstonadVedOvertakelseAvForeldreansvaretEllerOmsorgen")
        ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN,
        @JsonProperty("engangsstonadTilFarSomAdoptererAlene")
        ENGANGSSTONAD_TIL_FAR_SOM_ADOPTERER_ALENE,
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

    public record LeggTilBarnsFodselsdatoRow(@NotNull LocalDate fodselsdatoDdMmAaaa) {
    }

    public record LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @NotNull String hvilketLandSkalDuBoI
    ) {
    }

    public record LeggTilNyttUtenlandsoppholdRow(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @NotNull String hvilketLandBoddeDuI
    ) {
    }
}
