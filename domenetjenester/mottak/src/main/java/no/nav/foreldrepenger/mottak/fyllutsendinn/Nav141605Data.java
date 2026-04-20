package no.nav.foreldrepenger.mottak.fyllutsendinn;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;


/** NAV 14-16.05 – Søknad om endring eller nytt uttak av foreldrepenger */
public record Nav141605Data(
    @NotNull Boolean jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2,
    @Valid DineOpplysninger1 dineOpplysninger1,
    @NotNull HvemErDu hvemErDu,
    JaNei erDuAleneOmOmsorgenAvBarnet,
    BleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene bleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene,
    JaNei harDenAndreForelderenRettTilForeldrepenger,
    JaNei harDuOrientertDenAndreForelderenOmSoknadenDin,
    @Valid HvaSokerDuOm hvaSokerDuOm,
    @Valid HvaSokerDuOmIkkeMor hvaSokerDuOmIkkeMor,
    Boolean jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger,
    Boolean jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1,
    @Valid Mor mor,
    @Valid HvilkenPeriodeSkalDuTaUtFar hvilkenPeriodeSkalDuTaUtFar,
    @Valid HvilkenPeriodeSkalDuTaUtMedmor hvilkenPeriodeSkalDuTaUtMedmor,
    List<@Valid ForeldrepengerForFodselRow> foreldrepengerForFodsel,
    List<@Valid ModrekvoteRow> modrekvote,
    List<@Valid FedrekvoteRow> fedrekvote,
    List<@Valid MedmorkvoteRow> medmorkvote,
    List<@Valid FellesperiodeMorRow> fellesperiodeMor,
    List<@Valid FellesperiodeFarMedmorRow> fellesperiodeFarMedmor,
    List<@Valid KunMorRettRow> kunMorRett,
    List<@Valid KunFarRettRow> kunFarRett,
    List<@Valid KunMedmorRettRow> kunMedmorRett,
    List<@Valid PeriodeMedforeldrepengerVedAleneomsorgRow> periodeMedforeldrepengerVedAleneomsorg,
    List<@Valid PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow> periodeMedforeldrepengerVedAleneomsorgFarMedmor,
    List<@Valid PeriodeMedforeldrepengerRow> periodeMedforeldrepenger,
    List<@Valid JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> jegSokerOmAOvertaKvotenTilDenAndreForelderen,
    List<@Valid HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow> hvilkenPeriodeSkalDuIkkeAllikevelTaUt,
    List<@Valid PerioderMedUtsettelseForste6UkerEtterFodselRow> perioderMedUtsettelseForste6UkerEtterFodsel
) {

    public enum HvemErDu {
        @JsonProperty("mor")
        MOR,
        @JsonProperty("far")
        FAR,
        @JsonProperty("medmor")
        MEDMOR,
        @JsonProperty("jegHarOvertattForeldreansvaret")
        JEG_HAR_OVERTATT_FORELDREANSVARET;
    }

    public enum BleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene {
        @JsonProperty("jegVarAleneOmOmsorgenForJegFikkForeldrepenger")
        JEG_VAR_ALENE_OM_OMSORGEN_FOR_JEG_FIKK_FORELDREPENGER,
        @JsonProperty("jegHarBlittAleneOmOmsorgenEtterAtJegHarFattForeldrepenger")
        JEG_HAR_BLITT_ALENE_OM_OMSORGEN_ETTER_AT_JEG_HAR_FATT_FORELDREPENGER;
    }

    public enum HvaSkalMorGjoreIDennePerioden {
        @JsonProperty("arbeid")
        ARBEID,
        @JsonProperty("utdanningPaHeltid")
        UTDANNING_PA_HELTID,
        @JsonProperty("arbeidOgUtdanningSomTilSammenBlirHeltid")
        ARBEID_OG_UTDANNING_SOM_TIL_SAMMEN_BLIR_HELTID,
        @JsonProperty("kvalifiseringsprogrammet")
        KVALIFISERINGSPROGRAMMET,
        @JsonProperty("introduksjonsprogrammet")
        INTRODUKSJONSPROGRAMMET,
        @JsonProperty("forSykTilATaSegAvBarnet")
        FOR_SYK_TIL_A_TA_SEG_AV_BARNET,
        @JsonProperty("innlagtPaHelseinstitusjon")
        INNLAGT_PA_HELSEINSTITUSJON,
        @JsonProperty("brukerDagerFraFlerbarnsukene")
        BRUKER_DAGER_FRA_FLERBARNSUKENE;
    }

    public enum SkalDuTaUtForeldrepenger {
        @JsonProperty("ja")
        JA,
        @JsonProperty("neiJegSkalHaOppholdIForeldrepengeneMine")
        NEI_JEG_SKAL_HA_OPPHOLD_I_FORELDREPENGENE_MINE;
    }

    public enum HvaSkalMorGjoreIDennePeriodenOpphold {
        @JsonProperty("arbeid")
        ARBEID,
        @JsonProperty("utdanningPaHeltid")
        UTDANNING_PA_HELTID,
        @JsonProperty("arbeidOgUtdanningSomTilSammenBlirHeltid")
        ARBEID_OG_UTDANNING_SOM_TIL_SAMMEN_BLIR_HELTID,
        @JsonProperty("kvalifiseringsprogrammet")
        KVALIFISERINGSPROGRAMMET,
        @JsonProperty("introduksjonsprogrammet")
        INTRODUKSJONSPROGRAMMET,
        @JsonProperty("forSykTilATaSegAvBarnet")
        FOR_SYK_TIL_A_TA_SEG_AV_BARNET,
        @JsonProperty("innlagtPaHelseinstitusjon")
        INNLAGT_PA_HELSEINSTITUSJON;
    }

    public enum HvorforSkalDuOvertaKvoten {
        @JsonProperty("denAndreForelderenErForSykTilATaSegAvBarnet")
        DEN_ANDRE_FORELDEREN_ER_FOR_SYK_TIL_A_TA_SEG_AV_BARNET,
        @JsonProperty("denAndreForelderenErInnlagtPaHelseinstitusjon")
        DEN_ANDRE_FORELDEREN_ER_INNLAGT_PA_HELSEINSTITUSJON,
        @JsonProperty("jegHarBlittAleneOmOmsorgenForBarnet")
        JEG_HAR_BLITT_ALENE_OM_OMSORGEN_FOR_BARNET,
        @JsonProperty("denAndreForelderenHarIkkeRett")
        DEN_ANDRE_FORELDEREN_HAR_IKKE_RETT;
    }

    public enum HvorforSkalDuOvertaKvoten1 {
        @JsonProperty("denAndreForelderenErForSykTilATaSegAvBarnet")
        DEN_ANDRE_FORELDEREN_ER_FOR_SYK_TIL_A_TA_SEG_AV_BARNET,
        @JsonProperty("denAndreForelderenErInnlagtPaHelseinstitusjon")
        DEN_ANDRE_FORELDEREN_ER_INNLAGT_PA_HELSEINSTITUSJON,
        @JsonProperty("jegHarBlittAleneOmOmsorgenForBarnet")
        JEG_HAR_BLITT_ALENE_OM_OMSORGEN_FOR_BARNET;
    }

    public enum HvorforSkalDuUtsetteForeldrepenger {
        @JsonProperty("jegErForSykTilATaMegAvBarnet")
        JEG_ER_FOR_SYK_TIL_A_TA_MEG_AV_BARNET,
        @JsonProperty("jegErInnlagtIHelseinstitusjon")
        JEG_ER_INNLAGT_I_HELSEINSTITUSJON,
        @JsonProperty("barnetErInnlagtIHelseinstitusjon")
        BARNET_ER_INNLAGT_I_HELSEINSTITUSJON;
    }

    public record HvaSokerDuOm(
        Boolean periodeMedForeldrepenger,
        Boolean periodeUtenForeldrepenger,
        Boolean utsettelseForste6UkeneEtterFodsel
    ) {
    }

    public record HvaSokerDuOmIkkeMor(Boolean periodeMedForeldrepenger, Boolean periodeUtenForeldrepenger) {
    }

    public record Mor(
        @Valid HvilkenPeriodeSkalDuTaUtAleneMor hvilkenPeriodeSkalDuTaUtAleneMor,
        @Valid HvilkenPeriodeSkalDuTaUtKunMorRett hvilkenPeriodeSkalDuTaUtKunMorRett,
        @Valid HvilkenPeriodeSkalDuTaUtMor hvilkenPeriodeSkalDuTaUtMor
    ) {

        public record HvilkenPeriodeSkalDuTaUtAleneMor(Boolean foreldrepengerForFodsel, Boolean foreldrepengerVedAleneomsorg) {
        }

        public record HvilkenPeriodeSkalDuTaUtKunMorRett(Boolean foreldrepengerForFodsel, Boolean foreldrepengerKunMorRett) {
        }

        public record HvilkenPeriodeSkalDuTaUtMor(
            Boolean foreldrepengerForFodsel,
            Boolean modrekvote,
            Boolean fellesperiode,
            Boolean overforingAvAnnenForeldersKvote
        ) {
        }
    }

    public record HvilkenPeriodeSkalDuTaUtFar(
        Boolean fedrekvote,
        Boolean fellesperiode,
        Boolean overforingAvAnnenForeldersKvote
    ) {
    }

    public record HvilkenPeriodeSkalDuTaUtMedmor(
        Boolean medmorkvote,
        Boolean fellesperiode,
        Boolean overforingAvAnnenForeldersKvote
    ) {
    }

    public record ForeldrepengerForFodselRow(@NotNull LocalDate foreldrepengerFraOgMedDatoDdMmAaaa, @NotNull LocalDate foreldrepengerTilOgMedDatoDdMmAaaa) {
    }

    public record ModrekvoteRow(
        @NotNull LocalDate modrekvoteFraOgMedDatoDdMmAaaa,
        @NotNull LocalDate modrekvoteTilOgMedDatoDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode,
        @Min(0) @Max(100) Integer oppgiHvorMangeProsentForeldrepengerDuSkalTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record FedrekvoteRow(
        @NotNull LocalDate fedrekvoteFraOgMedDatoDdMmAaaa,
        @NotNull LocalDate fedrekvoteTilOgMedDatoDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record MedmorkvoteRow(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record FellesperiodeMorRow(
        @NotNull LocalDate fellesperiodeFraOgMedDdMmAaaa,
        @NotNull LocalDate fellesperiodeTilOgMedDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record FellesperiodeFarMedmorRow(
        @NotNull LocalDate fellesperiodeFraOgMedDdMmAaaa,
        @NotNull LocalDate fellesperiodeTilOgMedDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record KunMorRettRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record KunFarRettRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger,
        HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden,
        HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record KunMedmorRettRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger,
        HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden,
        HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record PeriodeMedforeldrepengerVedAleneomsorgRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record PeriodeMedforeldrepengerRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbeOvertattForeldreansvaret hvorSkalDuJobbeOvertattForeldreansvaret,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbeOvertattForeldreansvaret(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record JegSokerOmAOvertaKvotenTilDenAndreForelderenRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        HvorforSkalDuOvertaKvoten hvorforSkalDuOvertaKvoten,
        HvorforSkalDuOvertaKvoten1 hvorforSkalDuOvertaKvoten1,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote,
        @Min(1) @Max(99) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {

        public record HvorSkalDuJobbe(
            Boolean hosArbeidsgiver,
            Boolean frilanser,
            Boolean selvstendigNaeringsdrivende
        ) {
        }
    }

    public record HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow(@NotNull LocalDate datoFraOgMedDdMmAaaaUtenForeldrepenger, @NotNull LocalDate datoTilOgMedDdMmAaaa) {
    }

    public record PerioderMedUtsettelseForste6UkerEtterFodselRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull HvorforSkalDuUtsetteForeldrepenger hvorforSkalDuUtsetteForeldrepenger
    ) {
    }
}
