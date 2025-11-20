package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;

import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;

public class TidsromPermisjonDto {

    @Size(max = 100)
    private List<@Valid OverføringsperiodeDto> overføringsperioder;

    @Size(max = 100)
    private List<@Valid PermisjonPeriodeDto> permisjonsPerioder;

    @Size(max = 100)
    private List<@Valid GraderingDto> graderingPeriode;

    @Size(max = 100)
    private List<@Valid UtsettelseDto> utsettelsePeriode;

    @Size(max = 100)
    private List<@Valid OppholdDto> oppholdPerioder;

    @JsonAlias("overforingsperioder")
    public List<OverføringsperiodeDto> getOverføringsperioder() {
        return overføringsperioder == null ? List.of() : overføringsperioder;
    }

    public void setOverføringsperioder(List<OverføringsperiodeDto> overføringsperioder) {
        this.overføringsperioder = overføringsperioder;
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

