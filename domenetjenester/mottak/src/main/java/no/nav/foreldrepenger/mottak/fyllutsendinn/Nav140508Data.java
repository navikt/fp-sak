package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/** NAV 14-05.08 – Søknad om engangsstønad ved adopsjon */
public class Nav140508Data {

    private Boolean jegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger;

    private String fornavnSoker;

    private String etternavnSoker;

    private String fodselsnummerDNummerSoker;

    private HvaSokerDuOm hvaSokerDuOm;

    private LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa;

    private List<LeggTilBarnsFodselsdatoRow> leggTilBarnsFodselsdato;

    private JaNei borDuINorge;

    private HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene;

    private List<LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow> leggTilNyttUtenlandsoppholdDeNeste12Manedene;

    private HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene;

    private List<LeggTilNyttUtenlandsoppholdRow> leggTilNyttUtenlandsopphold;

    private JaNei harDuTilleggsopplysningerSomErRelevantForSoknaden;

    private String tilleggsopplysninger;

    private Boolean deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad;

    public enum HvaSokerDuOm {
        @JsonProperty("engangsstonadVedAdopsjon")
        ENGANGSSTONAD_VED_ADOPSJON,
        @JsonProperty("engangsstonadVedOvertakelseAvForeldreansvaretEllerOmsorgen")
        ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN,
        @JsonProperty("engangsstonadTilFarSomAdoptererAlene")
        ENGANGSSTONAD_TIL_FAR_SOM_ADOPTERER_ALENE;
    }

    public enum HvorSkalDuBoDeNeste12Manedene {
        @JsonProperty("boIUtlandetHeltEllerDelvis")
        BO_I_UTLANDET_HELT_ELLER_DELVIS,
        @JsonProperty("kunBoINorge")
        KUN_BO_I_NORGE;
    }

    public enum HvorHarDuBoddDeSiste12Manedene {
        @JsonProperty("boddIUtlandetHeltEllerDelvis")
        BODD_I_UTLANDET_HELT_ELLER_DELVIS,
        @JsonProperty("kunBoddINorge")
        KUN_BODD_I_NORGE;
    }

    public static class LeggTilBarnsFodselsdatoRow {

        private LocalDate fodselsdatoDdMmAaaa;

        public LeggTilBarnsFodselsdatoRow() {}

        public LocalDate getFodselsdatoDdMmAaaa() {
            return fodselsdatoDdMmAaaa;
        }

        public void setFodselsdatoDdMmAaaa(LocalDate fodselsdatoDdMmAaaa) {
            this.fodselsdatoDdMmAaaa = fodselsdatoDdMmAaaa;
        }

    }

    public static class LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow {

        private LocalDate fraDatoDdMmAaaa;

        private LocalDate tilDatoDdMmAaaa;

        private String hvilketLandSkalDuBoI;

        public LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow() {}

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

        public String getHvilketLandSkalDuBoI() {
            return hvilketLandSkalDuBoI;
        }

        public void setHvilketLandSkalDuBoI(String hvilketLandSkalDuBoI) {
            this.hvilketLandSkalDuBoI = hvilketLandSkalDuBoI;
        }

    }

    public static class LeggTilNyttUtenlandsoppholdRow {

        private LocalDate fraDatoDdMmAaaa;

        private LocalDate tilDatoDdMmAaaa;

        private String hvilketLandBoddeDuI;

        public LeggTilNyttUtenlandsoppholdRow() {}

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

        public String getHvilketLandBoddeDuI() {
            return hvilketLandBoddeDuI;
        }

        public void setHvilketLandBoddeDuI(String hvilketLandBoddeDuI) {
            this.hvilketLandBoddeDuI = hvilketLandBoddeDuI;
        }

    }

    public Nav140508Data() {}

    public Boolean isJegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger() {
        return jegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger;
    }

    public void setJegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger(Boolean jegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger) {
        this.jegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger = jegHarLestOgForstattDetSomStarPaNettsidenDuHarPliktTilAGiNavRiktigeOpplysninger;
    }

    public String getFornavnSoker() {
        return fornavnSoker;
    }

    public void setFornavnSoker(String fornavnSoker) {
        this.fornavnSoker = fornavnSoker;
    }

    public String getEtternavnSoker() {
        return etternavnSoker;
    }

