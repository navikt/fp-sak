package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class Yrkesaktivitet implements IndexKey {

    @ChangeTracked
    private Set<AktivitetsAvtale> aktivitetsAvtale = new LinkedHashSet<>();

    @ChangeTracked
    private Set<Permisjon> permisjon = new LinkedHashSet<>();

    @ChangeTracked
    private String navnArbeidsgiverUtland;

    /**
     * Kan være privat eller virksomhet som arbeidsgiver. Dersom {@link #arbeidType}
     * = 'NÆRING', er denne null.
     */
    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    private InternArbeidsforholdRef arbeidsforholdRef;

    @ChangeTracked
    private ArbeidType arbeidType;

    public Yrkesaktivitet() {
        // hibernate
    }

    public Yrkesaktivitet(Yrkesaktivitet yrkesaktivitet) {
        this.arbeidType = yrkesaktivitet.getArbeidType();
        this.arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        this.arbeidsforholdRef = yrkesaktivitet.arbeidsforholdRef;
        this.navnArbeidsgiverUtland = yrkesaktivitet.getNavnArbeidsgiverUtland();

        // NB må aksessere felt her heller en getter siden getter filtrerer
        this.aktivitetsAvtale = yrkesaktivitet.aktivitetsAvtale.stream().map(AktivitetsAvtale::new).collect(Collectors.toCollection(LinkedHashSet::new));

        this.permisjon = yrkesaktivitet.permisjon.stream().map(p -> {
            var permisjonEntitet = new Permisjon(p);
            permisjonEntitet.setYrkesaktivitet(this);
            return permisjonEntitet;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(arbeidsgiver, arbeidsforholdRef, arbeidType);
    }

    /**
     * Kategorisering av aktivitet som er enten pensjonsgivende inntekt eller
     * likestilt med pensjonsgivende inntekt
     * <p>
     * Fra aa-reg
     * <ul>
     * <li>{@link ArbeidType#ORDINÆRT_ARBEIDSFORHOLD}</li>
     * <li>{@link ArbeidType#MARITIMT_ARBEIDSFORHOLD}</li>
     * <li>{@link ArbeidType#FORENKLET_OPPGJØRSORDNING}</li>
     * </ul>
     * <p>
     * Fra inntektskomponenten
     * <ul>
     * <li>{@link ArbeidType#FRILANSER_OPPDRAGSTAKER_MED_MER}</li>
     * </ul>
     * <p>
     * De resterende kommer fra søknaden
     *
     * @return {@link ArbeidType}
     */
    public ArbeidType getArbeidType() {
        return arbeidType;
    }

    void setArbeidType(ArbeidType arbeidType) {
        this.arbeidType = arbeidType;
    }

    /**
     * Unik identifikator for arbeidsforholdet til aktøren i bedriften. Selve
     * nøkkelen er ikke unik, men er unik for arbeidstaker hos arbeidsgiver.
     * <p>
     * NB! Vil kun forekomme i aktiviteter som er hentet inn fra aa-reg
     *
     * @return {@code ArbeidsforholdRef.ref(null)} hvis ikke tilstede
     */
    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    void setArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
        this.arbeidsforholdRef = arbeidsforholdId != null && !InternArbeidsforholdRef.nullRef().equals(arbeidsforholdId) ? arbeidsforholdId : null;
    }

    /**
     * Identifiser om yrkesaktiviteten gjelder for arbeidsgiver og
     * arbeidsforholdRef.
     *
     * @param arbeidsgiver      en {@link Arbeidsgiver}
     * @param arbeidsforholdRef et {@link InternArbeidsforholdRef}
     * @return true hvis arbeidsgiver og arbeidsforholdRef macther
     */

    public boolean gjelderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        var gjelderForArbeidsgiver = Objects.equals(getArbeidsgiver(), arbeidsgiver);
        return gjelderForArbeidsgiver && getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    /**
     * Liste over fremtidige / historiske permisjoner hos arbeidsgiver.
     * <p>
     * NB! Vil kun forekomme i aktiviteter som er hentet inn fra aa-reg
     *
     * @return liste med permisjoner
     */
    public Collection<Permisjon> getPermisjon() {
        return Collections.unmodifiableSet(permisjon);
    }

    void leggTilPermisjon(Permisjon permisjon) {
        this.permisjon.add(permisjon);
        permisjon.setYrkesaktivitet(this);
    }

    public Collection<AktivitetsAvtale> getAlleAktivitetsAvtaler() {
        return Collections.unmodifiableSet(aktivitetsAvtale);
    }

    void leggTilAktivitetsAvtale(AktivitetsAvtale aktivitetsAvtale) {
        this.aktivitetsAvtale.add(aktivitetsAvtale);
    }

    boolean erArbeidsforholdAktivt(LocalDate dato) {
        return this.getAlleAktivitetsAvtaler()
                .stream()
                .filter(AktivitetsAvtale::erAnsettelsesPeriode)
                .anyMatch(aa -> aa.getPeriode().inkluderer(dato));
    }

    /**
     * Arbeidsgiver
     * <p>
     * NB! Vil kun forekomme i aktiviteter som er hentet inn fra aa-reg
     *
     * @return {@link Arbeidsgiver}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    /**
     * Navn på utenlands arbeidsgiver
     *
     * @return Navn
     */
    public String getNavnArbeidsgiverUtland() {
        return navnArbeidsgiverUtland;
    }

    void setNavnArbeidsgiverUtland(String navnArbeidsgiverUtland) {
        this.navnArbeidsgiverUtland = navnArbeidsgiverUtland;
    }

    public boolean erArbeidsforhold() {
        return ArbeidType.AA_REGISTER_TYPER.contains(arbeidType);
    }

    void tilbakestillPermisjon() {
        permisjon.clear();
    }

    void tilbakestillAvtaler() {
        aktivitetsAvtale.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Yrkesaktivitet other)) {
            return false;
        }
        return Objects.equals(this.getArbeidsforholdRef(), other.getArbeidsforholdRef()) &&
                Objects.equals(this.getNavnArbeidsgiverUtland(), other.getNavnArbeidsgiverUtland()) &&
                Objects.equals(this.getArbeidType(), other.getArbeidType()) &&
                Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getArbeidsforholdRef(), getNavnArbeidsgiverUtland(), getArbeidType(), getArbeidsgiver());
    }

    @Override
    public String toString() {
        return "YrkesaktivitetEntitet{" +
                "arbeidsgiver=" + arbeidsgiver +
                ", arbeidsforholdRef=" + arbeidsforholdRef +
                ", arbeidType=" + arbeidType +
                '}';
    }

    void fjernPeriode(DatoIntervallEntitet aktivitetsPeriode) {
        aktivitetsAvtale.removeIf(aa -> aa.matcherPeriode(aktivitetsPeriode));
    }

}
