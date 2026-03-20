package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/** NAV 14-16.05 – Søknad om endring eller nytt uttak av foreldrepenger */
public class Nav141605Data {

    private Boolean jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2;

    private DineOpplysninger1 dineOpplysninger1;

    private HvemErDu hvemErDu;

    private JaNei erDuAleneOmOmsorgenAvBarnet;

    private BleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene bleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene;

    private JaNei harDenAndreForelderenRettTilForeldrepenger;

    private JaNei harDuOrientertDenAndreForelderenOmSoknadenDin;

    private HvaSokerDuOm hvaSokerDuOm;

    private HvaSokerDuOmIkkeMor hvaSokerDuOmIkkeMor;

    private Boolean jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger;

    private Boolean jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1;

    private Mor mor;

    private HvilkenPeriodeSkalDuTaUtFar hvilkenPeriodeSkalDuTaUtFar;

    private HvilkenPeriodeSkalDuTaUtMedmor hvilkenPeriodeSkalDuTaUtMedmor;

    private List<ForeldrepengerForFodselRow> foreldrepengerForFodsel;

    private List<ModrekvoteRow> modrekvote;

    private List<FedrekvoteRow> fedrekvote;

    private List<MedmorkvoteRow> medmorkvote;

    private List<FellesperiodeMorRow> fellesperiodeMor;

    private List<FellesperiodeFarMedmorRow> fellesperiodeFarMedmor;

    private List<KunMorRettRow> kunMorRett;

    private List<KunFarRettRow> kunFarRett;

    private List<KunMedmorRettRow> kunMedmorRett;

    private List<PeriodeMedforeldrepengerVedAleneomsorgRow> periodeMedforeldrepengerVedAleneomsorg;

    private List<PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow> periodeMedforeldrepengerVedAleneomsorgFarMedmor;

    private List<PeriodeMedforeldrepengerRow> periodeMedforeldrepenger;

    private List<JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> jegSokerOmAOvertaKvotenTilDenAndreForelderen;

    private List<HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow> hvilkenPeriodeSkalDuIkkeAllikevelTaUt;

    private List<PerioderMedUtsettelseForste6UkerEtterFodselRow> perioderMedUtsettelseForste6UkerEtterFodsel;

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

    public static class HvaSokerDuOm {

        private Boolean periodeMedForeldrepenger;

        private Boolean periodeUtenForeldrepenger;

        private Boolean utsettelseForste6UkeneEtterFodsel;

        public HvaSokerDuOm() {}

        public Boolean isPeriodeMedForeldrepenger() {
            return periodeMedForeldrepenger;
        }

        public void setPeriodeMedForeldrepenger(Boolean periodeMedForeldrepenger) {
            this.periodeMedForeldrepenger = periodeMedForeldrepenger;
        }

        public Boolean isPeriodeUtenForeldrepenger() {
            return periodeUtenForeldrepenger;
        }

        public void setPeriodeUtenForeldrepenger(Boolean periodeUtenForeldrepenger) {
            this.periodeUtenForeldrepenger = periodeUtenForeldrepenger;
        }

        public Boolean isUtsettelseForste6UkeneEtterFodsel() {
            return utsettelseForste6UkeneEtterFodsel;
        }

        public void setUtsettelseForste6UkeneEtterFodsel(Boolean utsettelseForste6UkeneEtterFodsel) {
            this.utsettelseForste6UkeneEtterFodsel = utsettelseForste6UkeneEtterFodsel;
        }

    }

    public static class HvaSokerDuOmIkkeMor {

        private Boolean periodeMedForeldrepenger;

        private Boolean periodeUtenForeldrepenger;

        public HvaSokerDuOmIkkeMor() {}

        public Boolean isPeriodeMedForeldrepenger() {
            return periodeMedForeldrepenger;
        }

        public void setPeriodeMedForeldrepenger(Boolean periodeMedForeldrepenger) {
            this.periodeMedForeldrepenger = periodeMedForeldrepenger;
        }

