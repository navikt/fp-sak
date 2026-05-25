package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;


/** NAV 14-05.06 – Søknad om foreldrepenger ved adopsjon */
public record Nav140506Data(
    @NotNull Boolean jegBekrefterAtJegHarLestOgForstattMinePlikter,
    @Valid DineOpplysninger1 dineOpplysninger1,
    @NotNull HvemErDu hvemErDu,
    @NotNull HvorLangPeriodeMedForeldrepengerOnskerDu hvorLangPeriodeMedForeldrepengerOnskerDu,
    JaNei gjelderSoknadenDinStebarnsadopsjon,
    LocalDate oppgiDatoenForStebarnsadopsjonDdMmAaaa,
    LocalDate narOvertarDuOmsorgenDdMmAaaa,
    LocalDate datoForOmsorgsovertakelseDdMmAaaa,
    @Min(1) @Max(9) Integer hvorMangeBarnSkalDuAdoptere,
    Integer hvorMangeBarnOvertarDuOmsorgenFor,
    LocalDate narBleDetEldsteBarnetFodtDdMmAaaa,
    LocalDate narBleDetEldsteBarnetFodtDdMmAaaa1,
    @NotNull JaNei kanDuGiOssNavnetPaDenAndreForelderen,
    String fornavn,
    String etternavn,
    JaNei harDenAndreForelderenNorskFodselsnummerEllerDNummer,
    String hvaErDenAndreForelderensFodselsnummerEllerDNummer,
    String hvaErDenAndreForelderensUtenlandskeFodselsnummer,
    @Valid Landvalg hvorBorDenAndreForelderen,
    JaNei erDuAleneOmOmsorgenAvBarnet,
    JaNei harDenAndreForelderenRettTilForeldrepenger,
    JaNei harDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt,
    JaNei harDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt,
    JaNei harDenAndreForelderenUforetrygd,
    JaNei harDuOrientertDenAndreForelderenOmSoknadenDin,
    @Valid Mor mor,
    @Valid HvilkenPeriodeSkalDuTaUtFar hvilkenPeriodeSkalDuTaUtFar,
    @Valid HvilkenPeriodeSkalDuTaUtMedmor hvilkenPeriodeSkalDuTaUtMedmor,
    List<@Valid ModrekvoteRow> modrekvote,
    List<@Valid FedrekvoteRow> fedrekvote,
    List<@Valid MedmorkvoteRow> medmorkvote,
    List<@Valid FellesperiodeMorRow> fellesperiodeMor,
    List<@Valid FellesperiodeFarMedmorRow> fellesperiodeFarMedmor,
    List<@Valid KunMorRettRow> kunMorRett,
    List<@Valid KunFarRettRow> kunFarRett,
    List<@Valid KunMedmorRettRow> kunMedmorRett,
    List<@Valid PeriodeMedforeldrepengerVedAleneomsorgRow> periodeMedforeldrepengerVedAleneomsorg,
    List<@Valid JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> jegSokerOmAOvertaKvotenTilDenAndreForelderen,
    List<@Valid PeriodeMedforeldrepengerRow> periodeMedforeldrepenger,
    @NotNull HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene,
    List<@Valid LeggTilUtenlandsoppholdForDeNeste12ManedeneRow> leggTilUtenlandsoppholdForDeNeste12Manedene,
    @NotNull HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene,
    List<@Valid LeggTilUtenlandsoppholdForDeSiste12ManedeneRow> leggTilUtenlandsoppholdForDeSiste12Manedene,
    @NotNull JaNei harDuArbeidsforholdINorge,
    @NotNull JaNei harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene,
    @Valid Frilanser frilanser,
    @NotNull JaNei harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene,
    HvilkenTypeVirksomhetDriverDu hvilkenTypeVirksomhetDriverDu,
    String hvaHeterVirksomhetenDin,
    LocalDate narStartetDuVirksomhetenDdMmAaaa,
    JaNei erVirksomhetenDinRegistrertINorge,
    String virksomhetensOrganisasjonsnummer1,
    @Valid Landvalg hvilketLandErVirksomhetenRegistrertI,
    JaNei erDuFremdelesSelvstendigNaeringsdrivende,
    LocalDate datoForAvsluttetNaeringsdriftDdMmAaaa,
    HvorLengeHarDuVaertSelvstendigNaeringsdrivende hvorLengeHarDuVaertSelvstendigNaeringsdrivende,
    String hvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten,
    String hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene,
    JaNei harDuBlittYrkesaktivILopetAvDetSisteAret,
    JaNei harDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene,
    LocalDate narBleDuYrkesaktivDdMmAaaa,
    JaNei harDuHattEnVarigEndringIArbeidsforholdetDitt,
    @Valid VarigEndring varigEndring,
    @Valid AndreInntektskilder andreInntektskilder
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

    public enum HvorLangPeriodeMedForeldrepengerOnskerDu {
        @JsonProperty("100ProsentForeldrepenger")
        _100_PROSENT_FORELDREPENGER,  // NOSONAR
        @JsonProperty("80ProsentForeldrepenger")
        _80_PROSENT_FORELDREPENGER;  // NOSONAR
    }

    public enum HvorforSkalDuOvertaKvoten {
        @JsonProperty("denAndreForelderenErForSykTilATaSegAvBarnet")
        DEN_ANDRE_FORELDEREN_ER_FOR_SYK_TIL_A_TA_SEG_AV_BARNET,
        @JsonProperty("denAndreForelderenErInnlagtPaHelseinstitusjon")
        DEN_ANDRE_FORELDEREN_ER_INNLAGT_PA_HELSEINSTITUSJON;
    }

    public enum HvorLengeHarDuVaertSelvstendigNaeringsdrivende {
        @JsonProperty("mindreEnn1Ar")
        MINDRE_ENN_1_AR,
        @JsonProperty("mellom1Og4Ar")
        MELLOM_1_OG_4_AR,
        @JsonProperty("merEnn4Ar")
        MER_ENN_4_AR;
    }

    public enum VelgInntektstype {
        @JsonProperty("jobbIUtlandet")
        JOBB_I_UTLANDET,
        @JsonProperty("sluttvederlagSluttpakkeEllerEtterlonn")
        SLUTTVEDERLAG_SLUTTPAKKE_ELLER_ETTERLONN,
        @JsonProperty("forstegangstjenesteIForsvaretEllerSivilforsvaret")
        FORSTEGANGSTJENESTE_I_FORSVARET_ELLER_SIVILFORSVARET;
    }

    public record Mor(
        @Valid HvilkenPeriodeSkalDuTaUtMor hvilkenPeriodeSkalDuTaUtMor
    ) {

        public record HvilkenPeriodeSkalDuTaUtMor(
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

    public record ModrekvoteRow(
        @NotNull LocalDate modrekvoteFraOgMedDatoDdMmAaaa,
        @NotNull LocalDate modrekvoteFraOgMedDatoDdMmAaaa1,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode,
        @Min(0) @Max(100) Integer oppgiHvorMangeProsentForeldrepengerDuSkalTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record FedrekvoteRow(
        @NotNull LocalDate fedrekvoteFraOgMedDatoDdMmAaaa,
        @NotNull LocalDate fedrekvoteTilOgMedDatoDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record MedmorkvoteRow(
        @NotNull LocalDate fraDatoDdMmAaaa,
        @NotNull LocalDate tilDatoDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record FellesperiodeMorRow(
        @NotNull LocalDate fellesperiodeFraOgMedDdMmAaaa,
        @NotNull LocalDate fellesperiodeTilOgMedDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record FellesperiodeFarMedmorRow(
        @NotNull LocalDate fellesperiodeFraOgMedDdMmAaaa,
        @NotNull LocalDate fellesperiodeTilOgMedDdMmAaaa,
        @NotNull JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1,
        @Min(0) @Max(100) Integer hvorMangeProsentForeldrepengerSkalDuTaUt,
        @NotNull HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record KunMorRettRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record KunFarRettRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger,
        HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden,
        HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record KunMedmorRettRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoFraOgMedDdMmAaaa1,
        @NotNull SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger,
        HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden,
        HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record PeriodeMedforeldrepengerVedAleneomsorgRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record JegSokerOmAOvertaKvotenTilDenAndreForelderenRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull HvorforSkalDuOvertaKvoten hvorforSkalDuOvertaKvoten,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbe,
        String navnPaArbeidsgiver
    ) {
    }

    public record PeriodeMedforeldrepengerRow(
        @NotNull LocalDate datoFraOgMedDdMmAaaa,
        @NotNull LocalDate datoTilOgMedDdMmAaaa,
        @NotNull JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret,
        @Min(0) @Max(100) Integer oppgiStillingsprosentenDuSkalJobbe,
        @Valid HvorSkalDuJobbe hvorSkalDuJobbeOvertattForeldreansvaret,
        String navnPaArbeidsgiver
    ) {
    }

    public record LeggTilUtenlandsoppholdForDeNeste12ManedeneRow(
        @NotNull LocalDate fraOgMedDdMmAaaa,
        @NotNull LocalDate tilOgMedDdMmAaaa,
        @Valid @NotNull Landvalg hvilketLandSkalDuBoI
    ) {
    }

    public record LeggTilUtenlandsoppholdForDeSiste12ManedeneRow(
        @NotNull LocalDate fraOgMedDdMmAaaa,
        @NotNull LocalDate tilOgMedDdMmAaaa,
        @Valid @NotNull Landvalg hvilketLandBoddeDuI
    ) {
    }

    public record Frilanser(
        @NotNull LocalDate narStartetDuSomFrilanserDdMmAaaa,
        @NotNull JaNei jobberDuFortsattSomFrilanser,
        LocalDate sluttdatoSomFrilanserDdMmAaaa
    ) {
    }

    public record VarigEndring(
        @NotNull LocalDate datoForEndringenDdMmAaaa,
        @NotNull String naeringsinntektenDinEtterEndringen,
        @NotNull String skrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin
    ) {
    }

    public record AndreInntektskilder(
        @NotNull JaNei harDuHattAndreInntektskilderDeSiste10Manedene,
        List<@Valid LeggTilInntektskildeRow> leggTilInntektskilde
    ) {

        public record LeggTilInntektskildeRow(
            @NotNull VelgInntektstype velgInntektstype,
            @NotNull LocalDate fraOgMedDdMmAaaa,
            LocalDate tilOgMedDdMmAaaa,
            Boolean pagaende,
            String landDuJobberI,
            String navnetPaArbeidsgiveren
        ) {
        }
    }
}
