package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;


/** NAV 14-04.10 – Søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser */
public record Nav140410Data(
    @Valid DineOpplysninger1 dineOpplysninger1,
    @NotNull HarDuBoddINorgeDeSiste12Manedene harDuBoddINorgeDeSiste12Manedene,
    @Valid Utenlandsopphold utenlandsopphold,
    @NotNull HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene,
    @Valid Utenlandsopphold1 utenlandsopphold1,
    @NotNull JaNei harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene,
    LocalDate narStartetDuSomFrilanserDdMmAaaa,
    JaNei jobberDuFortsattSomFrilanser,
    LocalDate narSluttetDuSomFrilanserDdMmAaaa,
    @NotNull JaNei harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene,
    HvilkenTypeVirksomhetDriverDu hvilkenTypeVirksomhetDriverDu,
    String hvaHeterVirksomhetenDin,
    LocalDate narStartetDuNaeringenDdMmAaaa,
    JaNei erTestRegistrertINorge,
    String virksomhetensOrganisasjonsnummer,
    @Valid Landvalg landvelger,
    JaNei erDetteEnVirksomhetDuDriverNa,
    LocalDate oppgiSluttdatoForNaeringenDdMmAaaa,
    HvorLengeHarDuHattDenneVirksomheten hvorLengeHarDuHattDenneVirksomheten,
    Integer hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene,
    JaNei harDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene,
    LocalDate narBleDuYrkesaktivDdMmAaaa,
    JaNei harDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene,
    LocalDate oppgiDatoForEndringenDdMmAaaa,
    Integer oppgiNaeringsinntektenDinEtterEndringen,
    String herKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin,
    @NotNull JaNei harDuHattJobbIEuEosLandDeSiste10Manedene,
    LocalDate fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa,
    JaNei erDetEnJobbDuHarPerIDag,
    LocalDate tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa,
    HvilketLandJobbetDuI hvilketLandJobbetDuI,
    String oppgiNavnetPaArbeidsgiveren,
    @Valid @NotNull HvorSkalDuSokeOmSvangerskapspengerFra hvorSkalDuSokeOmSvangerskapspengerFra,
    @NotNull LocalDate narHarDuTermindatoDdMmAaaa,
    @NotNull JaNei erBarnetFodt,
    LocalDate narBleBarnetFodtDdMmAaaa,
    @NotNull String beskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret,
    @NotNull LocalDate fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa,
    @Valid @NotNull HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger,
    List<@Valid RedusertArbeid1Row> redusertArbeid1,
    List<@Valid RedusertArbeidRow> redusertArbeid,
    List<@Valid JegKanIkkeFortsetteAJobbeRow> jegKanIkkeFortsetteAJobbe
) {

    public enum HarDuBoddINorgeDeSiste12Manedene {
        @JsonProperty("jegHarKunBoddINorge")
        JEG_HAR_KUN_BODD_I_NORGE,
        @JsonProperty("jegHarBoddIUtlandetHeltEllerDelvis")
        JEG_HAR_BODD_I_UTLANDET_HELT_ELLER_DELVIS;
    }

    public enum HvorSkalDuBoDeNeste12Manedene {
        @JsonProperty("jegSkalKunBoINorge")
        JEG_SKAL_KUN_BO_I_NORGE,
        @JsonProperty("jegSkalBoIUtlandetHeltEllerDelvis")
        JEG_SKAL_BO_I_UTLANDET_HELT_ELLER_DELVIS;
    }

    public enum HvilkenTypeVirksomhetDriverDu {
        @JsonProperty("fiske")
        FISKE,
        @JsonProperty("jordbruk")
        JORDBRUK,
        @JsonProperty("dagmammaEllerFamiliebarnehageIEgetHjem")
        DAGMAMMA_ELLER_FAMILIEBARNEHAGE_I_EGET_HJEM,
        @JsonProperty("annenTypeVirksomhet")
        ANNEN_TYPE_VIRKSOMHET;
    }

    public enum HvorLengeHarDuHattDenneVirksomheten {
        @JsonProperty("under4Ar")
        UNDER_4_AR,
        @JsonProperty("4ArEllerMer")
        _4_AR_ELLER_MER;
    }

    public enum HvilketLandJobbetDuI {
        @JsonProperty("belgia")
        BELGIA,
        @JsonProperty("bulgaria")
        BULGARIA,
        @JsonProperty("danmark")
        DANMARK,
        @JsonProperty("estland")
        ESTLAND,
        @JsonProperty("finland")
        FINLAND,
        @JsonProperty("frankrike")
        FRANKRIKE,
        @JsonProperty("hellas")
        HELLAS,
        @JsonProperty("irland")
        IRLAND,
        @JsonProperty("island")
        ISLAND,
        @JsonProperty("italia")
        ITALIA,
        @JsonProperty("kroatia")
        KROATIA,
        @JsonProperty("kypros")
        KYPROS,
        @JsonProperty("latvia")
        LATVIA,
        @JsonProperty("liechtenstein")
        LIECHTENSTEIN,
        @JsonProperty("litauen")
        LITAUEN,
        @JsonProperty("luxembourg")
        LUXEMBOURG,
        @JsonProperty("malta")
        MALTA,
        @JsonProperty("nederland")
        NEDERLAND,
        @JsonProperty("norge")
        NORGE,
        @JsonProperty("polen")
        POLEN,
        @JsonProperty("portugal")
        PORTUGAL,
        @JsonProperty("romania")
        ROMANIA,
        @JsonProperty("sveits")
        SVEITS,
        @JsonProperty("sverige")
        SVERIGE,
        @JsonProperty("slovakia")
        SLOVAKIA,
        @JsonProperty("slovenia")
        SLOVENIA,
        @JsonProperty("spania")
        SPANIA,
        @JsonProperty("tsjekkia")
        TSJEKKIA,
        @JsonProperty("tyskland")
        TYSKLAND,
        @JsonProperty("ungarn")
        UNGARN,
        @JsonProperty("osterrike")
        OSTERRIKE;
    }

    public record Utenlandsopphold(
        @Valid @NotNull Landvalg hvilketLandBoddeDuI,
        @NotNull LocalDate fraOgMedDatoDdMmAaaa,
        @NotNull LocalDate tilOgMedDatoDdMmAaaa
    ) {
    }

    public record Utenlandsopphold1(
        @Valid @NotNull Landvalg hvilketLandSkalDuBoI,
        @NotNull LocalDate fraOgMedDatoDdMmAaaa,
        @NotNull LocalDate fraOgMedDatoDdMmAaaa1
    ) {
    }

    public record HvorSkalDuSokeOmSvangerskapspengerFra(Boolean selvstendigNaeringsdrivende, Boolean frilanser) {
    }

    public record HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger(
        Boolean jegKanFortsetteMedSammeStillingsprosent,
        Boolean jegKanFortsetteMedRedusertArbeidstid,
        Boolean jegKanIkkeFortsetteAJobbe
    ) {
    }

    public record RedusertArbeid1Row(
        @NotNull LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa,
        @NotNull LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1
    ) {
    }

    public record RedusertArbeidRow(@NotNull LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa, @NotNull Integer oppgiStillingsprosentenDuSkalJobbe) {
    }

    public record JegKanIkkeFortsetteAJobbeRow(
        @NotNull LocalDate fraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa
    ) {
    }
}
