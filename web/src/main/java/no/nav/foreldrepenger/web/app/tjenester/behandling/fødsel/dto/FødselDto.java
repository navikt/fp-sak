package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.familiehendelse.rest.AvklartBarnDto;

public record FødselDto(FødselDto.Søknad søknad, FødselDto.Register register, FødselDto.Gjeldende gjeldende) {

    public record Søknad(List<AvklartBarnDto> barn, LocalDate termindato, LocalDate utstedtDato) {
    }

    public record Register(List<AvklartBarnDto> barn) {
    }

    public record Gjeldende(FødselDto.Gjeldende.Termindato termindato, FødselDto.Gjeldende.Utstedtdato utstedtdato, List<FødselDto.Gjeldende.Barn> barn) {

        public record Termindato(Kilde kilde, LocalDate termindato, boolean kanOverstyres) {
        }
        public record Utstedtdato(Kilde kilde, LocalDate utstedtdato) {
        }

        public record Barn(Kilde kilde, AvklartBarnDto barn, boolean kanOverstyres) {
        }
    }
}
