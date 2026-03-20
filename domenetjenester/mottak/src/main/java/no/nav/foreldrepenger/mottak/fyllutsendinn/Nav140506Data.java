package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/** NAV 14-05.06 – Søknad om foreldrepenger ved adopsjon */
public class Nav140506Data {

    private Boolean jegBekrefterAtJegHarLestOgForstattMinePlikter;

    private DineOpplysninger1 dineOpplysninger1;

    private HvemErDu hvemErDu;

    private HvorLangPeriodeMedForeldrepengerOnskerDu hvorLangPeriodeMedForeldrepengerOnskerDu;

    private JaNei gjelderSoknadenDinStebarnsadopsjon;

    private LocalDate oppgiDatoenForStebarnsadopsjonDdMmAaaa;

    private LocalDate narOvertarDuOmsorgenDdMmAaaa;

    private LocalDate datoForOmsorgsovertakelseDdMmAaaa;

    private Integer hvorMangeBarnSkalDuAdoptere;

    private Integer hvorMangeBarnOvertarDuOmsorgenFor;

    private LocalDate narBleDetEldsteBarnetFodtDdMmAaaa;

    private LocalDate narBleDetEldsteBarnetFodtDdMmAaaa1;

    private JaNei kanDuGiOssNavnetPaDenAndreForelderen;

    private String fornavn;

    private String etternavn;

    private JaNei harDenAndreForelderenNorskFodselsnummerEllerDNummer;

    private String hvaErDenAndreForelderensFodselsnummerEllerDNummer;

    private String hvaErDenAndreForelderensUtenlandskeFodselsnummer;

    private String hvorBorDenAndreForelderen;

    private JaNei erDuAleneOmOmsorgenAvBarnet;

    private JaNei harDenAndreForelderenRettTilForeldrepenger;

    private JaNei harDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt;

    private JaNei harDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt;

    private JaNei harDenAndreForelderenUforetrygd;

    private JaNei harDuOrientertDenAndreForelderenOmSoknadenDin;

    private Mor mor;

    private HvilkenPeriodeSkalDuTaUtFar hvilkenPeriodeSkalDuTaUtFar;

    private HvilkenPeriodeSkalDuTaUtMedmor hvilkenPeriodeSkalDuTaUtMedmor;

    private List<ModrekvoteRow> modrekvote;

    private List<FedrekvoteRow> fedrekvote;

    private List<MedmorkvoteRow> medmorkvote;

    private List<FellesperiodeMorRow> fellesperiodeMor;

    private List<FellesperiodeFarMedmorRow> fellesperiodeFarMedmor;

    private List<KunMorRettRow> kunMorRett;

    private List<KunFarRettRow> kunFarRett;

    private List<KunMedmorRettRow> kunMedmorRett;

    private List<PeriodeMedforeldrepengerVedAleneomsorgRow> periodeMedforeldrepengerVedAleneomsorg;

    private List<JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> jegSokerOmAOvertaKvotenTilDenAndreForelderen;

    private List<PeriodeMedforeldrepengerRow> periodeMedforeldrepenger;

    private HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene;

    private List<LeggTilUtenlandsoppholdForDeNeste12ManedeneRow> leggTilUtenlandsoppholdForDeNeste12Manedene;

    private HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene;

    private List<LeggTilUtenlandsoppholdForDeSiste12ManedeneRow> leggTilUtenlandsoppholdForDeSiste12Manedene;

    private JaNei harDuArbeidsforholdINorge;

    private JaNei harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene;

    private Frilanser frilanser;

    private JaNei harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene;

    private HvilkenTypeVirksomhetDriverDu hvilkenTypeVirksomhetDriverDu;

    private String hvaHeterVirksomhetenDin;

    private LocalDate narStartetDuVirksomhetenDdMmAaaa;

    private JaNei erVirksomhetenDinRegistrertINorge;

    private String virksomhetensOrganisasjonsnummer1;

    private String hvilketLandErVirksomhetenRegistrertI;

    private JaNei erDuFremdelesSelvstendigNaeringsdrivende;

    private LocalDate datoForAvsluttetNaeringsdriftDdMmAaaa;

    private HvorLengeHarDuVaertSelvstendigNaeringsdrivende hvorLengeHarDuVaertSelvstendigNaeringsdrivende;

    private BigDecimal hvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten;

    private BigDecimal hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene;

    private JaNei harDuBlittYrkesaktivILopetAvDetSisteAret;

    private JaNei harDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene;

    private LocalDate narBleDuYrkesaktivDdMmAaaa;

    private JaNei harDuHattEnVarigEndringIArbeidsforholdetDitt;

    private VarigEndring varigEndring;

