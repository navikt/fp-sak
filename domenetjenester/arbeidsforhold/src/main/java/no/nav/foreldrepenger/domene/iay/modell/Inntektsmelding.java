package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

public class Inntektsmelding implements IndexKey {

    @ChangeTracked
    private List<Gradering> graderinger = new ArrayList<>();

    @ChangeTracked
    private List<NaturalYtelse> naturalYtelser = new ArrayList<>();

    @ChangeTracked
    private List<UtsettelsePeriode> utsettelsePerioder = new ArrayList<>();

    @ChangeTracked
    private Arbeidsgiver arbeidsgiver;

    @ChangeTracked
    private InternArbeidsforholdRef arbeidsforholdRef;

    @ChangeTracked
    private LocalDate startDatoPermisjon;

    private boolean nærRelasjon;

    /**
     * Journalpost referanse (Joark referanse).
     */
    private JournalpostId journalpostId;

    /**
     * Dato inntektsmelding mottatt.
     */
    private LocalDate mottattDato;

    @ChangeTracked
    private Beløp inntektBeløp;

    @ChangeTracked
    private Beløp refusjonBeløpPerMnd;

    @ChangeTracked
    private LocalDate refusjonOpphører;

    private LocalDateTime innsendingstidspunkt;

    private String kanalreferanse;

    private String kildesystem;

    @ChangeTracked
    private List<Refusjon> endringerRefusjon = new ArrayList<>();

    @ChangeTracked
    private InntektsmeldingInnsendingsårsak innsendingsårsak = InntektsmeldingInnsendingsårsak.UDEFINERT;

    Inntektsmelding() {
    }

    public Inntektsmelding(Inntektsmelding inntektsmelding) {
        this.arbeidsgiver = inntektsmelding.getArbeidsgiver();
        this.arbeidsforholdRef = inntektsmelding.arbeidsforholdRef;
        this.startDatoPermisjon = inntektsmelding.getStartDatoPermisjon().orElse(null);
        this.nærRelasjon = inntektsmelding.getErNærRelasjon();
        this.journalpostId = inntektsmelding.getJournalpostId();
        this.inntektBeløp = inntektsmelding.getInntektBeløp();
        this.refusjonBeløpPerMnd = inntektsmelding.getRefusjonBeløpPerMnd();
        this.refusjonOpphører = inntektsmelding.getRefusjonOpphører();
        this.innsendingsårsak = inntektsmelding.getInntektsmeldingInnsendingsårsak();
        this.innsendingstidspunkt = inntektsmelding.getInnsendingstidspunkt();
        this.kanalreferanse = inntektsmelding.getKanalreferanse();
        this.kildesystem = inntektsmelding.getKildesystem();
        this.mottattDato = inntektsmelding.getMottattDato();
        this.graderinger = inntektsmelding.getGraderinger().stream().map(Gradering::new).toList();
        this.naturalYtelser = inntektsmelding.getNaturalYtelser().stream().map(NaturalYtelse::new).toList();
        this.utsettelsePerioder = inntektsmelding.getUtsettelsePerioder().stream().map(UtsettelsePeriode::new).toList();
        this.endringerRefusjon = inntektsmelding.getEndringerRefusjon().stream().map(Refusjon::new).toList();
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(arbeidsgiver, arbeidsforholdRef);
    }

    /**
     * Referanse til journalpost (i arkivsystem - Joark) der dokumentet ligger.
     *
     * @return {@link JournalpostId} som inneholder denne inntektsmeldingen.
     */
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    void setJournalpostId(JournalpostId journalpostId) {
        Objects.requireNonNull(journalpostId);
        this.journalpostId = journalpostId;
    }

    /**
     * Virksomheten som har sendt inn inntektsmeldingen
     *
     * @return {@link Virksomhet}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public InntektsmeldingInnsendingsårsak getInntektsmeldingInnsendingsårsak() {
        return innsendingsårsak;
    }

    void setInntektsmeldingInnsendingsårsak(InntektsmeldingInnsendingsårsak innsendingsårsak) {
        this.innsendingsårsak = innsendingsårsak;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    void setInnsendingstidspunkt(LocalDateTime innsendingstidspunkt) {
        this.innsendingstidspunkt = innsendingstidspunkt;
    }

    /**
     * Kanalreferanse, arkivnummer fra Altinn?
     *
     * @return kanalreferanse
     */
    public String getKanalreferanse() {
        return kanalreferanse;
    }

    void setKanalreferanse(String kanalreferanse) {
        this.kanalreferanse = kanalreferanse;
    }

    public String getKildesystem() {
        return kildesystem;
    }

