package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import java.time.LocalDate;
import java.util.List;

public record FødselDto(FødselDto.Søknad søknad, FødselDto.Register register, FødselDto.Gjeldende gjeldende) {

    public record BarnHendelseData(LocalDate fødselsdato, LocalDate dødsdato) {
    }

    /*
     * List<BarnHendelseData> barn er listen over barn som er født (registrert i søknaden) og som det søkes foreldrepenger for.
     * int antallBarn er antall barn det er søkt om i søknaden, hvor barna ennå ikke er født.
     * */
    public record Søknad(List<BarnHendelseData> barn, LocalDate termindato, LocalDate utstedtdato, int antallBarn) {
    }

    public record Register(List<BarnHendelseData> barn) {
    }

    public record Gjeldende(Termin termin, FødselDto.Gjeldende.Utstedtdato utstedtdato,
                            AntallBarn antallBarn, List<FødselDto.Gjeldende.GjeldendeBarn> barn, FødselDokumetasjonStatus fødselDokumetasjonStatus) {

        public record Termin(Kilde kilde, LocalDate termindato) {
        }

        public record AntallBarn(Kilde kilde, int antall) {
        }

        public record Utstedtdato(Kilde kilde, LocalDate utstedtdato) {
        }

        public record GjeldendeBarn(Kilde kilde, BarnHendelseData barn, boolean kanOverstyres) {
        }

        public enum FødselDokumetasjonStatus {
            DOKUMENTERT,
            IKKE_DOKUMENTERT,
            IKKE_VURDERT;
        }
    }
}