    public void setEtternavnSoker(String etternavnSoker) {
        this.etternavnSoker = etternavnSoker;
    }

    public String getFodselsnummerDNummerSoker() {
        return fodselsnummerDNummerSoker;
    }

    public void setFodselsnummerDNummerSoker(String fodselsnummerDNummerSoker) {
        this.fodselsnummerDNummerSoker = fodselsnummerDNummerSoker;
    }

    public HvaSokerDuOm getHvaSokerDuOm() {
        return hvaSokerDuOm;
    }

    public void setHvaSokerDuOm(HvaSokerDuOm hvaSokerDuOm) {
        this.hvaSokerDuOm = hvaSokerDuOm;
    }

    public LocalDate getDatoForOmsorgsovertakelsenAvBarnetDdMmAaaa() {
        return datoForOmsorgsovertakelsenAvBarnetDdMmAaaa;
    }

    public void setDatoForOmsorgsovertakelsenAvBarnetDdMmAaaa(LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa) {
        this.datoForOmsorgsovertakelsenAvBarnetDdMmAaaa = datoForOmsorgsovertakelsenAvBarnetDdMmAaaa;
    }

    public List<LeggTilBarnsFodselsdatoRow> getLeggTilBarnsFodselsdato() {
        return leggTilBarnsFodselsdato;
    }

    public void setLeggTilBarnsFodselsdato(List<LeggTilBarnsFodselsdatoRow> leggTilBarnsFodselsdato) {
        this.leggTilBarnsFodselsdato = leggTilBarnsFodselsdato;
    }

    public JaNei getBorDuINorge() {
        return borDuINorge;
    }

    public void setBorDuINorge(JaNei borDuINorge) {
        this.borDuINorge = borDuINorge;
    }

    public HvorSkalDuBoDeNeste12Manedene getHvorSkalDuBoDeNeste12Manedene() {
        return hvorSkalDuBoDeNeste12Manedene;
    }

    public void setHvorSkalDuBoDeNeste12Manedene(HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene) {
        this.hvorSkalDuBoDeNeste12Manedene = hvorSkalDuBoDeNeste12Manedene;
    }

    public List<LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow> getLeggTilNyttUtenlandsoppholdDeNeste12Manedene() {
        return leggTilNyttUtenlandsoppholdDeNeste12Manedene;
    }

    public void setLeggTilNyttUtenlandsoppholdDeNeste12Manedene(List<LeggTilNyttUtenlandsoppholdDeNeste12ManedeneRow> leggTilNyttUtenlandsoppholdDeNeste12Manedene) {
        this.leggTilNyttUtenlandsoppholdDeNeste12Manedene = leggTilNyttUtenlandsoppholdDeNeste12Manedene;
    }

    public HvorHarDuBoddDeSiste12Manedene getHvorHarDuBoddDeSiste12Manedene() {
        return hvorHarDuBoddDeSiste12Manedene;
    }

    public void setHvorHarDuBoddDeSiste12Manedene(HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene) {
        this.hvorHarDuBoddDeSiste12Manedene = hvorHarDuBoddDeSiste12Manedene;
    }

    public List<LeggTilNyttUtenlandsoppholdRow> getLeggTilNyttUtenlandsopphold() {
        return leggTilNyttUtenlandsopphold;
    }

    public void setLeggTilNyttUtenlandsopphold(List<LeggTilNyttUtenlandsoppholdRow> leggTilNyttUtenlandsopphold) {
        this.leggTilNyttUtenlandsopphold = leggTilNyttUtenlandsopphold;
    }

    public JaNei getHarDuTilleggsopplysningerSomErRelevantForSoknaden() {
        return harDuTilleggsopplysningerSomErRelevantForSoknaden;
    }

    public void setHarDuTilleggsopplysningerSomErRelevantForSoknaden(JaNei harDuTilleggsopplysningerSomErRelevantForSoknaden) {
        this.harDuTilleggsopplysningerSomErRelevantForSoknaden = harDuTilleggsopplysningerSomErRelevantForSoknaden;
    }

    public String getTilleggsopplysninger() {
        return tilleggsopplysninger;
    }

    public void setTilleggsopplysninger(String tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
    }

    public Boolean isDeOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad() {
        return deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad;
    }

    public void setDeOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad(Boolean deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad) {
        this.deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad = deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad;
    }

}
