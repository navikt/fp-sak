package no.nav.foreldrepenger.domene.prosess.opptjening;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class OpptjeningAktiviteter {

    private List<OpptjeningPeriode> opptjeningPerioder = new ArrayList<>();

    public OpptjeningAktiviteter(Collection<OpptjeningPeriode> perioder) {
        this.opptjeningPerioder.addAll(perioder);
    }

    public OpptjeningAktiviteter(OpptjeningPeriode... perioder) {
        this.opptjeningPerioder.addAll(Arrays.asList(perioder));
    }

    public List<OpptjeningPeriode> getOpptjeningPerioder() {
        return Collections.unmodifiableList(opptjeningPerioder);
    }

    public static class OpptjeningPeriode {

        private OpptjeningAktivitetType type;
        private Periode periode;
        private String arbeidsgiverOrgNummer;
        private String arbeidsgiverAktørId;

        /**
         * For virksomheter så er internt arbeidsforhold ref generert unikt på bakgrunn av virksomhetens oppgitt eksterne arbeidsforhold ref.
         * <p>
         * For private personer som arbeidsgivere vil ArbeidsforholdRef være linket
         * til ekstern arbeidsforhold ref som er syntetisk skapt (ved UUID#namedUUIDFromBytes). Så er altså ikke noe Altinn sender inn eller som på
         * annet vis fås i inntetksmelding for private arbeidsgiver. Brukes kun til å skille ulike arbeidstyper for samme privat person internt.
         */
        private InternArbeidsforholdRef arbeidsforholdId;

        OpptjeningPeriode() {
        }

        private OpptjeningPeriode(OpptjeningAktivitetType type,
                                  Periode periode,
                                  String arbeidsgiverOrgNummer,
                                  String arbeidsgiverAktørId,
                                  InternArbeidsforholdRef arbeidsforholdId) {
            this.type = Objects.requireNonNull(type, "type");
            this.periode = Objects.requireNonNull(periode, "periode");

            // sjekk preconditions
            if (arbeidsgiverAktørId != null) {
                this.arbeidsgiverAktørId = arbeidsgiverAktørId;
                if (arbeidsgiverOrgNummer != null) {
                    throw new IllegalArgumentException("Kan ikke ha orgnummer dersom personlig arbeidsgiver: " + this);
                }
            } else if (arbeidsgiverOrgNummer != null) {
                this.arbeidsgiverOrgNummer = arbeidsgiverOrgNummer;
                this.arbeidsforholdId = arbeidsforholdId;
            } else {
                if (arbeidsforholdId != null) {
                    throw new IllegalArgumentException("Kan ikke ha arbeidsforholdId dersom ikke har arbeidsgiver: " + this);
                }
            }

        }

        public OpptjeningAktivitetType getType() {
            return type;
        }

        public OpptjeningAktivitetType getOpptjeningAktivitetType() {
            return type;
        }

        public Periode getPeriode() {
            return periode;
        }

        public String getArbeidsgiverOrgNummer() {
            return arbeidsgiverOrgNummer;
        }

        public String getArbeidsgiverAktørId() {
            return arbeidsgiverAktørId;
        }

        public InternArbeidsforholdRef getArbeidsforholdId() {
            return arbeidsforholdId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, periode, arbeidsgiverOrgNummer, arbeidsgiverAktørId, arbeidsforholdId);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !obj.getClass().equals(this.getClass())) {
                return false;
            }
            OpptjeningPeriode other = (OpptjeningPeriode) obj;
            return Objects.equals(this.arbeidsgiverOrgNummer, other.arbeidsgiverOrgNummer)
                && Objects.equals(this.arbeidsgiverAktørId, other.arbeidsgiverAktørId)
                && Objects.equals(this.periode, other.periode)
                && Objects.equals(this.type, other.type)
                && Objects.equals(this.arbeidsforholdId, other.arbeidsforholdId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName()
                + "<type=" + type
                + ", periode=" + periode
                + (arbeidsgiverOrgNummer == null ? "" : ", arbeidsgiverOrgNummer=" + arbeidsgiverOrgNummer)
                + (arbeidsgiverAktørId == null ? "" : ", arbeidsgiverAktørId=" + arbeidsgiverAktørId)
                + (arbeidsforholdId == null ? "" : ", arbeidsforholdId=" + arbeidsforholdId)
                + ">";
        }

    }

    public static OpptjeningPeriode nyPeriode(OpptjeningAktivitetType type,
                                              Periode periode,
                                              String arbeidsgiverOrgNummer,
                                              String aktørId,
                                              InternArbeidsforholdRef arbeidsforholdId) {
        return new OpptjeningPeriode(type, periode, arbeidsgiverOrgNummer, aktørId, arbeidsforholdId);
    }

    public static OpptjeningPeriode nyPeriodeOrgnr(OpptjeningAktivitetType type,
                                                   Periode periode,
                                                   String arbeidsgiverOrgNummer,
                                                   InternArbeidsforholdRef arbeidsforholdId) {
        return new OpptjeningPeriode(type, periode, arbeidsgiverOrgNummer, null, arbeidsforholdId);
    }

    public static OpptjeningPeriode nyPeriodeOrgnr(OpptjeningAktivitetType type,
                                                   Periode periode,
                                                   String arbeidsgiverOrgNummer) {
        return new OpptjeningPeriode(type, periode, arbeidsgiverOrgNummer, null, null);
    }

    /** Lag ny opptjening periode for angitt aktivitet uten arbeidsgiver (kan ikke vøre type ARBEID). */
    public static OpptjeningPeriode nyPeriode(OpptjeningAktivitetType type, Periode periode) {
        kanIkkeVæreArbeid(type);
        return new OpptjeningPeriode(type, periode, null, null, null);
    }

    /** Lag ny opptjening periode for angitt aktivitet og med privat arbeidsgive (angitt ved aktørId). */
    public static OpptjeningPeriode nyPeriodeAktør(OpptjeningAktivitetType type, Periode periode, String aktørId) {
        return nyPeriode(type, periode, null, aktørId, null);
    }

    /** Lag ny opptjening periode for angitt aktivitet og med privat arbeidsgive (angitt ved aktørId) og arbeidsforhold ref. */
    public static OpptjeningPeriode nyPeriodeAktør(OpptjeningAktivitetType type, Periode periode, String aktørId, InternArbeidsforholdRef arbeidsforholdRef) {
        return nyPeriode(type, periode, null, aktørId, arbeidsforholdRef);
    }

    /** Med enkel, registrert arbeidsgiver. ArbeidsforholdReferanse optional. */
    public static OpptjeningAktiviteter fraOrgnr(OpptjeningAktivitetType type, Periode periode, String orgnr, InternArbeidsforholdRef arbId) {
        return new OpptjeningAktiviteter(nyPeriode(type, periode, orgnr, null, arbId));
    }

    /** Med enkel, registrert arbeidsgiver. Ikke arbeidsforholdReferanse. */
    public static OpptjeningAktiviteter fraOrgnr(OpptjeningAktivitetType type, Periode periode, String orgnr) {
        return new OpptjeningAktiviteter(nyPeriode(type, periode, orgnr, null, null));
    }

    /** Med enkel, privat arbeidsgiver. Merk - angi arbeidsgivers aktørId, ikke søkers. */
    public static OpptjeningAktiviteter fraAktørId(OpptjeningAktivitetType type, Periode periode, String arbeidsgiverAktørId) {
        return new OpptjeningAktiviteter(nyPeriode(type, periode, null, arbeidsgiverAktørId, null));
    }

    /**
     * Med enkel, privat arbeidsgiver. Merk - angi arbeidsgivers aktørId, ikke søkers.
     */
    public static OpptjeningAktiviteter fraAktørId(OpptjeningAktivitetType type, Periode periode, String arbeidsgiverAktørId,
                                                   InternArbeidsforholdRef arbeidsforholdRef) {
        return new OpptjeningAktiviteter(nyPeriode(type, periode, null, arbeidsgiverAktørId, arbeidsforholdRef));
    }

    /** Med enkel, aktivitet uten arbeidsgiver (kan ikke være {@link OpptjeningAktivitetType#ARBEID}. */
    public static OpptjeningAktiviteter fra(OpptjeningAktivitetType type, Periode periode) {
        kanIkkeVæreArbeid(type);
        return new OpptjeningAktiviteter(nyPeriode(type, periode, null, null, null));
    }

    private static void kanIkkeVæreArbeid(OpptjeningAktivitetType type) {
        if (OpptjeningAktivitetType.ARBEID.equals(type)) {
            throw new IllegalArgumentException("Kan ikke angi Opptjening av type ARBEID uten å angi arbeidsgiver.");
        }
    }

}