        public Boolean isPeriodeUtenForeldrepenger() {
            return periodeUtenForeldrepenger;
        }

        public void setPeriodeUtenForeldrepenger(Boolean periodeUtenForeldrepenger) {
            this.periodeUtenForeldrepenger = periodeUtenForeldrepenger;
        }

    }

    public static class Mor {

        private HvilkenPeriodeSkalDuTaUtAleneMor hvilkenPeriodeSkalDuTaUtAleneMor;

        private HvilkenPeriodeSkalDuTaUtKunMorRett hvilkenPeriodeSkalDuTaUtKunMorRett;

        private HvilkenPeriodeSkalDuTaUtMor hvilkenPeriodeSkalDuTaUtMor;

        public static class HvilkenPeriodeSkalDuTaUtAleneMor {

            private Boolean foreldrepengerForFodsel;

            private Boolean foreldrepengerVedAleneomsorg;

            public HvilkenPeriodeSkalDuTaUtAleneMor() {}

            public Boolean isForeldrepengerForFodsel() {
                return foreldrepengerForFodsel;
            }

            public void setForeldrepengerForFodsel(Boolean foreldrepengerForFodsel) {
                this.foreldrepengerForFodsel = foreldrepengerForFodsel;
            }

            public Boolean isForeldrepengerVedAleneomsorg() {
                return foreldrepengerVedAleneomsorg;
            }

            public void setForeldrepengerVedAleneomsorg(Boolean foreldrepengerVedAleneomsorg) {
                this.foreldrepengerVedAleneomsorg = foreldrepengerVedAleneomsorg;
            }

        }

        public static class HvilkenPeriodeSkalDuTaUtKunMorRett {

            private Boolean foreldrepengerForFodsel;

            private Boolean foreldrepengerKunMorRett;

            public HvilkenPeriodeSkalDuTaUtKunMorRett() {}

            public Boolean isForeldrepengerForFodsel() {
                return foreldrepengerForFodsel;
            }

            public void setForeldrepengerForFodsel(Boolean foreldrepengerForFodsel) {
                this.foreldrepengerForFodsel = foreldrepengerForFodsel;
            }

            public Boolean isForeldrepengerKunMorRett() {
                return foreldrepengerKunMorRett;
            }

            public void setForeldrepengerKunMorRett(Boolean foreldrepengerKunMorRett) {
                this.foreldrepengerKunMorRett = foreldrepengerKunMorRett;
            }

        }

        public static class HvilkenPeriodeSkalDuTaUtMor {

            private Boolean foreldrepengerForFodsel;

            private Boolean modrekvote;

            private Boolean fellesperiode;

            private Boolean overforingAvAnnenForeldersKvote;

            public HvilkenPeriodeSkalDuTaUtMor() {}

            public Boolean isForeldrepengerForFodsel() {
                return foreldrepengerForFodsel;
            }

            public void setForeldrepengerForFodsel(Boolean foreldrepengerForFodsel) {
                this.foreldrepengerForFodsel = foreldrepengerForFodsel;
            }

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

        public HvilkenPeriodeSkalDuTaUtAleneMor getHvilkenPeriodeSkalDuTaUtAleneMor() {
            return hvilkenPeriodeSkalDuTaUtAleneMor;
        }

        public void setHvilkenPeriodeSkalDuTaUtAleneMor(HvilkenPeriodeSkalDuTaUtAleneMor hvilkenPeriodeSkalDuTaUtAleneMor) {
            this.hvilkenPeriodeSkalDuTaUtAleneMor = hvilkenPeriodeSkalDuTaUtAleneMor;
        }

        public HvilkenPeriodeSkalDuTaUtKunMorRett getHvilkenPeriodeSkalDuTaUtKunMorRett() {
            return hvilkenPeriodeSkalDuTaUtKunMorRett;
        }

