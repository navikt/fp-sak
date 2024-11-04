package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@Entity
@Table(name = "SVP_UTTAK_ARBEIDSFORHOLD")
public class SvangerskapspengerUttakResultatArbeidsforholdEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_UTTAK_ARBEIDSFORHOLD")
    private Long id;

    @Convert(converter = ArbeidsforholdIkkeOppfyltÅrsak.KodeverdiConverter.class)
    @Column(name="arbeidsforhold_resultat_aarsak", nullable=false)
    private ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak = ArbeidsforholdIkkeOppfyltÅrsak.INGEN;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef = InternArbeidsforholdRef.nullRef();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "svp_uttak_arbeidsforhold_id")
    private List<SvangerskapspengerUttakResultatPeriodeEntitet> perioder = new ArrayList<>();

    @Convert(converter = UttakArbeidType.KodeverdiConverter.class)
    @Column(name = "UTTAK_ARBEID_TYPE")
    private UttakArbeidType uttakArbeidType;

    public Long getId() {
        return id;
    }

    public ArbeidsforholdIkkeOppfyltÅrsak getArbeidsforholdIkkeOppfyltÅrsak() {
        return arbeidsforholdIkkeOppfyltÅrsak;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public List<SvangerskapspengerUttakResultatPeriodeEntitet> getPerioder() {
        return perioder.stream().sorted(Comparator.comparing(SvangerskapspengerUttakResultatPeriodeEntitet::getFom)).toList();
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public boolean isAvslått() {
        return !ArbeidsforholdIkkeOppfyltÅrsak.INGEN.equals(getArbeidsforholdIkkeOppfyltÅrsak());
    }

    public static class Builder {
        private SvangerskapspengerUttakResultatArbeidsforholdEntitet kladd;

        public Builder() {
            this.kladd = new SvangerskapspengerUttakResultatArbeidsforholdEntitet();
        }

        public Builder medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak) {
            kladd.arbeidsforholdIkkeOppfyltÅrsak = arbeidsforholdIkkeOppfyltÅrsak;
            return this;
        }

        public Builder medArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsgiver = arbeidsgiver;
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medPeriode(SvangerskapspengerUttakResultatPeriodeEntitet svangerskapspengerUttakResultatPeriodeEntitet) {
            validerIkkeOverlapp(svangerskapspengerUttakResultatPeriodeEntitet);
            kladd.perioder.add(svangerskapspengerUttakResultatPeriodeEntitet);
            return this;
        }

        public Builder medUttakArbeidType(UttakArbeidType uttakArbeidType) {
            kladd.uttakArbeidType = uttakArbeidType;
            return this;
        }

        private void validerIkkeOverlapp(SvangerskapspengerUttakResultatPeriodeEntitet p2) {
            for (var p1 : kladd.perioder) {
                if (p1.getTidsperiode().overlapper(p2.getTidsperiode())) {
                    throw new IllegalArgumentException("UttakResultatPerioder kan ikke overlappe " + p2 + p1);
                }
            }
        }

        public SvangerskapspengerUttakResultatArbeidsforholdEntitet build() {
            return kladd;
        }
    }

}
