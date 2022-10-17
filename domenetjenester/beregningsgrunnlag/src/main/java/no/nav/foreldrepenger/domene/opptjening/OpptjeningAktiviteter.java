package no.nav.foreldrepenger.domene.opptjening;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class OpptjeningAktiviteter {

    private final List<OpptjeningPeriode> opptjeningPerioder = new ArrayList<>();

    public OpptjeningAktiviteter(Collection<OpptjeningPeriode> perioder) {
        this.opptjeningPerioder.addAll(perioder);
    }

    public OpptjeningAktiviteter(OpptjeningPeriode... perioder) {
        this.opptjeningPerioder.addAll(Arrays.asList(perioder));
    }

    public List<OpptjeningPeriode> getOpptjeningPerioder() {
        return Collections.unmodifiableList(opptjeningPerioder);
    }

    public record OpptjeningPeriode(OpptjeningAktivitetType opptjeningAktivitetType,
                                    Periode periode,
                                    String arbeidsgiverOrgNummer,
                                    String arbeidsgiverAktørId,
                                    InternArbeidsforholdRef arbeidsforholdId) {
        public OpptjeningPeriode {
            Objects.requireNonNull(opptjeningAktivitetType, "type");
            Objects.requireNonNull(periode, "periode");

            // sjekk preconditions
            if (arbeidsgiverAktørId != null) {
                if (arbeidsgiverOrgNummer != null) {
                    throw new IllegalArgumentException("Kan ikke ha orgnummer dersom personlig arbeidsgiver: " + this);
                }
            } else if (arbeidsgiverOrgNummer == null) {
                if (arbeidsforholdId != null) {
                    throw new IllegalArgumentException("Kan ikke ha arbeidsforholdId dersom ikke har arbeidsgiver: " + this);
                }
            }
        }

        @Override
        public String toString() {
            return "OpptjeningPeriode{" + "opptjeningAktivitetType=" + opptjeningAktivitetType + ", periode=" + periode + ", arbeidsgiverOrgNummer='"
                + OrgNummer.tilMaskertNummer(arbeidsgiverOrgNummer) + '\'' + ", arbeidsgiverAktørId='" + arbeidsgiverAktørId + '\'' + ", arbeidsforholdId=" + arbeidsforholdId
                + '}';
        }
    }

    @Override
    public String toString() {
        return "OpptjeningAktiviteter{" + "opptjeningPerioder=" + opptjeningPerioder + '}';
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