        public void setHvilkenPeriodeSkalDuTaUtKunMorRett(HvilkenPeriodeSkalDuTaUtKunMorRett hvilkenPeriodeSkalDuTaUtKunMorRett) {
            this.hvilkenPeriodeSkalDuTaUtKunMorRett = hvilkenPeriodeSkalDuTaUtKunMorRett;
        }

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

    public static class ForeldrepengerForFodselRow {

        private LocalDate foreldrepengerFraOgMedDatoDdMmAaaa;

        private LocalDate foreldrepengerTilOgMedDatoDdMmAaaa;

        public ForeldrepengerForFodselRow() {}

        public LocalDate getForeldrepengerFraOgMedDatoDdMmAaaa() {
            return foreldrepengerFraOgMedDatoDdMmAaaa;
        }

        public void setForeldrepengerFraOgMedDatoDdMmAaaa(LocalDate foreldrepengerFraOgMedDatoDdMmAaaa) {
            this.foreldrepengerFraOgMedDatoDdMmAaaa = foreldrepengerFraOgMedDatoDdMmAaaa;
        }

        public LocalDate getForeldrepengerTilOgMedDatoDdMmAaaa() {
            return foreldrepengerTilOgMedDatoDdMmAaaa;
        }

        public void setForeldrepengerTilOgMedDatoDdMmAaaa(LocalDate foreldrepengerTilOgMedDatoDdMmAaaa) {
            this.foreldrepengerTilOgMedDatoDdMmAaaa = foreldrepengerTilOgMedDatoDdMmAaaa;
        }

    }

    public static class ModrekvoteRow {

        private LocalDate modrekvoteFraOgMedDatoDdMmAaaa;

        private LocalDate modrekvoteTilOgMedDatoDdMmAaaa;

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

        public LocalDate getModrekvoteTilOgMedDatoDdMmAaaa() {
            return modrekvoteTilOgMedDatoDdMmAaaa;
        }

