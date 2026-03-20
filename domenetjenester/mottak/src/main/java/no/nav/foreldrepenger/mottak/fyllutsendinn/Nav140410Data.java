package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/** NAV 14-04.10 – Søknad om svangerskapspenger til selvstendig næringsdrivende og frilanser */
public class Nav140410Data {

    private DineOpplysninger1 dineOpplysninger1;

    private HarDuBoddINorgeDeSiste12Manedene harDuBoddINorgeDeSiste12Manedene;

    private Utenlandsopphold utenlandsopphold;

    private HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene;

    private Utenlandsopphold1 utenlandsopphold1;

    private JaNei harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene;

    private LocalDate narStartetDuSomFrilanserDdMmAaaa;

    private JaNei jobberDuFortsattSomFrilanser;

    private LocalDate narSluttetDuSomFrilanserDdMmAaaa;

    private JaNei harDuJobbetOgHattInntektSomSelvstendigNaeringsdrivendeDeSiste10Manedene;

    private HvilkenTypeVirksomhetDriverDu hvilkenTypeVirksomhetDriverDu;

    private String hvaHeterVirksomhetenDin;

    private LocalDate narStartetDuNaeringenDdMmAaaa;

    private JaNei erTestRegistrertINorge;

    private String virksomhetensOrganisasjonsnummer;

    private String landvelger;

    private JaNei erDetteEnVirksomhetDuDriverNa;

    private LocalDate oppgiSluttdatoForNaeringenDdMmAaaa;

    private HvorLengeHarDuHattDenneVirksomheten hvorLengeHarDuHattDenneVirksomheten;

    private Integer hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene;

    private JaNei harDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene;

    private LocalDate narBleDuYrkesaktivDdMmAaaa;

    private JaNei harDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene;

    private LocalDate oppgiDatoForEndringenDdMmAaaa;

    private Integer oppgiNaeringsinntektenDinEtterEndringen;

    private String herKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin;

    private JaNei harDuHattJobbIEuEosLandDeSiste10Manedene;

    private LocalDate fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa;

    private JaNei erDetEnJobbDuHarPerIDag;

    private LocalDate tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa;

    private HvilketLandJobbetDuI hvilketLandJobbetDuI;

    private String oppgiNavnetPaArbeidsgiveren;

    private HvorSkalDuSokeOmSvangerskapspengerFra hvorSkalDuSokeOmSvangerskapspengerFra;

    private LocalDate narHarDuTermindatoDdMmAaaa;

    private JaNei erBarnetFodt;

    private LocalDate narBleBarnetFodtDdMmAaaa;

    private String beskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret;

    private LocalDate fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa;

    private HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger;

    private List<RedusertArbeid1Row> redusertArbeid1;

    private List<RedusertArbeidRow> redusertArbeid;

    private List<JegKanIkkeFortsetteAJobbeRow> jegKanIkkeFortsetteAJobbe;

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

    public static class Utenlandsopphold {

        private String hvilketLandBoddeDuI;

        private LocalDate fraOgMedDatoDdMmAaaa;

        private LocalDate tilOgMedDatoDdMmAaaa;

        public Utenlandsopphold() {}

        public String getHvilketLandBoddeDuI() {
            return hvilketLandBoddeDuI;
        }

        public void setHvilketLandBoddeDuI(String hvilketLandBoddeDuI) {
            this.hvilketLandBoddeDuI = hvilketLandBoddeDuI;
        }

        public LocalDate getFraOgMedDatoDdMmAaaa() {
            return fraOgMedDatoDdMmAaaa;
        }

        public void setFraOgMedDatoDdMmAaaa(LocalDate fraOgMedDatoDdMmAaaa) {
            this.fraOgMedDatoDdMmAaaa = fraOgMedDatoDdMmAaaa;
        }

        public LocalDate getTilOgMedDatoDdMmAaaa() {
            return tilOgMedDatoDdMmAaaa;
        }

        public void setTilOgMedDatoDdMmAaaa(LocalDate tilOgMedDatoDdMmAaaa) {
            this.tilOgMedDatoDdMmAaaa = tilOgMedDatoDdMmAaaa;
        }

    }

    public static class Utenlandsopphold1 {

        private String hvilketLandSkalDuBoI;

        private LocalDate fraOgMedDatoDdMmAaaa;

        private LocalDate fraOgMedDatoDdMmAaaa1;

