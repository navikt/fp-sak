package no.nav.foreldrepenger.mottak.fyllutsendinn;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/** NAV 14-05.07 – Søknad om engangsstønad ved fødsel */
public class Nav140507Data {

    private Boolean jegHarLestOgForstattDetSomStarPaNavNoRettogplikt;

    private String fornavnSoker;

    private String etternavnSoker;

    private String fodselsnummerDNummerSoker;

    private HvaSokerDuOm hvaSokerDuOm;

    private NarErBarnetFodt narErBarnetFodt;

    private Integer antallBarn;

    private List<LeggTilBarnetEllerBarnasFodselsdatoRow> leggTilBarnetEllerBarnasFodselsdato;

    private LocalDate termindatoDdMmAaaa;

    private LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa;

    private JaNei planleggerDuAVaereINorgePaFodselstidspunktet1;

    private HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene;

    private List<UtenlandsoppholdRow> utenlandsopphold;

    private HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene;

    private List<Utenlandsopphold1Row> utenlandsopphold1;

    private JaNei harDuTilleggsopplysningerSomErRelevantForSoknaden;

    private String tilleggsopplysninger;

    private Boolean deOpplysningerJegHarOppgittErRiktigeOgJegHarIkkeHoldtTilbakeOpplysningerSomHarBetydningForMinRettTilEngangsstonad;

    public enum HvaSokerDuOm {
        @JsonProperty("engangsstonadVedFodsel")
        ENGANGSSTONAD_VED_FODSEL,
        @JsonProperty("engangsstonadVedOvertakelseAvForeldreansvaretEllerOmsorgen")
        ENGANGSSTONAD_VED_OVERTAKELSE_AV_FORELDREANSVARET_ELLER_OMSORGEN;
    }

    public enum NarErBarnetFodt {
        @JsonProperty("tilbakeITid")
        TILBAKE_I_TID,
        @JsonProperty("fremITid")
        FREM_I_TID;
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

    public static class LeggTilBarnetEllerBarnasFodselsdatoRow {

        private LocalDate fodselsdatoDdMmAaaa;

        public LeggTilBarnetEllerBarnasFodselsdatoRow() {}

        public LocalDate getFodselsdatoDdMmAaaa() {
            return fodselsdatoDdMmAaaa;
        }

        public void setFodselsdatoDdMmAaaa(LocalDate fodselsdatoDdMmAaaa) {
            this.fodselsdatoDdMmAaaa = fodselsdatoDdMmAaaa;
        }

    }

    public static class UtenlandsoppholdRow {

        private LocalDate fraDatoDdMmAaaa;

        private LocalDate tilDatoDdMmAaaa;

        private String hvilketLandSkalDuBoI;

        public UtenlandsoppholdRow() {}

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

    public static class Utenlandsopphold1Row {

        private LocalDate fraDatoDdMmAaaa;

        private LocalDate tilDatoDdMmAaaa;

        private String hvilketLandBoddeDuI;

        public Utenlandsopphold1Row() {}

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

    public Nav140507Data() {}

    public Boolean isJegHarLestOgForstattDetSomStarPaNavNoRettogplikt() {
        return jegHarLestOgForstattDetSomStarPaNavNoRettogplikt;
    }

    public void setJegHarLestOgForstattDetSomStarPaNavNoRettogplikt(Boolean jegHarLestOgForstattDetSomStarPaNavNoRettogplikt) {
        this.jegHarLestOgForstattDetSomStarPaNavNoRettogplikt = jegHarLestOgForstattDetSomStarPaNavNoRettogplikt;
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

    public NarErBarnetFodt getNarErBarnetFodt() {
        return narErBarnetFodt;
    }

    public void setNarErBarnetFodt(NarErBarnetFodt narErBarnetFodt) {
        this.narErBarnetFodt = narErBarnetFodt;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    public List<LeggTilBarnetEllerBarnasFodselsdatoRow> getLeggTilBarnetEllerBarnasFodselsdato() {
        return leggTilBarnetEllerBarnasFodselsdato;
    }

    public void setLeggTilBarnetEllerBarnasFodselsdato(List<LeggTilBarnetEllerBarnasFodselsdatoRow> leggTilBarnetEllerBarnasFodselsdato) {
        this.leggTilBarnetEllerBarnasFodselsdato = leggTilBarnetEllerBarnasFodselsdato;
    }

    public LocalDate getTermindatoDdMmAaaa() {
        return termindatoDdMmAaaa;
    }

    public void setTermindatoDdMmAaaa(LocalDate termindatoDdMmAaaa) {
        this.termindatoDdMmAaaa = termindatoDdMmAaaa;
    }

    public LocalDate getDatoForOmsorgsovertakelsenAvBarnetDdMmAaaa() {
        return datoForOmsorgsovertakelsenAvBarnetDdMmAaaa;
    }

    public void setDatoForOmsorgsovertakelsenAvBarnetDdMmAaaa(LocalDate datoForOmsorgsovertakelsenAvBarnetDdMmAaaa) {
        this.datoForOmsorgsovertakelsenAvBarnetDdMmAaaa = datoForOmsorgsovertakelsenAvBarnetDdMmAaaa;
    }

    public JaNei getPlanleggerDuAVaereINorgePaFodselstidspunktet1() {
        return planleggerDuAVaereINorgePaFodselstidspunktet1;
    }

    public void setPlanleggerDuAVaereINorgePaFodselstidspunktet1(JaNei planleggerDuAVaereINorgePaFodselstidspunktet1) {
        this.planleggerDuAVaereINorgePaFodselstidspunktet1 = planleggerDuAVaereINorgePaFodselstidspunktet1;
    }

    public HvorSkalDuBoDeNeste12Manedene getHvorSkalDuBoDeNeste12Manedene() {
        return hvorSkalDuBoDeNeste12Manedene;
    }

    public void setHvorSkalDuBoDeNeste12Manedene(HvorSkalDuBoDeNeste12Manedene hvorSkalDuBoDeNeste12Manedene) {
        this.hvorSkalDuBoDeNeste12Manedene = hvorSkalDuBoDeNeste12Manedene;
    }

    public List<UtenlandsoppholdRow> getUtenlandsopphold() {
        return utenlandsopphold;
    }

    public void setUtenlandsopphold(List<UtenlandsoppholdRow> utenlandsopphold) {
        this.utenlandsopphold = utenlandsopphold;
    }

    public HvorHarDuBoddDeSiste12Manedene getHvorHarDuBoddDeSiste12Manedene() {
        return hvorHarDuBoddDeSiste12Manedene;
    }

    public void setHvorHarDuBoddDeSiste12Manedene(HvorHarDuBoddDeSiste12Manedene hvorHarDuBoddDeSiste12Manedene) {
        this.hvorHarDuBoddDeSiste12Manedene = hvorHarDuBoddDeSiste12Manedene;
    }

    public List<Utenlandsopphold1Row> getUtenlandsopphold1() {
        return utenlandsopphold1;
    }

    public void setUtenlandsopphold1(List<Utenlandsopphold1Row> utenlandsopphold1) {
        this.utenlandsopphold1 = utenlandsopphold1;
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
