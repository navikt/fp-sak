package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.AntallTimer;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

public class AktivitetsAvtale extends BaseEntitet implements IndexKey {

    @DiffIgnore
    private AntallTimer antallTimer;

    @DiffIgnore
    private AntallTimer antallTimerFulltid;

    @ChangeTracked
    private Stillingsprosent prosentsats;

    private String beskrivelse;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private LocalDate sisteLønnsendringsdato;

    /**
     * Setter en periode brukt til overstyring av angitt periode (avledet fra saksbehandlers vurderinger). Benyttes kun transient (ved filtrering av modellen)
     */
    private DatoIntervallEntitet overstyrtPeriode;

    AktivitetsAvtale() {
        // hibernate
    }

    /**
     * Deep copy ctor
     */
    AktivitetsAvtale(AktivitetsAvtale aktivitetsAvtale) {
        this.antallTimer = aktivitetsAvtale.getAntallTimer();
        this.antallTimerFulltid = aktivitetsAvtale.getAntallTimerFulltid();
        this.prosentsats = aktivitetsAvtale.getProsentsats();
        this.beskrivelse = aktivitetsAvtale.getBeskrivelse();
        this.sisteLønnsendringsdato = aktivitetsAvtale.getSisteLønnsendringsdato();
        this.periode = aktivitetsAvtale.getPeriodeUtenOverstyring();
    }

    public AktivitetsAvtale(AktivitetsAvtale avtale, DatoIntervallEntitet overstyrtPeriode) {
        this(avtale);
        this.overstyrtPeriode = overstyrtPeriode;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, sisteLønnsendringsdato);
    }

    /**
     * For timelønnede så vil antallet timer i arbeidsavtalen være satt her
     *
     * @return antall timer
     * @deprecated Ikke lenger i bruk. Bruk sistelønnsendringsdato og evt. stillingsprosent for å avgjøre om det er en ansettelsesavtale
     */
    @Deprecated
    public AntallTimer getAntallTimer() {
        return antallTimer;
    }

    /**
     * @deprecated Bruker ikke antall timer lenger. Dersom det brekker tester, bruk sisteLønnsendringsdato i stedf. antall timer for å definere som ansettelsesavtale
     */
    @Deprecated
    void setAntallTimer(AntallTimer antallTimer) {
        this.antallTimer = antallTimer;
    }

    /**
     * Antall timer som tilsvarer fulltid (f.eks 40 timer)
     * @return antall timer
     * @deprecated Ikke lenger i bruk. Bruk sistelønnsendringsdato og evt. stillingsprosent for å avgjøre om det er en ansettelsesavtale
     */
    @Deprecated
    public AntallTimer getAntallTimerFulltid() {
        return antallTimerFulltid;
    }

    /**
     * @deprecated Bruker ikke antall timer lenger. Dersom det brekker tester, bruk sisteLønnsendringsdato i stedf. antall timer for å definere som ansettelsesavtale
     */
    @Deprecated
    void setAntallTimerFulltid(AntallTimer antallTimerFulltid) {
        this.antallTimerFulltid = antallTimerFulltid;
    }

    /**
     * Avtalt prosentsats i avtalen
     *
     * @return prosent
     */

    public Stillingsprosent getProsentsats() {
        return prosentsats;
    }

    void setProsentsats(Stillingsprosent prosentsats) {
        this.prosentsats = prosentsats;
    }

    /**
     * Perioden til aktivitetsavtalen.
     * Tar hensyn til overstyring gjort i 5080.
     *
     * @return Hele perioden, tar hensyn til overstyringer.
     */
    public DatoIntervallEntitet getPeriode() {
        return erOverstyrtPeriode() ? overstyrtPeriode : periode;
    }

    /**
     * Henter kun den originale perioden, ikke den overstyrte perioden.
     * Bruk heller {@link #getPeriode} i de fleste tilfeller
     * @return Hele den originale perioden, uten overstyringer.
     */

    public DatoIntervallEntitet getPeriodeUtenOverstyring() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    /**
     * Hvorvidet denne avtalen har en overstyrt periode.
     */

    public boolean erOverstyrtPeriode() {
        return overstyrtPeriode != null;
    }


    public LocalDate getSisteLønnsendringsdato() {
        return sisteLønnsendringsdato;
    }


    public boolean matcherPeriode(DatoIntervallEntitet aktivitetsAvtale) {
        return getPeriode().equals(aktivitetsAvtale);
    }

    /**
     * Er avtallen løpende
     *
     * @return true/false
     */
    public boolean getErLøpende() {
        return Tid.TIDENES_ENDE.equals(getPeriode().getTomDato());
    }


    public String getBeskrivelse() {
        return beskrivelse;
    }

    void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    void sisteLønnsendringsdato(LocalDate sisteLønnsendringsdato) {
        this.sisteLønnsendringsdato = sisteLønnsendringsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AktivitetsAvtale)) return false;
        AktivitetsAvtale that = (AktivitetsAvtale) o;
        return Objects.equals(antallTimer, that.antallTimer) &&
            Objects.equals(antallTimerFulltid, that.antallTimerFulltid) &&
            Objects.equals(beskrivelse, that.beskrivelse) &&
            Objects.equals(prosentsats, that.prosentsats) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(sisteLønnsendringsdato, that.sisteLønnsendringsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(antallTimer, antallTimerFulltid, beskrivelse, prosentsats, periode, sisteLønnsendringsdato);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            "antallTimer=" + antallTimer + //$NON-NLS-1$
            ", antallTimerFulltid=" + antallTimerFulltid + //$NON-NLS-1$
            ", periode=" + periode + //$NON-NLS-1$
            ", overstyrtPeriode=" + overstyrtPeriode + //$NON-NLS-1$
            ", prosentsats=" + prosentsats + //$NON-NLS-1$
            ", beskrivelse=" + beskrivelse + //$NON-NLS-1$
            ", sisteLønnsendringsdato="+sisteLønnsendringsdato + //$NON-NLS-1$
            '>';
    }

    boolean hasValues() {
        return antallTimer != null || antallTimerFulltid != null || prosentsats != null || periode != null;
    }

    public boolean erAnsettelsesPeriode() {
        return prosentsats == null || prosentsats.erNulltall();
    }
}
