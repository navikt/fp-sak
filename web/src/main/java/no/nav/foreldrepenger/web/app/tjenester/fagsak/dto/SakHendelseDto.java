package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;

public class SakHendelseDto {

    private FamilieHendelseType hendelseType;
    private LocalDate hendelseDato;
    private Integer antallBarn;
    private boolean dødfødsel;

    public SakHendelseDto() {
        // Injiseres i test
    }

    public SakHendelseDto(FamilieHendelseType hendelseType, LocalDate hendelseDato, Integer antallBarn, boolean dødfødsel) {
        this.hendelseType = hendelseType;
        this.hendelseDato = hendelseDato;
        this.antallBarn = antallBarn;
        this.dødfødsel = dødfødsel;
    }

    public FamilieHendelseType getHendelseType() {
        return hendelseType;
    }

    public LocalDate getHendelseDato() {
        return hendelseDato;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public boolean isDødfødsel() {
        return dødfødsel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SakHendelseDto that = (SakHendelseDto) o;
        return hendelseType == that.hendelseType && Objects.equals(hendelseDato, that.hendelseDato) && Objects.equals(antallBarn, that.antallBarn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hendelseType, hendelseDato, antallBarn);
    }

    @Override
    public String toString() {
        return "SaksHendelseDto{" +
            "hendelseType=" + hendelseType +
            ", hendelseDato=" + hendelseDato +
            ", antallBarn=" + antallBarn +
            '}';
    }
}
