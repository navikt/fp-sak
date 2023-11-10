package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;

public class TidsromPermisjonDto {

    @Valid
    @Size(max = 100)
    private List<OverføringsperiodeDto> overforingsperioder;

    @Valid
    @Size(max = 100)
    private List<PermisjonPeriodeDto> permisjonsPerioder;

    @Valid
    @Size(max = 100)
    private List<GraderingDto> graderingPeriode;

    @Valid
    @Size(max = 100)
    private List<UtsettelseDto> utsettelsePeriode;

    @Valid
    @Size(max = 100)
    private List<OppholdDto> oppholdPerioder;

    public List<OverføringsperiodeDto> getOverforingsperioder() {
        return overforingsperioder == null ? List.of() : overforingsperioder;
    }

    public void setOverforingsperioder(List<OverføringsperiodeDto> overforingsperioder) {
        this.overforingsperioder = overforingsperioder;
    }

    public List<PermisjonPeriodeDto> getPermisjonsPerioder() {
        return permisjonsPerioder == null ? List.of() : permisjonsPerioder;
    }

    public void setPermisjonsPerioder(List<PermisjonPeriodeDto> permisjonsPerioder) {
        this.permisjonsPerioder = permisjonsPerioder;
    }

    public List<GraderingDto> getGraderingPeriode() {
        return graderingPeriode == null ? List.of() : graderingPeriode;
    }

    public void setGraderingPeriode(List<GraderingDto> graderingPeriode) {
        this.graderingPeriode = graderingPeriode;
    }

    public List<UtsettelseDto> getUtsettelsePeriode() {
        return utsettelsePeriode == null ? List.of() : utsettelsePeriode;
    }

    public void setUtsettelsePeriode(List<UtsettelseDto> utsettelsePeriode) {
        this.utsettelsePeriode = utsettelsePeriode;
    }

    public List<OppholdDto> getOppholdPerioder() {
        return oppholdPerioder == null ? List.of() : oppholdPerioder;
    }

    public void setOppholdPerioder(List<OppholdDto> oppholdPerioder) {
        this.oppholdPerioder = oppholdPerioder;
    }
}
