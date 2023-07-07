package no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "ArbeidsforholdValg")
@Table(name = "ARBEIDSFORHOLD_VALG")
public class ArbeidsforholdValg extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ARBEIDSFORHOLD_VALG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = ArbeidsforholdKomplettVurderingType.KodeverdiConverter.class)
    @Column(name = "vurdering", nullable = false)
    private ArbeidsforholdKomplettVurderingType vurdering;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "arbeidsgiver_ident", nullable = false)
    private String arbeidsgiverIdent;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    protected ArbeidsforholdValg() {
        // Hibernate
    }

    public ArbeidsforholdValg(ArbeidsforholdValg kopi) {
        this.vurdering = kopi.vurdering;
        this.arbeidsgiverIdent = kopi.arbeidsgiverIdent;
        this.arbeidsforholdRef = kopi.arbeidsforholdRef;
        this.begrunnelse = kopi.begrunnelse;
        this.aktiv = kopi.aktiv;
    }

    public Long getId() {
        return id;
    }

    public ArbeidsforholdKomplettVurderingType getVurdering() {
        return vurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Arbeidsgiver getArbeidsgiver() {
        if (OrgNummer.erGyldigOrgnr(arbeidsgiverIdent)) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdent);
        }
        return Arbeidsgiver.person(new AktørId(arbeidsgiverIdent));
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public boolean erAktiv() {
        return aktiv;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = Objects.requireNonNull(behandlingId, "behandlingId");
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = Objects.requireNonNull(begrunnelse, "begrunnelse");
    }

    void setVurdering(ArbeidsforholdKomplettVurderingType vurdering) {
        this.vurdering = Objects.requireNonNull(vurdering, "vurdering");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ArbeidsforholdValg) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(vurdering, that.vurdering) &&
            Objects.equals(begrunnelse, that.begrunnelse) &&
            Objects.equals(arbeidsgiverIdent, that.arbeidsgiverIdent) &&
            Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, vurdering, begrunnelse, arbeidsgiverIdent, arbeidsforholdRef);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder kopier(ArbeidsforholdValg mal) {
        return new Builder(mal);
    }

    public static class Builder {
        private ArbeidsforholdValg kladd;

        Builder() {
            kladd = new ArbeidsforholdValg();
        }

        Builder(ArbeidsforholdValg mal) {
            kladd = new ArbeidsforholdValg(mal);
        }

        public Builder medBegrunnelse(String begrunnelse) {
            kladd.begrunnelse = Objects.requireNonNull(begrunnelse);
            return this;
        }

        public Builder medArbeidsgiver(String arbeidsgiverIdent) {
            kladd.arbeidsgiverIdent = Objects.requireNonNull(arbeidsgiverIdent);
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsforholdRef = Objects.requireNonNull(arbeidsforholdRef);
            return this;
        }

        public Builder medVurdering(ArbeidsforholdKomplettVurderingType vurdering) {
            kladd.vurdering = Objects.requireNonNull(vurdering);
            return this;
        }

        public ArbeidsforholdValg build() {
            kladd.validerForBygg();
            return kladd;
        }
    }

    private void validerForBygg() {
        Objects.requireNonNull(arbeidsgiverIdent);
        Objects.requireNonNull(begrunnelse);
        Objects.requireNonNull(vurdering);
    }

}
