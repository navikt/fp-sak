package no.nav.foreldrepenger.mottak.fyllutsendinn.frontend;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Speiler frontend-skjemaets tidsromPermisjon form field structure.
 * Inkluderer boolean-toggler (fulltUttak, skalUtsette, etc.) som kontrollerer hvilke paneler som vises,
 * i tillegg til periodelistene.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TidsromPermisjonFormValues(
    Boolean fulltUttak,
    List<PermisjonPeriode> permisjonsPerioder,
    Boolean skalOvertaKvote,
    List<OverforingPeriode> overføringsperioder,
    Boolean skalUtsette,
    List<UtsettelsPeriode> utsettelsePeriode,
    Boolean skalGradere,
    List<GraderingPeriode> graderingPeriode,
    Boolean skalHaOpphold,
    List<OppholdPeriode> oppholdPerioder
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PermisjonPeriode(LocalDate periodeFom, LocalDate periodeTom, String periodeType, String morsAktivitet) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverforingPeriode(LocalDate periodeFom, LocalDate periodeTom, String overforingArsak) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UtsettelsPeriode(LocalDate periodeFom, LocalDate periodeTom, String arsakForUtsettelse) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GraderingPeriode(LocalDate periodeFom, LocalDate periodeTom, String periodeForGradering,
                                   Integer prosentandelArbeid, String arbeidsgiverIdentifikator) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OppholdPeriode(LocalDate periodeFom, LocalDate periodeTom, String årsak) { }
}
