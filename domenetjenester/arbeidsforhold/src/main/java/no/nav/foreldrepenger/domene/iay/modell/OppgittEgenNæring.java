package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Convert;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

public class OppgittEgenNæring extends BaseEntitet implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    private OrgNummer virksomhetOrgnr;

    private VirksomhetType virksomhetType = VirksomhetType.UDEFINERT;

    private String regnskapsførerNavn;

    private String regnskapsførerTlf;

    private LocalDate endringDato;

    private String begrunnelse;

    private BigDecimal bruttoInntekt;

    @Convert(converter = BooleanToStringConverter.class)
    private boolean nyoppstartet;

    @Convert(converter = BooleanToStringConverter.class)
    private boolean varigEndring;

    @Convert(converter = BooleanToStringConverter.class)
    private boolean nærRelasjon;

    @Convert(converter = BooleanToStringConverter.class)
    private boolean nyIArbeidslivet;

    private OppgittUtenlandskVirksomhet utenlandskVirksomhet = new OppgittUtenlandskVirksomhet();

    OppgittEgenNæring() {
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, virksomhetOrgnr, utenlandskVirksomhet);
    }

    public LocalDate getFraOgMed() {
        return periode.getFomDato();
    }

    public LocalDate getTilOgMed() {
        return periode.getTomDato();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public VirksomhetType getVirksomhetType() {
        return virksomhetType;
    }

    void setVirksomhetType(VirksomhetType virksomhetType) {
        this.virksomhetType = virksomhetType;
    }

    /** Samme som {@link #getVirksomhetOrgnr()} men returnerer string. */
    public String getOrgnr() {
        return virksomhetOrgnr == null ? null : virksomhetOrgnr.getId();
    }

    public OrgNummer getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    void setVirksomhetOrgnr(OrgNummer orgNr) {
        this.virksomhetOrgnr = orgNr;
    }

    public String getRegnskapsførerNavn() {
        return regnskapsførerNavn;
    }

    void setRegnskapsførerNavn(String regnskapsførerNavn) {
        this.regnskapsførerNavn = regnskapsførerNavn;
    }

    public String getRegnskapsførerTlf() {
        return regnskapsførerTlf;
    }

    void setRegnskapsførerTlf(String regnskapsførerTlf) {
        this.regnskapsførerTlf = regnskapsførerTlf;
    }

    public LocalDate getEndringDato() {
        return endringDato;
    }

    void setEndringDato(LocalDate endringDato) {
        this.endringDato = endringDato;
    }

    public BigDecimal getBruttoInntekt() {
        return bruttoInntekt;
    }

    void setBruttoInntekt(BigDecimal bruttoInntekt) {
        this.bruttoInntekt = bruttoInntekt;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public boolean getNyoppstartet() {
        return nyoppstartet;
    }

    void setNyoppstartet(boolean nyoppstartet) {
        this.nyoppstartet = nyoppstartet;
    }

    void setNyIArbeidslivet(boolean nyIArbeidslivet) {
        this.nyIArbeidslivet = nyIArbeidslivet;
    }

    public boolean getNyIArbeidslivet() {
        return nyIArbeidslivet;
    }

    public boolean getVarigEndring() {
        return varigEndring;
    }

    void setVarigEndring(boolean varigEndring) {
        this.varigEndring = varigEndring;
    }

    public boolean getNærRelasjon() {
        return nærRelasjon;
    }

    void setNærRelasjon(boolean nærRelasjon) {
        this.nærRelasjon = nærRelasjon;
    }

    public OppgittUtenlandskVirksomhet getVirksomhet() {
        return utenlandskVirksomhet;
    }

    void setUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
        this.utenlandskVirksomhet = utenlandskVirksomhet;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || !(o instanceof OppgittEgenNæring that)) {
            return false;
        }
        return Objects.equals(periode, that.periode) &&
                Objects.equals(virksomhetOrgnr, that.virksomhetOrgnr) &&
                Objects.equals(nyoppstartet, that.nyoppstartet) &&
                Objects.equals(virksomhetType, that.virksomhetType) &&
                Objects.equals(regnskapsførerNavn, that.regnskapsførerNavn) &&
                Objects.equals(regnskapsførerTlf, that.regnskapsførerTlf) &&
                Objects.equals(endringDato, that.endringDato) &&
                Objects.equals(begrunnelse, that.begrunnelse) &&
                Objects.equals(bruttoInntekt, that.bruttoInntekt) &&
                Objects.equals(utenlandskVirksomhet, that.utenlandskVirksomhet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, virksomhetOrgnr, virksomhetType, nyoppstartet, regnskapsførerNavn, regnskapsførerTlf, endringDato, begrunnelse,
                bruttoInntekt, utenlandskVirksomhet);
    }

    @Override
    public String toString() {
        return "EgenNæringEntitet{" +
                "periode=" + periode +
                ", virksomhet=" + virksomhetOrgnr +
                ", nyoppstartet=" + nyoppstartet +
                ", virksomhetType=" + virksomhetType +
                ", regnskapsførerNavn='" + regnskapsførerNavn + '\'' +
                ", regnskapsførerTlf='" + regnskapsførerTlf + '\'' +
                ", endringDato=" + endringDato +
                ", begrunnelse='" + begrunnelse + '\'' +
                ", bruttoInntekt=" + bruttoInntekt +
                ", utenlandskVirksomhet=" + utenlandskVirksomhet +
                '}';
    }
}
