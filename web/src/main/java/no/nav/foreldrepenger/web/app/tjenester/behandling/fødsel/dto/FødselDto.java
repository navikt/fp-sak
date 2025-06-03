package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.familiehendelse.rest.AvklartBarnDto;

public record FødselDto(FødselDto.Søknad søknad, FødselDto.Register register, FødselDto.Gjeldende gjeldende) {

    /*
    * List<AvklartBarnDto> barn er listen over barn som er født (registrert i søknaden) og som det søkes foreldrepenger for.
    * int antallBarn er antall barn det er søkt om i søknaden, hvor barna ennå ikke er født.
    * */
    public record Søknad(List<AvklartBarnDto> barn, LocalDate termindato, LocalDate utstedtDato, int antallBarn) {
    }

    public record Register(List<AvklartBarnDto> barn) {
    }

    public record Gjeldende(FødselDto.Gjeldende.Termindato termindato, FødselDto.Gjeldende.Utstedtdato utstedtdato, List<FødselDto.Gjeldende.Barn> barn, int antallBarn) {

        public record Termindato(Kilde kilde, LocalDate termindato, boolean kanOverstyres) {
        }
        public record Utstedtdato(Kilde kilde, LocalDate utstedtdato) {
        }

        public record Barn(Kilde kilde, AvklartBarnDto barn, boolean kanOverstyres) {
        }
    }
}