    void setKildesystem(String kildesystem) {
        this.kildesystem = kildesystem;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    /**
     * Liste over perioder med graderinger
     *
     * @return {@link Gradering}
     */
    public List<Gradering> getGraderinger() {
        return Collections.unmodifiableList(graderinger);
    }

    /**
     * Liste over naturalytelser
     *
     * @return {@link NaturalYtelse}
     */
    public List<NaturalYtelse> getNaturalYtelser() {
        return Collections.unmodifiableList(naturalYtelser);
    }

    /**
     * Liste over utsettelse perioder
     *
     * @return {@link UtsettelsePeriode}
     */
    public List<UtsettelsePeriode> getUtsettelsePerioder() {
        return Collections.unmodifiableList(utsettelsePerioder);
    }

    /**
     * Arbeidsgivers arbeidsforhold referanse
     *
     * @return {@link ArbeidsforholdRef}
     */
    public InternArbeidsforholdRef getArbeidsforholdRef() {
        // Returnere NULL OBJECT slik at vi alltid har en ref (selv om den inneholder
        // null).
        // gjør enkelte sammenligninger (eks. gjelderFor) enklere.
        return arbeidsforholdRef != null ? arbeidsforholdRef : InternArbeidsforholdRef.nullRef();
    }

    /**
     * Gjelder for et spesifikt arbeidsforhold
     *
     * @return {@link Boolean}
     */
    public boolean gjelderForEtSpesifiktArbeidsforhold() {
        return getArbeidsforholdRef() != null && getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold();
    }

    public boolean gjelderSammeArbeidsforhold(Inntektsmelding annen) {
        return getArbeidsgiver().equals(annen.getArbeidsgiver()) && (getArbeidsforholdRef() == null || annen.getArbeidsforholdRef() == null
            || getArbeidsforholdRef() != null && getArbeidsforholdRef().gjelderFor(annen.getArbeidsforholdRef()));
    }

    public boolean endringIArbeidsforholdsIdForSammeArbGiver(Inntektsmelding annen) {
        return getArbeidsgiver().equals(annen.getArbeidsgiver()) && (!gjelderForEtSpesifiktArbeidsforhold() && annen.gjelderForEtSpesifiktArbeidsforhold()
            || gjelderForEtSpesifiktArbeidsforhold() && !annen.gjelderForEtSpesifiktArbeidsforhold());
    }

    /**
     * Setter intern arbeidsdforhold Id for inntektsmelding
     *
     * @param arbeidsforholdRef Intern arbeidsforhold id
     */
    void setArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef != null && !InternArbeidsforholdRef.nullRef().equals(arbeidsforholdRef) ? arbeidsforholdRef : null;
    }

    /**
     * Startdato for permisjonen
     *
     * @return {@link LocalDate}
     */
    public Optional<LocalDate> getStartDatoPermisjon() {
        return Optional.ofNullable(startDatoPermisjon);
    }

    void setStartDatoPermisjon(LocalDate startDatoPermisjon) {
        this.startDatoPermisjon = startDatoPermisjon;
    }

    /**
     * Er det nær relasjon mellom søker og arbeidsgiver
     *
     * @return {@link Boolean}
     */
    public boolean getErNærRelasjon() {
        return nærRelasjon;
    }

    void setNærRelasjon(boolean nærRelasjon) {
        this.nærRelasjon = nærRelasjon;
    }

    /**
     * Oppgitt årsinntekt fra arbeidsgiver
     *
     * @return {@link Beløp}
     */
    public Beløp getInntektBeløp() {
        return inntektBeløp;
    }

    void setInntektBeløp(Beløp inntektBeløp) {
        this.inntektBeløp = inntektBeløp;
    }

    /**
     * Beløpet arbeidsgiver ønsker refundert
     *
     * @return {@link Beløp}
     */
    public Beløp getRefusjonBeløpPerMnd() {
        return refusjonBeløpPerMnd;
    }

    void setRefusjonBeløpPerMnd(Beløp refusjonBeløpPerMnd) {
        this.refusjonBeløpPerMnd = refusjonBeløpPerMnd;
    }

    /**
     * Dersom refusjonen opphører i stønadsperioden angis siste dag det søkes om
     * refusjon for.
     *
     * @return {@link LocalDate}
     */
    public LocalDate getRefusjonOpphører() {
        return refusjonOpphører;
    }

    void setRefusjonOpphører(LocalDate refusjonOpphører) {
        this.refusjonOpphører = refusjonOpphører;
    }

    /**
     * Liste over endringer i refusjonsbeløp
     *
     * @return {@Link Refusjon}
     */
    public List<Refusjon> getEndringerRefusjon() {
        return Collections.unmodifiableList(endringerRefusjon);
    }

    void leggTil(Gradering gradering) {
        this.graderinger.add(gradering);
    }

    void leggTil(NaturalYtelse naturalYtelse) {
        this.naturalYtelser.add(naturalYtelse);
    }

    void leggTil(UtsettelsePeriode utsettelsePeriode) {
        this.utsettelsePerioder.add(utsettelsePeriode);
    }

    void leggTil(Refusjon refusjon) {
        this.endringerRefusjon.add(refusjon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Inntektsmelding entitet)) {
            return false;
        }
        return Objects.equals(getArbeidsgiver(), entitet.getArbeidsgiver())
                && Objects.equals(getArbeidsforholdRef(), entitet.getArbeidsforholdRef())
                && Objects.equals(getJournalpostId(), entitet.getJournalpostId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArbeidsgiver(), getArbeidsforholdRef(), getJournalpostId());
    }

    @Override
    public String toString() {
        return "InntektsmeldingEntitet{" +
                "virksomhet=" + arbeidsgiver +
                ", arbeidsforholdId='" + arbeidsforholdRef + '\'' +
                ", startDatoPermisjon=" + startDatoPermisjon +
                ", nærRelasjon=" + nærRelasjon +
                ", journalpostId=" + journalpostId +
                ", inntektBeløp=" + inntektBeløp +
                ", refusjonBeløpPerMnd=" + refusjonBeløpPerMnd +
                ", refusjonOpphører=" + refusjonOpphører +
                ", innsendingsårsak= " + innsendingsårsak +
                ", innsendingstidspunkt= " + innsendingstidspunkt +
                ", mottattDato = " + mottattDato +
                '}';
    }
}
