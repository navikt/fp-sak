package no.nav.foreldrepenger.mottak.fyllutsendinn.frontend;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

/**
 * Mellomlagring av papirsøknad-utkast. Feltnavnene speiler frontend-skjemaets form field names (raw getValues()),
 * ikke ManuellRegistreringDto sine transformerte feltnavn.
 * <p>
 * Brukes ikke direkte i REST-endepunktet (som tar opak String), men kan benyttes for å bygge
 * forhåndsutfylte mellomlagringsobjekter fra annen input og serialisere via {@code DefaultJsonMapper.toJson(dto)}.
 * Frontenden konsumerer via JSON.parse() og spreader inn i form defaultValues.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PapirsoknadEngangsstonadMellomlagreDto.class, name = AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE),
    @JsonSubTypes.Type(value = PapirsoknadForeldrepengerMellomlagreDto.class, name = AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER_KODE),
    @JsonSubTypes.Type(value = PapirsoknadEndringsøknadMellomlagreDto.class, name = AksjonspunktKodeDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER_KODE),
    @JsonSubTypes.Type(value = PapirsoknadSvangerskapspengerMellomlagreDto.class, name = AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE),
})
public abstract class PapirsoknadMellomlagreDto {

    // Metadata — satt av frontend-wrapper (RegistrerPapirsoknadPanel), bruker ASCII-feltnavn
    @ValidKodeverk
    private FamilieHendelseType familieHendelseType;

    @ValidKodeverk
    private FagsakYtelseType fagsakYtelseType;

    @ValidKodeverk
    private ForeldreType foreldreType;

    // Mottatt dato
    private LocalDate mottattDato;

    // Opphold i Norge — form field names (NB: oppholdSisteTolvINorge = "har bodd i Norge siste 12 mnd", IKKE negert)
    private Boolean oppholdINorge;
    private Boolean oppholdSisteTolvINorge;
    private Boolean oppholdNesteTolvINorge;

    @Size(max = 1000)
    private List<@Valid UtenlandsoppholdFormValues> tidligereOppholdUtenlands;

    @Size(max = 1000)
    private List<@Valid UtenlandsoppholdFormValues> fremtidigeOppholdUtenlands;

    // Termin og fødsel
    private Boolean erBarnetFødt;
    private LocalDate termindato;
    private LocalDate terminbekreftelseDato;

    @Max(9)
    private Integer antallBarnFraTerminbekreftelse;

    @Max(9)
    private Integer antallBarn;

    private LocalDate fødselsdato;

    // Rettigheter (enum-verdi som string, f.eks. "ANNEN_FORELDER_DOED")
    private String rettigheter;

    // Omsorg og adopsjon
    @Valid
    private OmsorgFormValues omsorg;

    // Annen forelder
    @Valid
    private AnnenForelderFormValues annenForelder;

    // Språk
    @ValidKodeverk
    private Språkkode språkkode;

    // Lagre-panel
    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String kommentarEndring;

    private Boolean registrerVerge;
    private Boolean ufullstendigSøknad;

    protected PapirsoknadMellomlagreDto() {
        // For Jackson
    }

    public FamilieHendelseType getFamilieHendelseType() {
        return familieHendelseType;
    }

    public void setFamilieHendelseType(FamilieHendelseType familieHendelseType) {
        this.familieHendelseType = familieHendelseType;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public void setFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
    }

    public ForeldreType getForeldreType() {
        return foreldreType;
    }

    public void setForeldreType(ForeldreType foreldreType) {
        this.foreldreType = foreldreType;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public Boolean getOppholdINorge() {
        return oppholdINorge;
    }

    public void setOppholdINorge(Boolean oppholdINorge) {
        this.oppholdINorge = oppholdINorge;
    }

    public Boolean getOppholdSisteTolvINorge() {
        return oppholdSisteTolvINorge;
    }

    public void setOppholdSisteTolvINorge(Boolean oppholdSisteTolvINorge) {
        this.oppholdSisteTolvINorge = oppholdSisteTolvINorge;
    }

    public Boolean getOppholdNesteTolvINorge() {
        return oppholdNesteTolvINorge;
    }

    public void setOppholdNesteTolvINorge(Boolean oppholdNesteTolvINorge) {
        this.oppholdNesteTolvINorge = oppholdNesteTolvINorge;
    }

    public List<UtenlandsoppholdFormValues> getTidligereOppholdUtenlands() {
        return tidligereOppholdUtenlands;
    }

    public void setTidligereOppholdUtenlands(List<UtenlandsoppholdFormValues> tidligereOppholdUtenlands) {
        this.tidligereOppholdUtenlands = tidligereOppholdUtenlands;
    }

    public List<UtenlandsoppholdFormValues> getFremtidigeOppholdUtenlands() {
        return fremtidigeOppholdUtenlands;
    }

    public void setFremtidigeOppholdUtenlands(List<UtenlandsoppholdFormValues> fremtidigeOppholdUtenlands) {
        this.fremtidigeOppholdUtenlands = fremtidigeOppholdUtenlands;
    }

    public Boolean getErBarnetFødt() {
        return erBarnetFødt;
    }

    public void setErBarnetFødt(Boolean erBarnetFødt) {
        this.erBarnetFødt = erBarnetFødt;
    }

    public LocalDate getTermindato() {
        return termindato;
    }

    public void setTermindato(LocalDate termindato) {
        this.termindato = termindato;
    }

    public LocalDate getTerminbekreftelseDato() {
        return terminbekreftelseDato;
    }

    public void setTerminbekreftelseDato(LocalDate terminbekreftelseDato) {
        this.terminbekreftelseDato = terminbekreftelseDato;
    }

    public Integer getAntallBarnFraTerminbekreftelse() {
        return antallBarnFraTerminbekreftelse;
    }

    public void setAntallBarnFraTerminbekreftelse(Integer antallBarnFraTerminbekreftelse) {
        this.antallBarnFraTerminbekreftelse = antallBarnFraTerminbekreftelse;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public String getRettigheter() {
        return rettigheter;
    }

    public void setRettigheter(String rettigheter) {
        this.rettigheter = rettigheter;
    }

    public OmsorgFormValues getOmsorg() {
        return omsorg;
    }

    public void setOmsorg(OmsorgFormValues omsorg) {
        this.omsorg = omsorg;
    }

    public AnnenForelderFormValues getAnnenForelder() {
        return annenForelder;
    }

    public void setAnnenForelder(AnnenForelderFormValues annenForelder) {
        this.annenForelder = annenForelder;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public void setSpråkkode(Språkkode språkkode) {
        this.språkkode = språkkode;
    }

    public String getKommentarEndring() {
        return kommentarEndring;
    }

    public void setKommentarEndring(String kommentarEndring) {
        this.kommentarEndring = kommentarEndring;
    }

    public Boolean getRegistrerVerge() {
        return registrerVerge;
    }

    public void setRegistrerVerge(Boolean registrerVerge) {
        this.registrerVerge = registrerVerge;
    }

    public Boolean getUfullstendigSøknad() {
        return ufullstendigSøknad;
    }

    public void setUfullstendigSøknad(Boolean ufullstendigSøknad) {
        this.ufullstendigSøknad = ufullstendigSøknad;
    }

    // --- Selvstendige form-value records (uavhengig av web-modulens DTOer) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UtenlandsoppholdFormValues(String land, LocalDate periodeFom, LocalDate periodeTom) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OmsorgFormValues(Integer antallBarn, List<LocalDate> fødselsdato, LocalDate omsorgsovertakelsesdato,
                                   LocalDate ankomstdato, Boolean erEktefellesBarn) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnnenForelderFormValues(String fødselsnummer, Boolean kanIkkeOppgiAnnenForelder,
                                          KanIkkeOppgiBegrunnelseFormValues kanIkkeOppgiBegrunnelse,
                                          Boolean søkerHarAleneomsorg, Boolean denAndreForelderenHarRettPåForeldrepenger,
                                          Boolean annenForelderRettEØS, Boolean morMottarUføretrygd) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KanIkkeOppgiBegrunnelseFormValues(String årsak, String utenlandskFødselsnummer, String land) { }
}