    private AndreInntektskilder andreInntektskilder;

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
        _100_PROSENT_FORELDREPENGER,
        @JsonProperty("80ProsentForeldrepenger")
        _80_PROSENT_FORELDREPENGER;
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
        DEN_ANDRE_FORELDEREN_ER_INNLAGT_PA_HELSEINSTITUSJON;
    }

    public enum HvorSkalDuBoDeNeste12Manedene {
        @JsonProperty("kunBoINorge")
        KUN_BO_I_NORGE,
        @JsonProperty("boIUtlandetHeltEllerDelvis")
        BO_I_UTLANDET_HELT_ELLER_DELVIS;
    }

    public enum HvorHarDuBoddDeSiste12Manedene {
        @JsonProperty("kunBoddINorge")
        KUN_BODD_I_NORGE,
        @JsonProperty("boddIUtlandetHeltEllerDelvis")
        BODD_I_UTLANDET_HELT_ELLER_DELVIS;
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

    public static class Mor {

        private HvilkenPeriodeSkalDuTaUtMor hvilkenPeriodeSkalDuTaUtMor;

        public static class HvilkenPeriodeSkalDuTaUtMor {

            private Boolean modrekvote;

            private Boolean fellesperiode;

            private Boolean overforingAvAnnenForeldersKvote;

            public HvilkenPeriodeSkalDuTaUtMor() {}

            public Boolean isModrekvote() {
                return modrekvote;
            }

            public void setModrekvote(Boolean modrekvote) {
                this.modrekvote = modrekvote;
            }

            public Boolean isFellesperiode() {
                return fellesperiode;
            }

            public void setFellesperiode(Boolean fellesperiode) {
                this.fellesperiode = fellesperiode;
            }

            public Boolean isOverforingAvAnnenForeldersKvote() {
                return overforingAvAnnenForeldersKvote;
            }

            public void setOverforingAvAnnenForeldersKvote(Boolean overforingAvAnnenForeldersKvote) {
                this.overforingAvAnnenForeldersKvote = overforingAvAnnenForeldersKvote;
            }

        }

        public Mor() {}

        public HvilkenPeriodeSkalDuTaUtMor getHvilkenPeriodeSkalDuTaUtMor() {
            return hvilkenPeriodeSkalDuTaUtMor;
        }

        public void setHvilkenPeriodeSkalDuTaUtMor(HvilkenPeriodeSkalDuTaUtMor hvilkenPeriodeSkalDuTaUtMor) {
            this.hvilkenPeriodeSkalDuTaUtMor = hvilkenPeriodeSkalDuTaUtMor;
        }

    }

    public static class HvilkenPeriodeSkalDuTaUtFar {

        private Boolean fedrekvote;

        private Boolean fellesperiode;

        private Boolean overforingAvAnnenForeldersKvote;

        public HvilkenPeriodeSkalDuTaUtFar() {}

        public Boolean isFedrekvote() {
            return fedrekvote;
        }

        public void setFedrekvote(Boolean fedrekvote) {
            this.fedrekvote = fedrekvote;
        }

        public Boolean isFellesperiode() {
            return fellesperiode;
        }

        public void setFellesperiode(Boolean fellesperiode) {
            this.fellesperiode = fellesperiode;
        }

        public Boolean isOverforingAvAnnenForeldersKvote() {
            return overforingAvAnnenForeldersKvote;
        }

        public void setOverforingAvAnnenForeldersKvote(Boolean overforingAvAnnenForeldersKvote) {
            this.overforingAvAnnenForeldersKvote = overforingAvAnnenForeldersKvote;
        }

    }

    public static class HvilkenPeriodeSkalDuTaUtMedmor {

        private Boolean medmorkvote;

        private Boolean fellesperiode;

        private Boolean overforingAvAnnenForeldersKvote;

        public HvilkenPeriodeSkalDuTaUtMedmor() {}

        public Boolean isMedmorkvote() {
            return medmorkvote;
        }

        public void setMedmorkvote(Boolean medmorkvote) {
            this.medmorkvote = medmorkvote;
        }

        public Boolean isFellesperiode() {
            return fellesperiode;
        }

        public void setFellesperiode(Boolean fellesperiode) {
            this.fellesperiode = fellesperiode;
        }

        public Boolean isOverforingAvAnnenForeldersKvote() {
            return overforingAvAnnenForeldersKvote;
        }

        public void setOverforingAvAnnenForeldersKvote(Boolean overforingAvAnnenForeldersKvote) {
            this.overforingAvAnnenForeldersKvote = overforingAvAnnenForeldersKvote;
        }

    }

    public static class ModrekvoteRow {

        private LocalDate modrekvoteFraOgMedDatoDdMmAaaa;

        private LocalDate modrekvoteFraOgMedDatoDdMmAaaa1;

        private JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode;

        private Integer oppgiHvorMangeProsentForeldrepengerDuSkalTaUt;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public ModrekvoteRow() {}

        public LocalDate getModrekvoteFraOgMedDatoDdMmAaaa() {
            return modrekvoteFraOgMedDatoDdMmAaaa;
        }

        public void setModrekvoteFraOgMedDatoDdMmAaaa(LocalDate modrekvoteFraOgMedDatoDdMmAaaa) {
            this.modrekvoteFraOgMedDatoDdMmAaaa = modrekvoteFraOgMedDatoDdMmAaaa;
        }

        public LocalDate getModrekvoteFraOgMedDatoDdMmAaaa1() {
            return modrekvoteFraOgMedDatoDdMmAaaa1;
        }

        public void setModrekvoteFraOgMedDatoDdMmAaaa1(LocalDate modrekvoteFraOgMedDatoDdMmAaaa1) {
            this.modrekvoteFraOgMedDatoDdMmAaaa1 = modrekvoteFraOgMedDatoDdMmAaaa1;
        }

        public JaNei getSkalDenAndreForelderenHaForeldrepengerISammePeriode() {
            return skalDenAndreForelderenHaForeldrepengerISammePeriode;
        }

        public void setSkalDenAndreForelderenHaForeldrepengerISammePeriode(JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode) {
            this.skalDenAndreForelderenHaForeldrepengerISammePeriode = skalDenAndreForelderenHaForeldrepengerISammePeriode;
        }

        public Integer getOppgiHvorMangeProsentForeldrepengerDuSkalTaUt() {
            return oppgiHvorMangeProsentForeldrepengerDuSkalTaUt;
        }

        public void setOppgiHvorMangeProsentForeldrepengerDuSkalTaUt(Integer oppgiHvorMangeProsentForeldrepengerDuSkalTaUt) {
            this.oppgiHvorMangeProsentForeldrepengerDuSkalTaUt = oppgiHvorMangeProsentForeldrepengerDuSkalTaUt;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class FedrekvoteRow {

        private LocalDate fedrekvoteFraOgMedDatoDdMmAaaa;

        private LocalDate fedrekvoteTilOgMedDatoDdMmAaaa;

        private JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode;

        private Integer hvorMangeProsentForeldrepengerSkalDuTaUt;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public FedrekvoteRow() {}

        public LocalDate getFedrekvoteFraOgMedDatoDdMmAaaa() {
            return fedrekvoteFraOgMedDatoDdMmAaaa;
        }

        public void setFedrekvoteFraOgMedDatoDdMmAaaa(LocalDate fedrekvoteFraOgMedDatoDdMmAaaa) {
            this.fedrekvoteFraOgMedDatoDdMmAaaa = fedrekvoteFraOgMedDatoDdMmAaaa;
        }

        public LocalDate getFedrekvoteTilOgMedDatoDdMmAaaa() {
            return fedrekvoteTilOgMedDatoDdMmAaaa;
        }

        public void setFedrekvoteTilOgMedDatoDdMmAaaa(LocalDate fedrekvoteTilOgMedDatoDdMmAaaa) {
            this.fedrekvoteTilOgMedDatoDdMmAaaa = fedrekvoteTilOgMedDatoDdMmAaaa;
        }

        public JaNei getSkalDenAndreForelderenHaForeldrepengerISammePeriode() {
            return skalDenAndreForelderenHaForeldrepengerISammePeriode;
        }

        public void setSkalDenAndreForelderenHaForeldrepengerISammePeriode(JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode) {
            this.skalDenAndreForelderenHaForeldrepengerISammePeriode = skalDenAndreForelderenHaForeldrepengerISammePeriode;
        }

        public Integer getHvorMangeProsentForeldrepengerSkalDuTaUt() {
            return hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public void setHvorMangeProsentForeldrepengerSkalDuTaUt(Integer hvorMangeProsentForeldrepengerSkalDuTaUt) {
            this.hvorMangeProsentForeldrepengerSkalDuTaUt = hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class MedmorkvoteRow {

        private LocalDate fraDatoDdMmAaaa;

        private LocalDate tilDatoDdMmAaaa;

        private JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode;

        private Integer hvorMangeProsentForeldrepengerSkalDuTaUt;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public MedmorkvoteRow() {}

        public LocalDate getFraDatoDdMmAaaa() {
            return fraDatoDdMmAaaa;
        }

        public void setFraDatoDdMmAaaa(LocalDate fraDatoDdMmAaaa) {
            this.fraDatoDdMmAaaa = fraDatoDdMmAaaa;
        }

        public LocalDate getTilDatoDdMmAaaa() {
            return tilDatoDdMmAaaa;
        }

        public void setTilDatoDdMmAaaa(LocalDate tilDatoDdMmAaaa) {
            this.tilDatoDdMmAaaa = tilDatoDdMmAaaa;
        }

        public JaNei getSkalDenAndreForelderenHaForeldrepengerISammePeriode() {
            return skalDenAndreForelderenHaForeldrepengerISammePeriode;
        }

        public void setSkalDenAndreForelderenHaForeldrepengerISammePeriode(JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode) {
            this.skalDenAndreForelderenHaForeldrepengerISammePeriode = skalDenAndreForelderenHaForeldrepengerISammePeriode;
        }

        public Integer getHvorMangeProsentForeldrepengerSkalDuTaUt() {
            return hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public void setHvorMangeProsentForeldrepengerSkalDuTaUt(Integer hvorMangeProsentForeldrepengerSkalDuTaUt) {
            this.hvorMangeProsentForeldrepengerSkalDuTaUt = hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class FellesperiodeMorRow {

        private LocalDate fellesperiodeFraOgMedDdMmAaaa;

        private LocalDate fellesperiodeTilOgMedDdMmAaaa;

        private JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1;

        private Integer hvorMangeProsentForeldrepengerSkalDuTaUt;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public FellesperiodeMorRow() {}

        public LocalDate getFellesperiodeFraOgMedDdMmAaaa() {
            return fellesperiodeFraOgMedDdMmAaaa;
        }

        public void setFellesperiodeFraOgMedDdMmAaaa(LocalDate fellesperiodeFraOgMedDdMmAaaa) {
            this.fellesperiodeFraOgMedDdMmAaaa = fellesperiodeFraOgMedDdMmAaaa;
        }

        public LocalDate getFellesperiodeTilOgMedDdMmAaaa() {
            return fellesperiodeTilOgMedDdMmAaaa;
        }

        public void setFellesperiodeTilOgMedDdMmAaaa(LocalDate fellesperiodeTilOgMedDdMmAaaa) {
            this.fellesperiodeTilOgMedDdMmAaaa = fellesperiodeTilOgMedDdMmAaaa;
        }

        public JaNei getSkalDenAndreForelderenHaForeldrepengerISammePeriode1() {
            return skalDenAndreForelderenHaForeldrepengerISammePeriode1;
        }

        public void setSkalDenAndreForelderenHaForeldrepengerISammePeriode1(JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1) {
            this.skalDenAndreForelderenHaForeldrepengerISammePeriode1 = skalDenAndreForelderenHaForeldrepengerISammePeriode1;
        }

        public Integer getHvorMangeProsentForeldrepengerSkalDuTaUt() {
            return hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public void setHvorMangeProsentForeldrepengerSkalDuTaUt(Integer hvorMangeProsentForeldrepengerSkalDuTaUt) {
            this.hvorMangeProsentForeldrepengerSkalDuTaUt = hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class FellesperiodeFarMedmorRow {

        private LocalDate fellesperiodeFraOgMedDdMmAaaa;

        private LocalDate fellesperiodeTilOgMedDdMmAaaa;

        private JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1;

        private Integer hvorMangeProsentForeldrepengerSkalDuTaUt;

        private HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public FellesperiodeFarMedmorRow() {}

        public LocalDate getFellesperiodeFraOgMedDdMmAaaa() {
            return fellesperiodeFraOgMedDdMmAaaa;
        }

        public void setFellesperiodeFraOgMedDdMmAaaa(LocalDate fellesperiodeFraOgMedDdMmAaaa) {
            this.fellesperiodeFraOgMedDdMmAaaa = fellesperiodeFraOgMedDdMmAaaa;
        }

        public LocalDate getFellesperiodeTilOgMedDdMmAaaa() {
            return fellesperiodeTilOgMedDdMmAaaa;
        }

        public void setFellesperiodeTilOgMedDdMmAaaa(LocalDate fellesperiodeTilOgMedDdMmAaaa) {
            this.fellesperiodeTilOgMedDdMmAaaa = fellesperiodeTilOgMedDdMmAaaa;
        }

        public JaNei getSkalDenAndreForelderenHaForeldrepengerISammePeriode1() {
            return skalDenAndreForelderenHaForeldrepengerISammePeriode1;
        }

        public void setSkalDenAndreForelderenHaForeldrepengerISammePeriode1(JaNei skalDenAndreForelderenHaForeldrepengerISammePeriode1) {
            this.skalDenAndreForelderenHaForeldrepengerISammePeriode1 = skalDenAndreForelderenHaForeldrepengerISammePeriode1;
        }

        public Integer getHvorMangeProsentForeldrepengerSkalDuTaUt() {
            return hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public void setHvorMangeProsentForeldrepengerSkalDuTaUt(Integer hvorMangeProsentForeldrepengerSkalDuTaUt) {
            this.hvorMangeProsentForeldrepengerSkalDuTaUt = hvorMangeProsentForeldrepengerSkalDuTaUt;
        }

        public HvaSkalMorGjoreIDennePerioden getHvaSkalMorGjoreIDennePerioden() {
            return hvaSkalMorGjoreIDennePerioden;
        }

        public void setHvaSkalMorGjoreIDennePerioden(HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden) {
            this.hvaSkalMorGjoreIDennePerioden = hvaSkalMorGjoreIDennePerioden;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class KunMorRettRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoFraOgMedDdMmAaaa1;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public KunMorRettRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa() {
            return datoFraOgMedDdMmAaaa;
        }

        public void setDatoFraOgMedDdMmAaaa(LocalDate datoFraOgMedDdMmAaaa) {
            this.datoFraOgMedDdMmAaaa = datoFraOgMedDdMmAaaa;
        }

        public LocalDate getDatoFraOgMedDdMmAaaa1() {
            return datoFraOgMedDdMmAaaa1;
        }

        public void setDatoFraOgMedDdMmAaaa1(LocalDate datoFraOgMedDdMmAaaa1) {
            this.datoFraOgMedDdMmAaaa1 = datoFraOgMedDdMmAaaa1;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class KunFarRettRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoFraOgMedDdMmAaaa1;

        private SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger;

        private HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden;

        private HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public KunFarRettRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa() {
            return datoFraOgMedDdMmAaaa;
        }

        public void setDatoFraOgMedDdMmAaaa(LocalDate datoFraOgMedDdMmAaaa) {
            this.datoFraOgMedDdMmAaaa = datoFraOgMedDdMmAaaa;
        }

        public LocalDate getDatoFraOgMedDdMmAaaa1() {
            return datoFraOgMedDdMmAaaa1;
        }

        public void setDatoFraOgMedDdMmAaaa1(LocalDate datoFraOgMedDdMmAaaa1) {
            this.datoFraOgMedDdMmAaaa1 = datoFraOgMedDdMmAaaa1;
        }

        public SkalDuTaUtForeldrepenger getSkalDuTaUtForeldrepenger() {
            return skalDuTaUtForeldrepenger;
        }

        public void setSkalDuTaUtForeldrepenger(SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger) {
            this.skalDuTaUtForeldrepenger = skalDuTaUtForeldrepenger;
        }

        public HvaSkalMorGjoreIDennePerioden getHvaSkalMorGjoreIDennePerioden() {
            return hvaSkalMorGjoreIDennePerioden;
        }

        public void setHvaSkalMorGjoreIDennePerioden(HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden) {
            this.hvaSkalMorGjoreIDennePerioden = hvaSkalMorGjoreIDennePerioden;
        }

        public HvaSkalMorGjoreIDennePeriodenOpphold getHvaSkalMorGjoreIDennePeriodenOpphold() {
            return hvaSkalMorGjoreIDennePeriodenOpphold;
        }

        public void setHvaSkalMorGjoreIDennePeriodenOpphold(HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold) {
            this.hvaSkalMorGjoreIDennePeriodenOpphold = hvaSkalMorGjoreIDennePeriodenOpphold;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class KunMedmorRettRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoFraOgMedDdMmAaaa1;

        private SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger;

        private HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden;

        private HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public KunMedmorRettRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa() {
            return datoFraOgMedDdMmAaaa;
        }

        public void setDatoFraOgMedDdMmAaaa(LocalDate datoFraOgMedDdMmAaaa) {
            this.datoFraOgMedDdMmAaaa = datoFraOgMedDdMmAaaa;
        }

        public LocalDate getDatoFraOgMedDdMmAaaa1() {
            return datoFraOgMedDdMmAaaa1;
        }

        public void setDatoFraOgMedDdMmAaaa1(LocalDate datoFraOgMedDdMmAaaa1) {
            this.datoFraOgMedDdMmAaaa1 = datoFraOgMedDdMmAaaa1;
        }

        public SkalDuTaUtForeldrepenger getSkalDuTaUtForeldrepenger() {
            return skalDuTaUtForeldrepenger;
        }

        public void setSkalDuTaUtForeldrepenger(SkalDuTaUtForeldrepenger skalDuTaUtForeldrepenger) {
            this.skalDuTaUtForeldrepenger = skalDuTaUtForeldrepenger;
        }

        public HvaSkalMorGjoreIDennePerioden getHvaSkalMorGjoreIDennePerioden() {
            return hvaSkalMorGjoreIDennePerioden;
        }

        public void setHvaSkalMorGjoreIDennePerioden(HvaSkalMorGjoreIDennePerioden hvaSkalMorGjoreIDennePerioden) {
            this.hvaSkalMorGjoreIDennePerioden = hvaSkalMorGjoreIDennePerioden;
        }

        public HvaSkalMorGjoreIDennePeriodenOpphold getHvaSkalMorGjoreIDennePeriodenOpphold() {
            return hvaSkalMorGjoreIDennePeriodenOpphold;
        }

        public void setHvaSkalMorGjoreIDennePeriodenOpphold(HvaSkalMorGjoreIDennePeriodenOpphold hvaSkalMorGjoreIDennePeriodenOpphold) {
            this.hvaSkalMorGjoreIDennePeriodenOpphold = hvaSkalMorGjoreIDennePeriodenOpphold;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class PeriodeMedforeldrepengerVedAleneomsorgRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoTilOgMedDdMmAaaa;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public PeriodeMedforeldrepengerVedAleneomsorgRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa() {
            return datoFraOgMedDdMmAaaa;
        }

        public void setDatoFraOgMedDdMmAaaa(LocalDate datoFraOgMedDdMmAaaa) {
            this.datoFraOgMedDdMmAaaa = datoFraOgMedDdMmAaaa;
        }

        public LocalDate getDatoTilOgMedDdMmAaaa() {
            return datoTilOgMedDdMmAaaa;
        }

        public void setDatoTilOgMedDdMmAaaa(LocalDate datoTilOgMedDdMmAaaa) {
            this.datoTilOgMedDdMmAaaa = datoTilOgMedDdMmAaaa;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeid() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeid(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeid) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeid = skalDuKombinereForeldrepengeneMedDelvisArbeid;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class JegSokerOmAOvertaKvotenTilDenAndreForelderenRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoTilOgMedDdMmAaaa;

        private HvorforSkalDuOvertaKvoten hvorforSkalDuOvertaKvoten;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbe hvorSkalDuJobbe;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbe {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbe() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public JegSokerOmAOvertaKvotenTilDenAndreForelderenRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa() {
            return datoFraOgMedDdMmAaaa;
        }

        public void setDatoFraOgMedDdMmAaaa(LocalDate datoFraOgMedDdMmAaaa) {
            this.datoFraOgMedDdMmAaaa = datoFraOgMedDdMmAaaa;
        }

        public LocalDate getDatoTilOgMedDdMmAaaa() {
            return datoTilOgMedDdMmAaaa;
        }

        public void setDatoTilOgMedDdMmAaaa(LocalDate datoTilOgMedDdMmAaaa) {
            this.datoTilOgMedDdMmAaaa = datoTilOgMedDdMmAaaa;
        }

        public HvorforSkalDuOvertaKvoten getHvorforSkalDuOvertaKvoten() {
            return hvorforSkalDuOvertaKvoten;
        }

        public void setHvorforSkalDuOvertaKvoten(HvorforSkalDuOvertaKvoten hvorforSkalDuOvertaKvoten) {
            this.hvorforSkalDuOvertaKvoten = hvorforSkalDuOvertaKvoten;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote = skalDuKombinereForeldrepengeneMedDelvisArbeidOvertaKvote;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbe getHvorSkalDuJobbe() {
            return hvorSkalDuJobbe;
        }

        public void setHvorSkalDuJobbe(HvorSkalDuJobbe hvorSkalDuJobbe) {
            this.hvorSkalDuJobbe = hvorSkalDuJobbe;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class PeriodeMedforeldrepengerRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoTilOgMedDdMmAaaa;

        private JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        private HvorSkalDuJobbeOvertattForeldreansvaret hvorSkalDuJobbeOvertattForeldreansvaret;

        private String navnPaArbeidsgiver;

        public static class HvorSkalDuJobbeOvertattForeldreansvaret {

            private Boolean hosArbeidsgiver;

            private Boolean frilanser;

            private Boolean selvstendigNaeringsdrivende;

            public HvorSkalDuJobbeOvertattForeldreansvaret() {}

            public Boolean isHosArbeidsgiver() {
                return hosArbeidsgiver;
            }

            public void setHosArbeidsgiver(Boolean hosArbeidsgiver) {
                this.hosArbeidsgiver = hosArbeidsgiver;
            }

            public Boolean isFrilanser() {
                return frilanser;
            }

            public void setFrilanser(Boolean frilanser) {
                this.frilanser = frilanser;
            }

            public Boolean isSelvstendigNaeringsdrivende() {
                return selvstendigNaeringsdrivende;
            }

            public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
                this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
            }

        }

        public PeriodeMedforeldrepengerRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa() {
            return datoFraOgMedDdMmAaaa;
        }

        public void setDatoFraOgMedDdMmAaaa(LocalDate datoFraOgMedDdMmAaaa) {
            this.datoFraOgMedDdMmAaaa = datoFraOgMedDdMmAaaa;
        }

        public LocalDate getDatoTilOgMedDdMmAaaa() {
            return datoTilOgMedDdMmAaaa;
        }

        public void setDatoTilOgMedDdMmAaaa(LocalDate datoTilOgMedDdMmAaaa) {
            this.datoTilOgMedDdMmAaaa = datoTilOgMedDdMmAaaa;
        }

        public JaNei getSkalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret() {
            return skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret;
        }

        public void setSkalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret(JaNei skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret) {
            this.skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret = skalDuKombinereForeldrepengeneMedDelvisArbeidOvertattForeldreansvaret;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

        public HvorSkalDuJobbeOvertattForeldreansvaret getHvorSkalDuJobbeOvertattForeldreansvaret() {
            return hvorSkalDuJobbeOvertattForeldreansvaret;
        }

        public void setHvorSkalDuJobbeOvertattForeldreansvaret(HvorSkalDuJobbeOvertattForeldreansvaret hvorSkalDuJobbeOvertattForeldreansvaret) {
            this.hvorSkalDuJobbeOvertattForeldreansvaret = hvorSkalDuJobbeOvertattForeldreansvaret;
        }

        public String getNavnPaArbeidsgiver() {
            return navnPaArbeidsgiver;
        }

        public void setNavnPaArbeidsgiver(String navnPaArbeidsgiver) {
            this.navnPaArbeidsgiver = navnPaArbeidsgiver;
        }

    }

    public static class LeggTilUtenlandsoppholdForDeNeste12ManedeneRow {

        private LocalDate fraOgMedDdMmAaaa;

        private LocalDate tilOgMedDdMmAaaa;

        private String hvilketLandSkalDuBoI;

        public LeggTilUtenlandsoppholdForDeNeste12ManedeneRow() {}

        public LocalDate getFraOgMedDdMmAaaa() {
            return fraOgMedDdMmAaaa;
        }

        public void setFraOgMedDdMmAaaa(LocalDate fraOgMedDdMmAaaa) {
            this.fraOgMedDdMmAaaa = fraOgMedDdMmAaaa;
        }

        public LocalDate getTilOgMedDdMmAaaa() {
            return tilOgMedDdMmAaaa;
        }

        public void setTilOgMedDdMmAaaa(LocalDate tilOgMedDdMmAaaa) {
            this.tilOgMedDdMmAaaa = tilOgMedDdMmAaaa;
        }

        public String getHvilketLandSkalDuBoI() {
            return hvilketLandSkalDuBoI;
        }

        public void setHvilketLandSkalDuBoI(String hvilketLandSkalDuBoI) {
            this.hvilketLandSkalDuBoI = hvilketLandSkalDuBoI;
        }

    }

    public static class LeggTilUtenlandsoppholdForDeSiste12ManedeneRow {

        private LocalDate fraOgMedDdMmAaaa;

        private LocalDate tilOgMedDdMmAaaa;

        private String hvilketLandBoddeDuI;

        public LeggTilUtenlandsoppholdForDeSiste12ManedeneRow() {}

        public LocalDate getFraOgMedDdMmAaaa() {
            return fraOgMedDdMmAaaa;
        }

        public void setFraOgMedDdMmAaaa(LocalDate fraOgMedDdMmAaaa) {
            this.fraOgMedDdMmAaaa = fraOgMedDdMmAaaa;
        }

        public LocalDate getTilOgMedDdMmAaaa() {
            return tilOgMedDdMmAaaa;
        }

        public void setTilOgMedDdMmAaaa(LocalDate tilOgMedDdMmAaaa) {
            this.tilOgMedDdMmAaaa = tilOgMedDdMmAaaa;
        }

        public String getHvilketLandBoddeDuI() {
            return hvilketLandBoddeDuI;
        }

        public void setHvilketLandBoddeDuI(String hvilketLandBoddeDuI) {
            this.hvilketLandBoddeDuI = hvilketLandBoddeDuI;
        }

    }

    public static class Frilanser {

        private LocalDate narStartetDuSomFrilanserDdMmAaaa;

        private JaNei jobberDuFortsattSomFrilanser;

        private LocalDate sluttdatoSomFrilanserDdMmAaaa;

        public Frilanser() {}

        public LocalDate getNarStartetDuSomFrilanserDdMmAaaa() {
            return narStartetDuSomFrilanserDdMmAaaa;
        }

        public void setNarStartetDuSomFrilanserDdMmAaaa(LocalDate narStartetDuSomFrilanserDdMmAaaa) {
            this.narStartetDuSomFrilanserDdMmAaaa = narStartetDuSomFrilanserDdMmAaaa;
        }

        public JaNei getJobberDuFortsattSomFrilanser() {
            return jobberDuFortsattSomFrilanser;
        }

        public void setJobberDuFortsattSomFrilanser(JaNei jobberDuFortsattSomFrilanser) {
            this.jobberDuFortsattSomFrilanser = jobberDuFortsattSomFrilanser;
        }

        public LocalDate getSluttdatoSomFrilanserDdMmAaaa() {
            return sluttdatoSomFrilanserDdMmAaaa;
        }

        public void setSluttdatoSomFrilanserDdMmAaaa(LocalDate sluttdatoSomFrilanserDdMmAaaa) {
            this.sluttdatoSomFrilanserDdMmAaaa = sluttdatoSomFrilanserDdMmAaaa;
        }

    }

    public static class VarigEndring {

        private LocalDate datoForEndringenDdMmAaaa;

        private BigDecimal naeringsinntektenDinEtterEndringen;

        private String skrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin;

        public VarigEndring() {}

        public LocalDate getDatoForEndringenDdMmAaaa() {
            return datoForEndringenDdMmAaaa;
        }

        public void setDatoForEndringenDdMmAaaa(LocalDate datoForEndringenDdMmAaaa) {
            this.datoForEndringenDdMmAaaa = datoForEndringenDdMmAaaa;
        }

        public BigDecimal getNaeringsinntektenDinEtterEndringen() {
            return naeringsinntektenDinEtterEndringen;
        }

        public void setNaeringsinntektenDinEtterEndringen(BigDecimal naeringsinntektenDinEtterEndringen) {
            this.naeringsinntektenDinEtterEndringen = naeringsinntektenDinEtterEndringen;
        }

        public String getSkrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin() {
            return skrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin;
        }

        public void setSkrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin(String skrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin) {
            this.skrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin = skrivKortHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin;
        }

    }

    public static class AndreInntektskilder {

        private JaNei harDuHattAndreInntektskilderDeSiste10Manedene;

        private List<LeggTilInntektskildeRow> leggTilInntektskilde;

        public static class LeggTilInntektskildeRow {

            private VelgInntektstype velgInntektstype;

            private LocalDate fraOgMedDdMmAaaa;

            private LocalDate tilOgMedDdMmAaaa;

            private Boolean pagaende;

            private String landDuJobberI;

            private String navnetPaArbeidsgiveren;

            public LeggTilInntektskildeRow() {}

            public VelgInntektstype getVelgInntektstype() {
                return velgInntektstype;
            }

            public void setVelgInntektstype(VelgInntektstype velgInntektstype) {
                this.velgInntektstype = velgInntektstype;
            }

            public LocalDate getFraOgMedDdMmAaaa() {
                return fraOgMedDdMmAaaa;
            }

            public void setFraOgMedDdMmAaaa(LocalDate fraOgMedDdMmAaaa) {
                this.fraOgMedDdMmAaaa = fraOgMedDdMmAaaa;
            }

            public LocalDate getTilOgMedDdMmAaaa() {
                return tilOgMedDdMmAaaa;
            }

            public void setTilOgMedDdMmAaaa(LocalDate tilOgMedDdMmAaaa) {
                this.tilOgMedDdMmAaaa = tilOgMedDdMmAaaa;
            }

            public Boolean isPagaende() {
                return pagaende;
            }

            public void setPagaende(Boolean pagaende) {
                this.pagaende = pagaende;
            }

            public String getLandDuJobberI() {
                return landDuJobberI;
            }

            public void setLandDuJobberI(String landDuJobberI) {
                this.landDuJobberI = landDuJobberI;
            }

            public String getNavnetPaArbeidsgiveren() {
                return navnetPaArbeidsgiveren;
            }

            public void setNavnetPaArbeidsgiveren(String navnetPaArbeidsgiveren) {
                this.navnetPaArbeidsgiveren = navnetPaArbeidsgiveren;
            }

        }

        public AndreInntektskilder() {}

        public JaNei getHarDuHattAndreInntektskilderDeSiste10Manedene() {
            return harDuHattAndreInntektskilderDeSiste10Manedene;
        }

        public void setHarDuHattAndreInntektskilderDeSiste10Manedene(JaNei harDuHattAndreInntektskilderDeSiste10Manedene) {
            this.harDuHattAndreInntektskilderDeSiste10Manedene = harDuHattAndreInntektskilderDeSiste10Manedene;
        }

        public List<LeggTilInntektskildeRow> getLeggTilInntektskilde() {
            return leggTilInntektskilde;
        }

        public void setLeggTilInntektskilde(List<LeggTilInntektskildeRow> leggTilInntektskilde) {
            this.leggTilInntektskilde = leggTilInntektskilde;
        }

    }

    public Nav140506Data() {}

    public Boolean isJegBekrefterAtJegHarLestOgForstattMinePlikter() {
        return jegBekrefterAtJegHarLestOgForstattMinePlikter;
    }

    public void setJegBekrefterAtJegHarLestOgForstattMinePlikter(Boolean jegBekrefterAtJegHarLestOgForstattMinePlikter) {
        this.jegBekrefterAtJegHarLestOgForstattMinePlikter = jegBekrefterAtJegHarLestOgForstattMinePlikter;
    }

    public DineOpplysninger1 getDineOpplysninger1() {
        return dineOpplysninger1;
    }

    public void setDineOpplysninger1(DineOpplysninger1 dineOpplysninger1) {
        this.dineOpplysninger1 = dineOpplysninger1;
    }

    public HvemErDu getHvemErDu() {
        return hvemErDu;
    }

    public void setHvemErDu(HvemErDu hvemErDu) {
        this.hvemErDu = hvemErDu;
    }

    public HvorLangPeriodeMedForeldrepengerOnskerDu getHvorLangPeriodeMedForeldrepengerOnskerDu() {
        return hvorLangPeriodeMedForeldrepengerOnskerDu;
    }

    public void setHvorLangPeriodeMedForeldrepengerOnskerDu(HvorLangPeriodeMedForeldrepengerOnskerDu hvorLangPeriodeMedForeldrepengerOnskerDu) {
        this.hvorLangPeriodeMedForeldrepengerOnskerDu = hvorLangPeriodeMedForeldrepengerOnskerDu;
    }

    public JaNei getGjelderSoknadenDinStebarnsadopsjon() {
        return gjelderSoknadenDinStebarnsadopsjon;
    }

    public void setGjelderSoknadenDinStebarnsadopsjon(JaNei gjelderSoknadenDinStebarnsadopsjon) {
        this.gjelderSoknadenDinStebarnsadopsjon = gjelderSoknadenDinStebarnsadopsjon;
    }

    public LocalDate getOppgiDatoenForStebarnsadopsjonDdMmAaaa() {
        return oppgiDatoenForStebarnsadopsjonDdMmAaaa;
    }

    public void setOppgiDatoenForStebarnsadopsjonDdMmAaaa(LocalDate oppgiDatoenForStebarnsadopsjonDdMmAaaa) {
        this.oppgiDatoenForStebarnsadopsjonDdMmAaaa = oppgiDatoenForStebarnsadopsjonDdMmAaaa;
    }

    public LocalDate getNarOvertarDuOmsorgenDdMmAaaa() {
        return narOvertarDuOmsorgenDdMmAaaa;
    }

    public void setNarOvertarDuOmsorgenDdMmAaaa(LocalDate narOvertarDuOmsorgenDdMmAaaa) {
        this.narOvertarDuOmsorgenDdMmAaaa = narOvertarDuOmsorgenDdMmAaaa;
    }

    public LocalDate getDatoForOmsorgsovertakelseDdMmAaaa() {
        return datoForOmsorgsovertakelseDdMmAaaa;
    }

    public void setDatoForOmsorgsovertakelseDdMmAaaa(LocalDate datoForOmsorgsovertakelseDdMmAaaa) {
        this.datoForOmsorgsovertakelseDdMmAaaa = datoForOmsorgsovertakelseDdMmAaaa;
    }

    public Integer getHvorMangeBarnSkalDuAdoptere() {
        return hvorMangeBarnSkalDuAdoptere;
    }

    public void setHvorMangeBarnSkalDuAdoptere(Integer hvorMangeBarnSkalDuAdoptere) {
        this.hvorMangeBarnSkalDuAdoptere = hvorMangeBarnSkalDuAdoptere;
    }

    public Integer getHvorMangeBarnOvertarDuOmsorgenFor() {
        return hvorMangeBarnOvertarDuOmsorgenFor;
    }

    public void setHvorMangeBarnOvertarDuOmsorgenFor(Integer hvorMangeBarnOvertarDuOmsorgenFor) {
        this.hvorMangeBarnOvertarDuOmsorgenFor = hvorMangeBarnOvertarDuOmsorgenFor;
    }

    public LocalDate getNarBleDetEldsteBarnetFodtDdMmAaaa() {
        return narBleDetEldsteBarnetFodtDdMmAaaa;
    }

    public void setNarBleDetEldsteBarnetFodtDdMmAaaa(LocalDate narBleDetEldsteBarnetFodtDdMmAaaa) {
        this.narBleDetEldsteBarnetFodtDdMmAaaa = narBleDetEldsteBarnetFodtDdMmAaaa;
    }

    public LocalDate getNarBleDetEldsteBarnetFodtDdMmAaaa1() {
        return narBleDetEldsteBarnetFodtDdMmAaaa1;
    }

    public void setNarBleDetEldsteBarnetFodtDdMmAaaa1(LocalDate narBleDetEldsteBarnetFodtDdMmAaaa1) {
        this.narBleDetEldsteBarnetFodtDdMmAaaa1 = narBleDetEldsteBarnetFodtDdMmAaaa1;
    }

    public JaNei getKanDuGiOssNavnetPaDenAndreForelderen() {
        return kanDuGiOssNavnetPaDenAndreForelderen;
    }

    public void setKanDuGiOssNavnetPaDenAndreForelderen(JaNei kanDuGiOssNavnetPaDenAndreForelderen) {
        this.kanDuGiOssNavnetPaDenAndreForelderen = kanDuGiOssNavnetPaDenAndreForelderen;
    }

    public String getFornavn() {
        return fornavn;
    }

    public void setFornavn(String fornavn) {
        this.fornavn = fornavn;
    }

    public String getEtternavn() {
        return etternavn;
    }

    public void setEtternavn(String etternavn) {
        this.etternavn = etternavn;
    }

    public JaNei getHarDenAndreForelderenNorskFodselsnummerEllerDNummer() {
        return harDenAndreForelderenNorskFodselsnummerEllerDNummer;
    }

    public void setHarDenAndreForelderenNorskFodselsnummerEllerDNummer(JaNei harDenAndreForelderenNorskFodselsnummerEllerDNummer) {
        this.harDenAndreForelderenNorskFodselsnummerEllerDNummer = harDenAndreForelderenNorskFodselsnummerEllerDNummer;
    }

    public String getHvaErDenAndreForelderensFodselsnummerEllerDNummer() {
        return hvaErDenAndreForelderensFodselsnummerEllerDNummer;
    }

    public void setHvaErDenAndreForelderensFodselsnummerEllerDNummer(String hvaErDenAndreForelderensFodselsnummerEllerDNummer) {
        this.hvaErDenAndreForelderensFodselsnummerEllerDNummer = hvaErDenAndreForelderensFodselsnummerEllerDNummer;
    }

    public String getHvaErDenAndreForelderensUtenlandskeFodselsnummer() {
        return hvaErDenAndreForelderensUtenlandskeFodselsnummer;
    }

    public void setHvaErDenAndreForelderensUtenlandskeFodselsnummer(String hvaErDenAndreForelderensUtenlandskeFodselsnummer) {
        this.hvaErDenAndreForelderensUtenlandskeFodselsnummer = hvaErDenAndreForelderensUtenlandskeFodselsnummer;
    }

    public String getHvorBorDenAndreForelderen() {
        return hvorBorDenAndreForelderen;
    }

    public void setHvorBorDenAndreForelderen(String hvorBorDenAndreForelderen) {
        this.hvorBorDenAndreForelderen = hvorBorDenAndreForelderen;
    }

    public JaNei getErDuAleneOmOmsorgenAvBarnet() {
        return erDuAleneOmOmsorgenAvBarnet;
    }

    public void setErDuAleneOmOmsorgenAvBarnet(JaNei erDuAleneOmOmsorgenAvBarnet) {
        this.erDuAleneOmOmsorgenAvBarnet = erDuAleneOmOmsorgenAvBarnet;
    }

    public JaNei getHarDenAndreForelderenRettTilForeldrepenger() {
        return harDenAndreForelderenRettTilForeldrepenger;
    }

    public void setHarDenAndreForelderenRettTilForeldrepenger(JaNei harDenAndreForelderenRettTilForeldrepenger) {
        this.harDenAndreForelderenRettTilForeldrepenger = harDenAndreForelderenRettTilForeldrepenger;
    }

    public JaNei getHarDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt() {
        return harDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt;
    }

    public void setHarDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt(JaNei harDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt) {
        this.harDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt = harDenAndreForelderenOppholdtSegFastIEtAnnetEosLandEnnNorgeEttArForBarnetBleFodt;
    }

    public JaNei getHarDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt() {
        return harDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt;
    }

    public void setHarDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt(JaNei harDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt) {
        this.harDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt = harDenAndreForelderenArbeidetEllerMottattPengestotteIEtEosLandIMinstSeksAvDeSisteTiManedeneForBarnetBleFodt;
    }

    public JaNei getHarDenAndreForelderenUforetrygd() {
        return harDenAndreForelderenUforetrygd;
    }

    public void setHarDenAndreForelderenUforetrygd(JaNei harDenAndreForelderenUforetrygd) {
        this.harDenAndreForelderenUforetrygd = harDenAndreForelderenUforetrygd;
    }

    public JaNei getHarDuOrientertDenAndreForelderenOmSoknadenDin() {
        return harDuOrientertDenAndreForelderenOmSoknadenDin;
    }

    public void setHarDuOrientertDenAndreForelderenOmSoknadenDin(JaNei harDuOrientertDenAndreForelderenOmSoknadenDin) {
        this.harDuOrientertDenAndreForelderenOmSoknadenDin = harDuOrientertDenAndreForelderenOmSoknadenDin;
    }

    public Mor getMor() {
        return mor;
    }

    public void setMor(Mor mor) {
        this.mor = mor;
    }

    public HvilkenPeriodeSkalDuTaUtFar getHvilkenPeriodeSkalDuTaUtFar() {
        return hvilkenPeriodeSkalDuTaUtFar;
    }

    public void setHvilkenPeriodeSkalDuTaUtFar(HvilkenPeriodeSkalDuTaUtFar hvilkenPeriodeSkalDuTaUtFar) {
        this.hvilkenPeriodeSkalDuTaUtFar = hvilkenPeriodeSkalDuTaUtFar;
    }

    public HvilkenPeriodeSkalDuTaUtMedmor getHvilkenPeriodeSkalDuTaUtMedmor() {
        return hvilkenPeriodeSkalDuTaUtMedmor;
    }

    public void setHvilkenPeriodeSkalDuTaUtMedmor(HvilkenPeriodeSkalDuTaUtMedmor hvilkenPeriodeSkalDuTaUtMedmor) {
        this.hvilkenPeriodeSkalDuTaUtMedmor = hvilkenPeriodeSkalDuTaUtMedmor;
    }

    public List<ModrekvoteRow> getModrekvote() {
        return modrekvote;
    }

    public void setModrekvote(List<ModrekvoteRow> modrekvote) {
        this.modrekvote = modrekvote;
    }

    public List<FedrekvoteRow> getFedrekvote() {
        return fedrekvote;
    }

    public void setFedrekvote(List<FedrekvoteRow> fedrekvote) {
        this.fedrekvote = fedrekvote;
    }

    public List<MedmorkvoteRow> getMedmorkvote() {
        return medmorkvote;
    }

    public void setMedmorkvote(List<MedmorkvoteRow> medmorkvote) {
        this.medmorkvote = medmorkvote;
    }

    public List<FellesperiodeMorRow> getFellesperiodeMor() {
        return fellesperiodeMor;
    }

    public void setFellesperiodeMor(List<FellesperiodeMorRow> fellesperiodeMor) {
        this.fellesperiodeMor = fellesperiodeMor;
    }

    public List<FellesperiodeFarMedmorRow> getFellesperiodeFarMedmor() {
        return fellesperiodeFarMedmor;
    }

    public void setFellesperiodeFarMedmor(List<FellesperiodeFarMedmorRow> fellesperiodeFarMedmor) {
        this.fellesperiodeFarMedmor = fellesperiodeFarMedmor;
    }

    public List<KunMorRettRow> getKunMorRett() {
        return kunMorRett;
    }

    public void setKunMorRett(List<KunMorRettRow> kunMorRett) {
        this.kunMorRett = kunMorRett;
    }

    public List<KunFarRettRow> getKunFarRett() {
        return kunFarRett;
    }

    public void setKunFarRett(List<KunFarRettRow> kunFarRett) {
        this.kunFarRett = kunFarRett;
    }

    public List<KunMedmorRettRow> getKunMedmorRett() {
        return kunMedmorRett;
    }

    public void setKunMedmorRett(List<KunMedmorRettRow> kunMedmorRett) {
        this.kunMedmorRett = kunMedmorRett;
    }

    public List<PeriodeMedforeldrepengerVedAleneomsorgRow> getPeriodeMedforeldrepengerVedAleneomsorg() {
        return periodeMedforeldrepengerVedAleneomsorg;
    }

    public void setPeriodeMedforeldrepengerVedAleneomsorg(List<PeriodeMedforeldrepengerVedAleneomsorgRow> periodeMedforeldrepengerVedAleneomsorg) {
        this.periodeMedforeldrepengerVedAleneomsorg = periodeMedforeldrepengerVedAleneomsorg;
    }

    public List<JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> getJegSokerOmAOvertaKvotenTilDenAndreForelderen() {
        return jegSokerOmAOvertaKvotenTilDenAndreForelderen;
    }

    public void setJegSokerOmAOvertaKvotenTilDenAndreForelderen(List<JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> jegSokerOmAOvertaKvotenTilDenAndreForelderen) {
        this.jegSokerOmAOvertaKvotenTilDenAndreForelderen = jegSokerOmAOvertaKvotenTilDenAndreForelderen;
    }

    public List<PeriodeMedforeldrepengerRow> getPeriodeMedforeldrepenger() {
        return periodeMedforeldrepenger;
    }

    public void setPeriodeMedforeldrepenger(List<PeriodeMedforeldrepengerRow> periodeMedforeldrepenger) {
        this.periodeMedforeldrepenger = periodeMedforeldrepenger;
    }

    public HvorSkalDuBoDeNeste12Manedene getHvorSkalDuBoDeNeste12Manedene() {
        return hvorSkalDuBoDeNeste12Manedene;
    }

    public void setHvorSkalDuBoDeNeste12Manedene(HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene) {
        this.hvorSkalDuBoDeNeste12Manedene = hvorSkalDuBoDeNeste12Manedene;
    }

    public List<LeggTilUtenlandsoppholdForDeNeste12ManedeneRow> getLeggTilUtenlandsoppholdForDeNeste12Manedene() {
        return leggTilUtenlandsoppholdForDeNeste12Manedene;
    }

    public void setLeggTilUtenlandsoppholdForDeNeste12Manedene(List<LeggTilUtenlandsoppholdForDeNeste12ManedeneRow> leggTilUtenlandsoppholdForDeNeste12Manedene) {
        this.leggTilUtenlandsoppholdForDeNeste12Manedene = leggTilUtenlandsoppholdForDeNeste12Manedene;
    }

    public HvorHarDuBoddDeSiste12Manedene getHvorHarDuBoddDeSiste12Manedene() {
        return hvorHarDuBoddDeSiste12Manedene;
    }

    public void setHvorHarDuBoddDeSiste12Manedene(HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene) {
        this.hvorHarDuBoddDeSiste12Manedene = hvorHarDuBoddDeSiste12Manedene;
    }

    public List<LeggTilUtenlandsoppholdForDeSiste12ManedeneRow> getLeggTilUtenlandsoppholdForDeSiste12Manedene() {
        return leggTilUtenlandsoppholdForDeSiste12Manedene;
    }

    public void setLeggTilUtenlandsoppholdForDeSiste12Manedene(List<LeggTilUtenlandsoppholdForDeSiste12ManedeneRow> leggTilUtenlandsoppholdForDeSiste12Manedene) {
        this.leggTilUtenlandsoppholdForDeSiste12Manedene = leggTilUtenlandsoppholdForDeSiste12Manedene;
    }

    public JaNei getHarDuArbeidsforholdINorge() {
        return harDuArbeidsforholdINorge;
    }

    public void setHarDuArbeidsforholdINorge(JaNei harDuArbeidsforholdINorge) {
        this.harDuArbeidsforholdINorge = harDuArbeidsforholdINorge;
    }

    public JaNei getHarDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene() {
        return harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene;
    }

    public void setHarDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene(JaNei harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene) {
        this.harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene = harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene;
    }

    public Frilanser getFrilanser() {
        return frilanser;
    }

    public void setFrilanser(Frilanser frilanser) {
        this.frilanser = frilanser;
    }

    public JaNei getHarDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene() {
        return harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene;
    }

    public void setHarDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene(JaNei harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene) {
        this.harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene = harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene;
    }

    public HvilkenTypeVirksomhetDriverDu getHvilkenTypeVirksomhetDriverDu() {
        return hvilkenTypeVirksomhetDriverDu;
    }

    public void setHvilkenTypeVirksomhetDriverDu(HvilkenTypeVirksomhetDriverDu hvilkenTypeVirksomhetDriverDu) {
        this.hvilkenTypeVirksomhetDriverDu = hvilkenTypeVirksomhetDriverDu;
    }

    public String getHvaHeterVirksomhetenDin() {
        return hvaHeterVirksomhetenDin;
    }

    public void setHvaHeterVirksomhetenDin(String hvaHeterVirksomhetenDin) {
        this.hvaHeterVirksomhetenDin = hvaHeterVirksomhetenDin;
    }

    public LocalDate getNarStartetDuVirksomhetenDdMmAaaa() {
        return narStartetDuVirksomhetenDdMmAaaa;
    }

    public void setNarStartetDuVirksomhetenDdMmAaaa(LocalDate narStartetDuVirksomhetenDdMmAaaa) {
        this.narStartetDuVirksomhetenDdMmAaaa = narStartetDuVirksomhetenDdMmAaaa;
    }

    public JaNei getErVirksomhetenDinRegistrertINorge() {
        return erVirksomhetenDinRegistrertINorge;
    }

    public void setErVirksomhetenDinRegistrertINorge(JaNei erVirksomhetenDinRegistrertINorge) {
        this.erVirksomhetenDinRegistrertINorge = erVirksomhetenDinRegistrertINorge;
    }

    public String getVirksomhetensOrganisasjonsnummer1() {
        return virksomhetensOrganisasjonsnummer1;
    }

    public void setVirksomhetensOrganisasjonsnummer1(String virksomhetensOrganisasjonsnummer1) {
        this.virksomhetensOrganisasjonsnummer1 = virksomhetensOrganisasjonsnummer1;
    }

    public String getHvilketLandErVirksomhetenRegistrertI() {
        return hvilketLandErVirksomhetenRegistrertI;
    }

    public void setHvilketLandErVirksomhetenRegistrertI(String hvilketLandErVirksomhetenRegistrertI) {
        this.hvilketLandErVirksomhetenRegistrertI = hvilketLandErVirksomhetenRegistrertI;
    }

    public JaNei getErDuFremdelesSelvstendigNaeringsdrivende() {
        return erDuFremdelesSelvstendigNaeringsdrivende;
    }

    public void setErDuFremdelesSelvstendigNaeringsdrivende(JaNei erDuFremdelesSelvstendigNaeringsdrivende) {
        this.erDuFremdelesSelvstendigNaeringsdrivende = erDuFremdelesSelvstendigNaeringsdrivende;
    }

    public LocalDate getDatoForAvsluttetNaeringsdriftDdMmAaaa() {
        return datoForAvsluttetNaeringsdriftDdMmAaaa;
    }

    public void setDatoForAvsluttetNaeringsdriftDdMmAaaa(LocalDate datoForAvsluttetNaeringsdriftDdMmAaaa) {
        this.datoForAvsluttetNaeringsdriftDdMmAaaa = datoForAvsluttetNaeringsdriftDdMmAaaa;
    }

    public HvorLengeHarDuVaertSelvstendigNaeringsdrivende getHvorLengeHarDuVaertSelvstendigNaeringsdrivende() {
        return hvorLengeHarDuVaertSelvstendigNaeringsdrivende;
    }

    public void setHvorLengeHarDuVaertSelvstendigNaeringsdrivende(HvorLengeHarDuVaertSelvstendigNaeringsdrivende hvorLengeHarDuVaertSelvstendigNaeringsdrivende) {
        this.hvorLengeHarDuVaertSelvstendigNaeringsdrivende = hvorLengeHarDuVaertSelvstendigNaeringsdrivende;
    }

    public BigDecimal getHvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten() {
        return hvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten;
    }

    public void setHvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten(BigDecimal hvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten) {
        this.hvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten = hvaHarDuHattINaeringsresultatForSkattFraDuStartetOppVirksomheten;
    }

    public BigDecimal getHvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene() {
        return hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene;
    }

    public void setHvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene(BigDecimal hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene) {
        this.hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene = hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene;
    }

    public JaNei getHarDuBlittYrkesaktivILopetAvDetSisteAret() {
        return harDuBlittYrkesaktivILopetAvDetSisteAret;
    }

    public void setHarDuBlittYrkesaktivILopetAvDetSisteAret(JaNei harDuBlittYrkesaktivILopetAvDetSisteAret) {
        this.harDuBlittYrkesaktivILopetAvDetSisteAret = harDuBlittYrkesaktivILopetAvDetSisteAret;
    }

    public JaNei getHarDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene() {
        return harDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene;
    }

    public void setHarDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene(JaNei harDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene) {
        this.harDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene = harDuBlittYrkesaktivILopetAvDe3SisteFerdiglignedeArene;
    }

    public LocalDate getNarBleDuYrkesaktivDdMmAaaa() {
        return narBleDuYrkesaktivDdMmAaaa;
    }

    public void setNarBleDuYrkesaktivDdMmAaaa(LocalDate narBleDuYrkesaktivDdMmAaaa) {
        this.narBleDuYrkesaktivDdMmAaaa = narBleDuYrkesaktivDdMmAaaa;
    }

    public JaNei getHarDuHattEnVarigEndringIArbeidsforholdetDitt() {
        return harDuHattEnVarigEndringIArbeidsforholdetDitt;
    }

    public void setHarDuHattEnVarigEndringIArbeidsforholdetDitt(JaNei harDuHattEnVarigEndringIArbeidsforholdetDitt) {
        this.harDuHattEnVarigEndringIArbeidsforholdetDitt = harDuHattEnVarigEndringIArbeidsforholdetDitt;
    }

    public VarigEndring getVarigEndring() {
        return varigEndring;
    }

    public void setVarigEndring(VarigEndring varigEndring) {
        this.varigEndring = varigEndring;
    }

    public AndreInntektskilder getAndreInntektskilder() {
        return andreInntektskilder;
    }

    public void setAndreInntektskilder(AndreInntektskilder andreInntektskilder) {
        this.andreInntektskilder = andreInntektskilder;
    }

}