        public Utenlandsopphold1() {}

        public String getHvilketLandSkalDuBoI() {
            return hvilketLandSkalDuBoI;
        }

        public void setHvilketLandSkalDuBoI(String hvilketLandSkalDuBoI) {
            this.hvilketLandSkalDuBoI = hvilketLandSkalDuBoI;
        }

        public LocalDate getFraOgMedDatoDdMmAaaa() {
            return fraOgMedDatoDdMmAaaa;
        }

        public void setFraOgMedDatoDdMmAaaa(LocalDate fraOgMedDatoDdMmAaaa) {
            this.fraOgMedDatoDdMmAaaa = fraOgMedDatoDdMmAaaa;
        }

        public LocalDate getFraOgMedDatoDdMmAaaa1() {
            return fraOgMedDatoDdMmAaaa1;
        }

        public void setFraOgMedDatoDdMmAaaa1(LocalDate fraOgMedDatoDdMmAaaa1) {
            this.fraOgMedDatoDdMmAaaa1 = fraOgMedDatoDdMmAaaa1;
        }

    }

    public static class HvorSkalDuSokeOmSvangerskapspengerFra {

        private Boolean selvstendigNaeringsdrivende;

        private Boolean frilanser;

        public HvorSkalDuSokeOmSvangerskapspengerFra() {}

        public Boolean isSelvstendigNaeringsdrivende() {
            return selvstendigNaeringsdrivende;
        }

        public void setSelvstendigNaeringsdrivende(Boolean selvstendigNaeringsdrivende) {
            this.selvstendigNaeringsdrivende = selvstendigNaeringsdrivende;
        }

        public Boolean isFrilanser() {
            return frilanser;
        }

        public void setFrilanser(Boolean frilanser) {
            this.frilanser = frilanser;
        }

    }

    public static class HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger {

        private Boolean jegKanFortsetteMedSammeStillingsprosent;

        private Boolean jegKanFortsetteMedRedusertArbeidstid;

        private Boolean jegKanIkkeFortsetteAJobbe;

        public HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger() {}

        public Boolean isJegKanFortsetteMedSammeStillingsprosent() {
            return jegKanFortsetteMedSammeStillingsprosent;
        }

        public void setJegKanFortsetteMedSammeStillingsprosent(Boolean jegKanFortsetteMedSammeStillingsprosent) {
            this.jegKanFortsetteMedSammeStillingsprosent = jegKanFortsetteMedSammeStillingsprosent;
        }

        public Boolean isJegKanFortsetteMedRedusertArbeidstid() {
            return jegKanFortsetteMedRedusertArbeidstid;
        }

        public void setJegKanFortsetteMedRedusertArbeidstid(Boolean jegKanFortsetteMedRedusertArbeidstid) {
            this.jegKanFortsetteMedRedusertArbeidstid = jegKanFortsetteMedRedusertArbeidstid;
        }

        public Boolean isJegKanIkkeFortsetteAJobbe() {
            return jegKanIkkeFortsetteAJobbe;
        }

        public void setJegKanIkkeFortsetteAJobbe(Boolean jegKanIkkeFortsetteAJobbe) {
            this.jegKanIkkeFortsetteAJobbe = jegKanIkkeFortsetteAJobbe;
        }

    }

    public static class RedusertArbeid1Row {

        private LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa;

        private LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1;

        public RedusertArbeid1Row() {}

        public LocalDate getFraHvilkenDatoKanDuJobbeRedusertDdMmAaaa() {
            return fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa;
        }

        public void setFraHvilkenDatoKanDuJobbeRedusertDdMmAaaa(LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa) {
            this.fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa = fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa;
        }

        public LocalDate getFraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1() {
            return fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1;
        }

        public void setFraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1(LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1) {
            this.fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1 = fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa1;
        }

    }

    public static class RedusertArbeidRow {

        private LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa;

        private Integer oppgiStillingsprosentenDuSkalJobbe;

        public RedusertArbeidRow() {}

        public LocalDate getFraHvilkenDatoKanDuJobbeRedusertDdMmAaaa() {
            return fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa;
        }

        public void setFraHvilkenDatoKanDuJobbeRedusertDdMmAaaa(LocalDate fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa) {
            this.fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa = fraHvilkenDatoKanDuJobbeRedusertDdMmAaaa;
        }

