package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Table(name = "SVP_UTTAK_RESULTAT")
@Entity
public class SvangerskapspengerUttakResultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_UTTAK_RESULTAT")
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "svp_uttak_resultat_id")
    private List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> uttaksResultatArbeidsforhold = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "behandling_resultat_id", nullable = false, updatable = false)
    private Behandlingsresultat behandlingsresultat;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public Long getId() {
        return id;
    }

    public Behandlingsresultat getBehandlingsresultat() {
        return behandlingsresultat;
    }

    public List<SvangerskapspengerUttakResultatArbeidsforholdEntitet> getUttaksResultatArbeidsforhold() {
        return uttaksResultatArbeidsforhold;
    }

    public void deaktiver() {
        aktiv = false;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public Optional<LocalDate> finnSisteUttaksdato() {
        Optional<LocalDate> sisteUttaksdato = Optional.empty();
        for (var perArbeidsforhold : uttaksResultatArbeidsforhold) {
            var maxDatoForArbeidsforhold = perArbeidsforhold.getPerioder()
                .stream()
                .map(p -> p.getTidsperiode().getTomDato())
                .max(LocalDate::compareTo);
            if (sisteUttaksdato.isEmpty() || maxDatoForArbeidsforhold.isPresent() && maxDatoForArbeidsforhold.get().isAfter(sisteUttaksdato.get())) {
                sisteUttaksdato = maxDatoForArbeidsforhold;
            }
        }
        return sisteUttaksdato;
    }

    public Optional<LocalDate> finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad() {
        Optional<LocalDate> sisteUttaksdato = Optional.empty();
        for (var perArbeidsforhold : uttaksResultatArbeidsforhold) {
            var maxDatoForArbeidsforhold = perArbeidsforhold.getPerioder()
                .stream()
                .filter(SvangerskapspengerUttakResultatPeriodeEntitet::isInnvilget)
                .filter(p -> p.getUtbetalingsgrad().harUtbetaling())
                .map(p -> p.getTidsperiode().getTomDato())
                .max(LocalDate::compareTo);
            if (sisteUttaksdato.isEmpty() || maxDatoForArbeidsforhold.isPresent() && maxDatoForArbeidsforhold.get().isAfter(sisteUttaksdato.get())) {
                sisteUttaksdato = maxDatoForArbeidsforhold;
            }
        }
        return sisteUttaksdato;
    }

    public Optional<LocalDate> finnFørsteUttaksdato() {
        Optional<LocalDate> førsteUttaksdato = Optional.empty();
        for (var perArbeidsforhold : uttaksResultatArbeidsforhold) {
            var minDatoForArbeidsforhold = perArbeidsforhold.getPerioder()
                .stream()
                .map(p -> p.getTidsperiode().getFomDato())
                .min(LocalDate::compareTo);
            if (førsteUttaksdato.isEmpty() || minDatoForArbeidsforhold.isPresent() && minDatoForArbeidsforhold.get()
                .isBefore(førsteUttaksdato.get())) {
                førsteUttaksdato = minDatoForArbeidsforhold;
            }
        }
        return førsteUttaksdato;
    }

    public static class Builder {
        private SvangerskapspengerUttakResultatEntitet kladd;

        public Builder(Behandlingsresultat behandlingsresultat) {
            kladd = new SvangerskapspengerUttakResultatEntitet();
            kladd.behandlingsresultat = behandlingsresultat;
        }

        public Builder medUttakResultatArbeidsforhold(SvangerskapspengerUttakResultatArbeidsforholdEntitet entitet) {
            kladd.uttaksResultatArbeidsforhold.add(entitet);
            return this;
        }

        public SvangerskapspengerUttakResultatEntitet build() {
            return kladd;
        }
    }
}
