package no.nav.foreldrepenger.domene.iay.modell;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class Opptjeningsnøkkel {

    private InternArbeidsforholdRef arbeidsforholdId;
    private String orgNummer;
    private String aktørId;

    public Opptjeningsnøkkel(Yrkesaktivitet yrkesaktivitet) {
        this(yrkesaktivitet.getArbeidsforholdRef(), yrkesaktivitet.getArbeidsgiver());
    }

    public Opptjeningsnøkkel(Arbeidsgiver arbeidsgiver) {
        this(null, arbeidsgiver);
    }

    public Opptjeningsnøkkel(InternArbeidsforholdRef arbeidsforholdId, Arbeidsgiver arbeidsgiver) {
        this(arbeidsforholdId,
            arbeidsgiver.getErVirksomhet() ? arbeidsgiver.getIdentifikator() : null,
            arbeidsgiver.getErVirksomhet() ? null : arbeidsgiver.getIdentifikator());
    }

    public Opptjeningsnøkkel(InternArbeidsforholdRef arbeidsforholdId, String orgNummer, String aktørId) {
        if (arbeidsforholdId == null && orgNummer == null && aktørId == null) {
            throw new IllegalArgumentException("Minst en av arbeidsforholdId, orgnummer og aktørId må vere ulik null");
        }
        this.arbeidsforholdId = arbeidsforholdId;
        this.orgNummer = orgNummer;
        this.aktørId = aktørId;
    }

    public Opptjeningsnøkkel(Virksomhet virksomhet) {
        this(null, virksomhet.getOrgnr(), null);
    }

    public static Opptjeningsnøkkel forOrgnummer(String orgNummer) {
        return new Opptjeningsnøkkel(null, orgNummer, null);
    }

    public static Opptjeningsnøkkel forArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        return new Opptjeningsnøkkel(arbeidsgiver);
    }

    public static Opptjeningsnøkkel forArbeidsforholdIdMedArbeidgiver(InternArbeidsforholdRef arbeidsforholdId, Arbeidsgiver arbeidsgiver) {
        return new Opptjeningsnøkkel(arbeidsforholdId, arbeidsgiver);
    }

    public static Opptjeningsnøkkel forType(String id, Type nøkkelType) {
        return nøkkelType.nyNøkkel(id);
    }

    /**
     * Gir en opptjeningsnøkkel basert på følgende rank
     * 1) ArbeidsforholdId er en unik id fra AAreg
     * 2) Org nummer er iden til en virksomhet som fungere som arbeidsgiver
     * 3) Aktør id er iden til en person som fungere som arbeidsgiver
     */
    public String getVerdi() {
        if (harArbeidsforholdId())
            return this.arbeidsforholdId.getReferanse();
        else if (this.orgNummer != null) {
            return this.orgNummer;
        } else if (this.aktørId != null) {
            return this.aktørId;
        } else {
            throw new IllegalStateException("Har ikke nøkkel");
        }
    }

    private boolean harArbeidsforholdId() {
        return this.arbeidsforholdId != null && this.arbeidsforholdId.getReferanse() != null;
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforholdId);
    }

    public String getForType(Type type) {
        if (type.equals(Type.ARBEIDSFORHOLD_ID)) {
            return arbeidsforholdId != null ? arbeidsforholdId.getReferanse() : null;
        } else if (type.equals(Type.ORG_NUMMER)) {
            return orgNummer;
        } else if (type.equals(Type.AKTØR_ID)) {
            return aktørId;
        }
        throw new IllegalArgumentException("Utvikler-feil: Støtter ikke typen");
    }

    public Type getType() {
        if (harArbeidsforholdId())
            return Type.ARBEIDSFORHOLD_ID;
        else if (this.orgNummer != null) {
            return Type.ORG_NUMMER;
        } else if (this.aktørId != null) {
            return Type.AKTØR_ID;
        } else {
            return null;
        }
    }

    public Type getArbeidsgiverType() {
        if (this.orgNummer != null) {
            return Type.ORG_NUMMER;
        } else if (this.aktørId != null) {
            return Type.AKTØR_ID;
        } else {
            return null;
        }
    }

    public boolean matcher(Opptjeningsnøkkel other) {
        if (other == null) {
            return false;
        }
        if ((this.getType() != Type.ARBEIDSFORHOLD_ID && other.getType() == this.getType())) {
            return other.getVerdi().equals(this.getVerdi());
        } else if ((this.getType() == Type.ARBEIDSFORHOLD_ID && other.getType() == this.getType())) {
            return matchArbeidsforholdId(other);
        } else {
            return matchOrgEllerAktørId(other);
        }
    }

    private boolean matchOrgEllerAktørId(Opptjeningsnøkkel other) {
        if ((Type.ORG_NUMMER.equals(other.getType()) || Type.ORG_NUMMER.equals(this.getType()))
            && (!other.harArbeidsforholdId() || !this.harArbeidsforholdId())) {
            return other.orgNummer != null && other.orgNummer.equals(this.orgNummer);
        }
        return Objects.equals(other.aktørId, this.aktørId);
    }

    private boolean matchArbeidsforholdId(Opptjeningsnøkkel other) {
        boolean likArbeidsforholdsId = other.getVerdi().equals(this.getVerdi());
        boolean likArbeidsgiver;
        if ((other.orgNummer != null) || this.orgNummer != null) {
            likArbeidsgiver = other.orgNummer != null && other.orgNummer.equals(this.orgNummer);
        } else {
            likArbeidsgiver = other.aktørId != null && other.aktørId.equals(this.aktørId);
        }
        return likArbeidsforholdsId && likArbeidsgiver;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj.getClass().equals(this.getClass()))) {
            return false;
        }
        Opptjeningsnøkkel other = (Opptjeningsnøkkel) obj;
        return Objects.equals(aktørId, other.aktørId)
            && Objects.equals(orgNummer, other.orgNummer)
            && Objects.equals(arbeidsforholdId, other.arbeidsforholdId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsforholdId, orgNummer, aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "type=" + getType()
            + ", key=" + getVerdi()
            + ">";
    }

    public boolean harType(Type type) {
        return type.equals(getType());
    }

    public enum Type {
        ARBEIDSFORHOLD_ID {
            @Override
            Opptjeningsnøkkel nyNøkkel(String id) {
                return new Opptjeningsnøkkel(InternArbeidsforholdRef.ref(id), null, null);
            }
        },
        ORG_NUMMER {
            @Override
            Opptjeningsnøkkel nyNøkkel(String id) {
                return new Opptjeningsnøkkel(null, id, null);
            }
        },
        AKTØR_ID {
            @Override
            Opptjeningsnøkkel nyNøkkel(String id) {
                return new Opptjeningsnøkkel(null, null, id);
            }
        };
        abstract Opptjeningsnøkkel nyNøkkel(String id);
    }
}