        public Integer getOppgiStillingsprosentenDuSkalJobbe() {
            return oppgiStillingsprosentenDuSkalJobbe;
        }

        public void setOppgiStillingsprosentenDuSkalJobbe(Integer oppgiStillingsprosentenDuSkalJobbe) {
            this.oppgiStillingsprosentenDuSkalJobbe = oppgiStillingsprosentenDuSkalJobbe;
        }

    }

    public static class JegKanIkkeFortsetteAJobbeRow {

        private LocalDate fraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa;

        public JegKanIkkeFortsetteAJobbeRow() {}

        public LocalDate getFraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa() {
            return fraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa;
        }

        public void setFraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa(LocalDate fraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa) {
            this.fraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa = fraHvilkenDatoKanDuIkkeFortsetteAJobbeDdMmAaaa;
        }

    }

    public Nav140410Data() {}

    public DineOpplysninger1 getDineOpplysninger1() {
        return dineOpplysninger1;
    }

    public void setDineOpplysninger1(DineOpplysninger1 dineOpplysninger1) {
        this.dineOpplysninger1 = dineOpplysninger1;
    }

    public HarDuBoddINorgeDeSiste12Manedene getHarDuBoddINorgeDeSiste12Manedene() {
        return harDuBoddINorgeDeSiste12Manedene;
    }

    public void setHarDuBoddINorgeDeSiste12Manedene(HarDuBoddINorgeDeSiste12Manedene harDuBoddINorgeDeSiste12Manedene) {
        this.harDuBoddINorgeDeSiste12Manedene = harDuBoddINorgeDeSiste12Manedene;
    }

    public Utenlandsopphold getUtenlandsopphold() {
        return utenlandsopphold;
    }

    public void setUtenlandsopphold(Utenlandsopphold utenlandsopphold) {
        this.utenlandsopphold = utenlandsopphold;
    }

    public HvorSkalDuBoDeNeste12Manedene getHvorSkalDuBoDeNeste12Manedene() {
        return hvorSkalDuBoDeNeste12Manedene;
    }

    public void setHvorSkalDuBoDeNeste12Manedene(HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene) {
        this.hvorSkalDuBoDeNeste12Manedene = hvorSkalDuBoDeNeste12Manedene;
    }

    public Utenlandsopphold1 getUtenlandsopphold1() {
        return utenlandsopphold1;
    }

    public void setUtenlandsopphold1(Utenlandsopphold1 utenlandsopphold1) {
        this.utenlandsopphold1 = utenlandsopphold1;
    }

    public JaNei getHarDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene() {
        return harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene;
    }

    public void setHarDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene(JaNei harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene) {
        this.harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene = harDuJobbetOgHattInntektSomFrilanserDeSiste10Manedene;
    }

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

    public LocalDate getNarSluttetDuSomFrilanserDdMmAaaa() {
        return narSluttetDuSomFrilanserDdMmAaaa;
    }