        public void setModrekvoteTilOgMedDatoDdMmAaaa(LocalDate modrekvoteTilOgMedDatoDdMmAaaa) {
            this.modrekvoteTilOgMedDatoDdMmAaaa = modrekvoteTilOgMedDatoDdMmAaaa;
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

    public static class PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow {

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

        public PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow() {}

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

    public static class JegSokerOmAOvertaKvotenTilDenAndreForelderenRow {

        private LocalDate datoFraOgMedDdMmAaaa;

        private LocalDate datoTilOgMedDdMmAaaa;

        private HvorforSkalDuOvertaKvoten hvorforSkalDuOvertaKvoten;

        private HvorforSkalDuOvertaKvoten1 hvorforSkalDuOvertaKvoten1;

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

        public HvorforSkalDuOvertaKvoten1 getHvorforSkalDuOvertaKvoten1() {
            return hvorforSkalDuOvertaKvoten1;
        }

        public void setHvorforSkalDuOvertaKvoten1(HvorforSkalDuOvertaKvoten1 hvorforSkalDuOvertaKvoten1) {
            this.hvorforSkalDuOvertaKvoten1 = hvorforSkalDuOvertaKvoten1;
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

    public static class HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow {

        private LocalDate datoFraOgMedDdMmAaaaUtenForeldrepenger;

        private LocalDate datoTilOgMedDdMmAaaa;

        public HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaaUtenForeldrepenger() {
            return datoFraOgMedDdMmAaaaUtenForeldrepenger;
        }

        public void setDatoFraOgMedDdMmAaaaUtenForeldrepenger(LocalDate datoFraOgMedDdMmAaaaUtenForeldrepenger) {
            this.datoFraOgMedDdMmAaaaUtenForeldrepenger = datoFraOgMedDdMmAaaaUtenForeldrepenger;
        }

        public LocalDate getDatoTilOgMedDdMmAaaa() {
            return datoTilOgMedDdMmAaaa;
        }

        public void setDatoTilOgMedDdMmAaaa(LocalDate datoTilOgMedDdMmAaaa) {
            this.datoTilOgMedDdMmAaaa = datoTilOgMedDdMmAaaa;
        }

    }

    public static class PerioderMedUtsettelseForste6UkerEtterFodselRow {

        private LocalDate datoFraOgMedDdMmAaaa1;

        private LocalDate datoTilOgMedDdMmAaaa;

        private HvorforSkalDuUtsetteForeldrepenger hvorforSkalDuUtsetteForeldrepenger;

        public PerioderMedUtsettelseForste6UkerEtterFodselRow() {}

        public LocalDate getDatoFraOgMedDdMmAaaa1() {
            return datoFraOgMedDdMmAaaa1;
        }

        public void setDatoFraOgMedDdMmAaaa1(LocalDate datoFraOgMedDdMmAaaa1) {
            this.datoFraOgMedDdMmAaaa1 = datoFraOgMedDdMmAaaa1;
        }

        public LocalDate getDatoTilOgMedDdMmAaaa() {
            return datoTilOgMedDdMmAaaa;
        }

        public void setDatoTilOgMedDdMmAaaa(LocalDate datoTilOgMedDdMmAaaa) {
            this.datoTilOgMedDdMmAaaa = datoTilOgMedDdMmAaaa;
        }

        public HvorforSkalDuUtsetteForeldrepenger getHvorforSkalDuUtsetteForeldrepenger() {
            return hvorforSkalDuUtsetteForeldrepenger;
        }

        public void setHvorforSkalDuUtsetteForeldrepenger(HvorforSkalDuUtsetteForeldrepenger hvorforSkalDuUtsetteForeldrepenger) {
            this.hvorforSkalDuUtsetteForeldrepenger = hvorforSkalDuUtsetteForeldrepenger;
        }

    }

    public Nav141605Data() {}

    public Boolean isJegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2() {
        return jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2;
    }

    public void setJegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2(Boolean jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2) {
        this.jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2 = jegVilSvareSaGodtJegKanPaSporsmaleneISoknaden2;
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

    public JaNei getErDuAleneOmOmsorgenAvBarnet() {
        return erDuAleneOmOmsorgenAvBarnet;
    }

    public void setErDuAleneOmOmsorgenAvBarnet(JaNei erDuAleneOmOmsorgenAvBarnet) {
        this.erDuAleneOmOmsorgenAvBarnet = erDuAleneOmOmsorgenAvBarnet;
    }

    public BleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene getBleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene() {
        return bleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene;
    }

    public void setBleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene(BleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene bleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene) {
        this.bleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene = bleDuAleneOmOmsorgenForEllerEtterOppstartAvForeldrepengene;
    }

    public JaNei getHarDenAndreForelderenRettTilForeldrepenger() {
        return harDenAndreForelderenRettTilForeldrepenger;
    }

    public void setHarDenAndreForelderenRettTilForeldrepenger(JaNei harDenAndreForelderenRettTilForeldrepenger) {
        this.harDenAndreForelderenRettTilForeldrepenger = harDenAndreForelderenRettTilForeldrepenger;
    }

    public JaNei getHarDuOrientertDenAndreForelderenOmSoknadenDin() {
        return harDuOrientertDenAndreForelderenOmSoknadenDin;
    }

    public void setHarDuOrientertDenAndreForelderenOmSoknadenDin(JaNei harDuOrientertDenAndreForelderenOmSoknadenDin) {
        this.harDuOrientertDenAndreForelderenOmSoknadenDin = harDuOrientertDenAndreForelderenOmSoknadenDin;
    }

    public HvaSokerDuOm getHvaSokerDuOm() {
        return hvaSokerDuOm;
    }

    public void setHvaSokerDuOm(HvaSokerDuOm hvaSokerDuOm) {
        this.hvaSokerDuOm = hvaSokerDuOm;
    }

    public HvaSokerDuOmIkkeMor getHvaSokerDuOmIkkeMor() {
        return hvaSokerDuOmIkkeMor;
    }

    public void setHvaSokerDuOmIkkeMor(HvaSokerDuOmIkkeMor hvaSokerDuOmIkkeMor) {
        this.hvaSokerDuOmIkkeMor = hvaSokerDuOmIkkeMor;
    }

    public Boolean isJegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger() {
        return jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger;
    }

    public void setJegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger(Boolean jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger) {
        this.jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger = jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger;
    }

    public Boolean isJegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1() {
        return jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1;
    }

    public void setJegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1(Boolean jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1) {
        this.jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1 = jegBekrefterAtjegSkalHaOmsorgenForBarnetIPeriodeneJegSokerForeldrepenger1;
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

    public List<ForeldrepengerForFodselRow> getForeldrepengerForFodsel() {
        return foreldrepengerForFodsel;
    }

    public void setForeldrepengerForFodsel(List<ForeldrepengerForFodselRow> foreldrepengerForFodsel) {
        this.foreldrepengerForFodsel = foreldrepengerForFodsel;
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

    public List<PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow> getPeriodeMedforeldrepengerVedAleneomsorgFarMedmor() {
        return periodeMedforeldrepengerVedAleneomsorgFarMedmor;
    }

    public void setPeriodeMedforeldrepengerVedAleneomsorgFarMedmor(List<PeriodeMedforeldrepengerVedAleneomsorgFarMedmorRow> periodeMedforeldrepengerVedAleneomsorgFarMedmor) {
        this.periodeMedforeldrepengerVedAleneomsorgFarMedmor = periodeMedforeldrepengerVedAleneomsorgFarMedmor;
    }

    public List<PeriodeMedforeldrepengerRow> getPeriodeMedforeldrepenger() {
        return periodeMedforeldrepenger;
    }

    public void setPeriodeMedforeldrepenger(List<PeriodeMedforeldrepengerRow> periodeMedforeldrepenger) {
        this.periodeMedforeldrepenger = periodeMedforeldrepenger;
    }

    public List<JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> getJegSokerOmAOvertaKvotenTilDenAndreForelderen() {
        return jegSokerOmAOvertaKvotenTilDenAndreForelderen;
    }

    public void setJegSokerOmAOvertaKvotenTilDenAndreForelderen(List<JegSokerOmAOvertaKvotenTilDenAndreForelderenRow> jegSokerOmAOvertaKvotenTilDenAndreForelderen) {
        this.jegSokerOmAOvertaKvotenTilDenAndreForelderen = jegSokerOmAOvertaKvotenTilDenAndreForelderen;
    }

    public List<HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow> getHvilkenPeriodeSkalDuIkkeAllikevelTaUt() {
        return hvilkenPeriodeSkalDuIkkeAllikevelTaUt;
    }

    public void setHvilkenPeriodeSkalDuIkkeAllikevelTaUt(List<HvilkenPeriodeSkalDuIkkeAllikevelTaUtRow> hvilkenPeriodeSkalDuIkkeAllikevelTaUt) {
        this.hvilkenPeriodeSkalDuIkkeAllikevelTaUt = hvilkenPeriodeSkalDuIkkeAllikevelTaUt;
    }

    public List<PerioderMedUtsettelseForste6UkerEtterFodselRow> getPerioderMedUtsettelseForste6UkerEtterFodsel() {
        return perioderMedUtsettelseForste6UkerEtterFodsel;
    }

    public void setPerioderMedUtsettelseForste6UkerEtterFodsel(List<PerioderMedUtsettelseForste6UkerEtterFodselRow> perioderMedUtsettelseForste6UkerEtterFodsel) {
        this.perioderMedUtsettelseForste6UkerEtterFodsel = perioderMedUtsettelseForste6UkerEtterFodsel;
    }

}
