package no.nav.foreldrepenger.web.app.tjenester.registrering;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OmsorgDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.RettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;
import no.nav.vedtak.util.InputValideringRegex;

public abstract class ManuellRegistreringDto extends BekreftetAksjonspunktDto {

    @NotNull
    @ValidKodeverk
    private FamilieHendelseType tema;
    @NotNull
    @ValidKodeverk
    private FagsakYtelseType soknadstype;

    @NotNull
    @ValidKodeverk
    private ForeldreType soker;

    @Valid
    private RettigheterDto rettigheter;

    private boolean oppholdINorge;
    private boolean harTidligereOppholdUtenlands;
    private boolean harFremtidigeOppholdUtenlands;

    @Size(max = 1000)
    private List<@Valid UtenlandsoppholdDto> tidligereOppholdUtenlands;

    @Size(max = 1000)
    private List<@Valid UtenlandsoppholdDto> fremtidigeOppholdUtenlands;

    private boolean erBarnetFodt;
    private LocalDate termindato;
    private LocalDate terminbekreftelseDato;

    @Min(1)
    @Max(9)
    private Integer antallBarnFraTerminbekreftelse;

    @Min(1)
    @Max(9)
    private Integer antallBarn;

    private LocalDate foedselsDato;

    @Valid
    private AnnenForelderDto annenForelder;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String tilleggsopplysninger;

    @ValidKodeverk
    private Språkkode språkkode;

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String kommentarEndring;
    private boolean registrerVerge = Boolean.FALSE;
    private LocalDate mottattDato;
    private boolean ufullstendigSoeknad;

    @Valid
    OmsorgDto omsorg;

    protected ManuellRegistreringDto() {
        // For Jackson
    }

    public FagsakYtelseType getSoknadstype() {
        return soknadstype;
    }

    public ForeldreType getSoker() {
        return soker;
    }

    public void setSoker(ForeldreType soker) {
        this.soker = soker;
    }

    public void setSoknadstype(FagsakYtelseType soknadstype) {
        this.soknadstype = soknadstype;
    }

    public FamilieHendelseType getTema() {
        return tema;
    }

    public void setTema(FamilieHendelseType tema) {
        this.tema = tema;
    }

    public RettigheterDto getRettigheter() {
        return rettigheter;
    }

    public void setRettigheter(RettigheterDto rettigheter) {
        this.rettigheter = rettigheter;
    }

    public boolean getOppholdINorge() {
        return oppholdINorge;
    }

    public void setOppholdINorge(boolean oppholdINorge) {
        this.oppholdINorge = oppholdINorge;
    }

    public boolean getHarTidligereOppholdUtenlands() {
        return harTidligereOppholdUtenlands;
    }

    public void setHarTidligereOppholdUtenlands(boolean harTidligereOppholdUtenlands) {
        this.harTidligereOppholdUtenlands = harTidligereOppholdUtenlands;
    }

    public boolean getHarFremtidigeOppholdUtenlands() {
        return harFremtidigeOppholdUtenlands;
    }

    public void setHarFremtidigeOppholdUtenlands(boolean harFremtidigeOppholdUtenlands) {
        this.harFremtidigeOppholdUtenlands = harFremtidigeOppholdUtenlands;
    }

    public List<UtenlandsoppholdDto> getTidligereOppholdUtenlands() {
        return tidligereOppholdUtenlands;
    }

    public void setTidligereOppholdUtenlands(List<UtenlandsoppholdDto> tidligereOppholdUtenlands) {
        this.tidligereOppholdUtenlands = tidligereOppholdUtenlands;
    }

    public List<UtenlandsoppholdDto> getFremtidigeOppholdUtenlands() {
        return fremtidigeOppholdUtenlands;
    }

    public void setFremtidigeOppholdUtenlands(List<UtenlandsoppholdDto> fremtidigeOppholdUtenlands) {
        this.fremtidigeOppholdUtenlands = fremtidigeOppholdUtenlands;
    }

    public boolean getErBarnetFodt() {
        return erBarnetFodt;
    }

    public void setErBarnetFodt(boolean erBarnetFodt) {
        this.erBarnetFodt = erBarnetFodt;
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

    public LocalDate getFoedselsDato() {
        return foedselsDato;
    }

    public void setFoedselsDato(LocalDate foedselsDato) {
        this.foedselsDato = foedselsDato;
    }

    public AnnenForelderDto getAnnenForelder() {
        return annenForelder;
    }

    public void setAnnenForelder(AnnenForelderDto annenForelder) {
        this.annenForelder = annenForelder;
    }

    public String getTilleggsopplysninger() {
        return tilleggsopplysninger;
    }

    public void setTilleggsopplysninger(String tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
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

    public boolean isRegistrerVerge() {
        return registrerVerge;
    }

    public void setRegistrerVerge(boolean registrerVerge) {
        this.registrerVerge = registrerVerge;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public boolean getUfullstendigSoeknad() {
        return ufullstendigSoeknad;
    }

    public void setUfullstendigSoeknad(boolean ufullstendigSoeknad) {
        this.ufullstendigSoeknad = ufullstendigSoeknad;
    }

    public OmsorgDto getOmsorg() {
        return omsorg;
    }

    public void setOmsorg(OmsorgDto omsorg) {
        this.omsorg = omsorg;
    }
}