    public void setNarSluttetDuSomFrilanserDdMmAaaa(LocalDate narSluttetDuSomFrilanserDdMmAaaa) {
        this.narSluttetDuSomFrilanserDdMmAaaa = narSluttetDuSomFrilanserDdMmAaaa;
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

    public LocalDate getNarStartetDuNaeringenDdMmAaaa() {
        return narStartetDuNaeringenDdMmAaaa;
    }

    public void setNarStartetDuNaeringenDdMmAaaa(LocalDate narStartetDuNaeringenDdMmAaaa) {
        this.narStartetDuNaeringenDdMmAaaa = narStartetDuNaeringenDdMmAaaa;
    }

    public JaNei getErTestRegistrertINorge() {
        return erTestRegistrertINorge;
    }

    public void setErTestRegistrertINorge(JaNei erTestRegistrertINorge) {
        this.erTestRegistrertINorge = erTestRegistrertINorge;
    }

    public String getVirksomhetensOrganisasjonsnummer() {
        return virksomhetensOrganisasjonsnummer;
    }

    public void setVirksomhetensOrganisasjonsnummer(String virksomhetensOrganisasjonsnummer) {
        this.virksomhetensOrganisasjonsnummer = virksomhetensOrganisasjonsnummer;
    }

    public String getLandvelger() {
        return landvelger;
    }

    public void setLandvelger(String landvelger) {
        this.landvelger = landvelger;
    }

    public JaNei getErDetteEnVirksomhetDuDriverNa() {
        return erDetteEnVirksomhetDuDriverNa;
    }

    public void setErDetteEnVirksomhetDuDriverNa(JaNei erDetteEnVirksomhetDuDriverNa) {
        this.erDetteEnVirksomhetDuDriverNa = erDetteEnVirksomhetDuDriverNa;
    }

    public LocalDate getOppgiSluttdatoForNaeringenDdMmAaaa() {
        return oppgiSluttdatoForNaeringenDdMmAaaa;
    }

    public void setOppgiSluttdatoForNaeringenDdMmAaaa(LocalDate oppgiSluttdatoForNaeringenDdMmAaaa) {
        this.oppgiSluttdatoForNaeringenDdMmAaaa = oppgiSluttdatoForNaeringenDdMmAaaa;
    }

    public HvorLengeHarDuHattDenneVirksomheten getHvorLengeHarDuHattDenneVirksomheten() {
        return hvorLengeHarDuHattDenneVirksomheten;
    }

    public void setHvorLengeHarDuHattDenneVirksomheten(HvorLengeHarDuHattDenneVirksomheten hvorLengeHarDuHattDenneVirksomheten) {
        this.hvorLengeHarDuHattDenneVirksomheten = hvorLengeHarDuHattDenneVirksomheten;
    }

    public Integer getHvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene() {
        return hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene;
    }

    public void setHvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene(Integer hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene) {
        this.hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene = hvaHarDuHattINaeringsresultatForSkattDeSiste12Manedene;
    }

    public JaNei getHarDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene() {
        return harDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene;
    }

    public void setHarDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene(JaNei harDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene) {
        this.harDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene = harDuBlittYrkesaktivILopetAvDe3SisteFerdigliknendeArene;
    }

    public LocalDate getNarBleDuYrkesaktivDdMmAaaa() {
        return narBleDuYrkesaktivDdMmAaaa;
    }

    public void setNarBleDuYrkesaktivDdMmAaaa(LocalDate narBleDuYrkesaktivDdMmAaaa) {
        this.narBleDuYrkesaktivDdMmAaaa = narBleDuYrkesaktivDdMmAaaa;
    }

    public JaNei getHarDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene() {
        return harDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene;
    }

    public void setHarDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene(JaNei harDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene) {
        this.harDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene = harDuHattEnVarigEndringIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDinDeSiste4Arene;
    }

    public LocalDate getOppgiDatoForEndringenDdMmAaaa() {
        return oppgiDatoForEndringenDdMmAaaa;
    }

    public void setOppgiDatoForEndringenDdMmAaaa(LocalDate oppgiDatoForEndringenDdMmAaaa) {
        this.oppgiDatoForEndringenDdMmAaaa = oppgiDatoForEndringenDdMmAaaa;
    }

    public Integer getOppgiNaeringsinntektenDinEtterEndringen() {
        return oppgiNaeringsinntektenDinEtterEndringen;
    }

    public void setOppgiNaeringsinntektenDinEtterEndringen(Integer oppgiNaeringsinntektenDinEtterEndringen) {
        this.oppgiNaeringsinntektenDinEtterEndringen = oppgiNaeringsinntektenDinEtterEndringen;
    }

    public String getHerKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin() {
        return herKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin;
    }

    public void setHerKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin(String herKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin) {
        this.herKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin = herKanDuSkriveKortOmHvaSomHarEndretSegIArbeidsforholdetDittVirksomhetenEllerArbeidssituasjonenDin;
    }

    public JaNei getHarDuHattJobbIEuEosLandDeSiste10Manedene() {
        return harDuHattJobbIEuEosLandDeSiste10Manedene;
    }

    public void setHarDuHattJobbIEuEosLandDeSiste10Manedene(JaNei harDuHattJobbIEuEosLandDeSiste10Manedene) {
        this.harDuHattJobbIEuEosLandDeSiste10Manedene = harDuHattJobbIEuEosLandDeSiste10Manedene;
    }

    public LocalDate getFraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa() {
        return fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa;
    }

    public void setFraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa(LocalDate fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa) {
        this.fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa = fraHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa;
    }

    public JaNei getErDetEnJobbDuHarPerIDag() {
        return erDetEnJobbDuHarPerIDag;
    }

    public void setErDetEnJobbDuHarPerIDag(JaNei erDetEnJobbDuHarPerIDag) {
        this.erDetEnJobbDuHarPerIDag = erDetEnJobbDuHarPerIDag;
    }

    public LocalDate getTilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa() {
        return tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa;
    }

    public void setTilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa(LocalDate tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa) {
        this.tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa = tilHvilkenDatoHarDuHattJobbIEuEosLandDdMmAaaa;
    }

    public HvilketLandJobbetDuI getHvilketLandJobbetDuI() {
        return hvilketLandJobbetDuI;
    }

    public void setHvilketLandJobbetDuI(HvilketLandJobbetDuI hvilketLandJobbetDuI) {
        this.hvilketLandJobbetDuI = hvilketLandJobbetDuI;
    }

    public String getOppgiNavnetPaArbeidsgiveren() {
        return oppgiNavnetPaArbeidsgiveren;
    }

    public void setOppgiNavnetPaArbeidsgiveren(String oppgiNavnetPaArbeidsgiveren) {
        this.oppgiNavnetPaArbeidsgiveren = oppgiNavnetPaArbeidsgiveren;
    }

    public HvorSkalDuSokeOmSvangerskapspengerFra getHvorSkalDuSokeOmSvangerskapspengerFra() {
        return hvorSkalDuSokeOmSvangerskapspengerFra;
    }

    public void setHvorSkalDuSokeOmSvangerskapspengerFra(HvorSkalDuSokeOmSvangerskapspengerFra hvorSkalDuSokeOmSvangerskapspengerFra) {
        this.hvorSkalDuSokeOmSvangerskapspengerFra = hvorSkalDuSokeOmSvangerskapspengerFra;
    }

    public LocalDate getNarHarDuTermindatoDdMmAaaa() {
        return narHarDuTermindatoDdMmAaaa;
    }

    public void setNarHarDuTermindatoDdMmAaaa(LocalDate narHarDuTermindatoDdMmAaaa) {
        this.narHarDuTermindatoDdMmAaaa = narHarDuTermindatoDdMmAaaa;
    }

    public JaNei getErBarnetFodt() {
        return erBarnetFodt;
    }

    public void setErBarnetFodt(JaNei erBarnetFodt) {
        this.erBarnetFodt = erBarnetFodt;
    }

    public LocalDate getNarBleBarnetFodtDdMmAaaa() {
        return narBleBarnetFodtDdMmAaaa;
    }

    public void setNarBleBarnetFodtDdMmAaaa(LocalDate narBleBarnetFodtDdMmAaaa) {
        this.narBleBarnetFodtDdMmAaaa = narBleBarnetFodtDdMmAaaa;
    }

    public String getBeskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret() {
        return beskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret;
    }

    public void setBeskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret(String beskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret) {
        this.beskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret = beskrivSaNoyaktigSomMuligDeForholdVedArbeidssituasjonenSomDuMenerKanInnebaereRisikoForFosteret;
    }

    public LocalDate getFraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa() {
        return fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa;
    }

    public void setFraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa(LocalDate fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa) {
        this.fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa = fraHvilkenDatoHarDuBehovForSvangerskapspengerDdMmAaaa;
    }

    public HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger getHvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger() {
        return hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger;
    }

    public void setHvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger(HvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger) {
        this.hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger = hvordanKanDuJobbeIPeriodenDuHarBehovForSvangerskapspenger;
    }

    public List<RedusertArbeid1Row> getRedusertArbeid1() {
        return redusertArbeid1;
    }

    public void setRedusertArbeid1(List<RedusertArbeid1Row> redusertArbeid1) {
        this.redusertArbeid1 = redusertArbeid1;
    }

    public List<RedusertArbeidRow> getRedusertArbeid() {
        return redusertArbeid;
    }

    public void setRedusertArbeid(List<RedusertArbeidRow> redusertArbeid) {
        this.redusertArbeid = redusertArbeid;
    }

    public List<JegKanIkkeFortsetteAJobbeRow> getJegKanIkkeFortsetteAJobbe() {
        return jegKanIkkeFortsetteAJobbe;
    }

    public void setJegKanIkkeFortsetteAJobbe(List<JegKanIkkeFortsetteAJobbeRow> jegKanIkkeFortsetteAJobbe) {
        this.jegKanIkkeFortsetteAJobbe = jegKanIkkeFortsetteAJobbe;
    }

}
