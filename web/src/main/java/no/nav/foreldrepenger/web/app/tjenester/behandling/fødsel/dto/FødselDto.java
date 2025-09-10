package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import java.time.LocalDate;
import java.util.List;

public record FødselDto(FødselDto.Søknad søknad, FødselDto.Register register, FødselDto.Gjeldende gjeldende) {

    public record BarnHendelseData(LocalDate fødselsdato, LocalDate dødsdato) {
    }

    public record Søknad(List<BarnHendelseData> barn, LocalDate termindato, LocalDate utstedtdato, int antallBarn) {
    }

    public record Register(List<BarnHendelseData> barn) {
    }

    public record Gjeldende(Termin termin, FødselDto.Gjeldende.Utstedtdato utstedtdato, AntallBarn antallBarn,
                            List<FødselDto.Gjeldende.GjeldendeBarn> barn, FødselDokumetasjonStatus fødselDokumetasjonStatus) {

        public enum FødselDokumetasjonStatus {
            DOKUMENTERT,
            IKKE_DOKUMENTERT,
            IKKE_VURDERT;
        }

        public record Termin(Kilde kilde, LocalDate termindato) {
        }

        public record AntallBarn(Kilde kilde, int antall) {
        }

        public record Utstedtdato(Kilde kilde, LocalDate utstedtdato) {
        }

        public record GjeldendeBarn(Kilde kilde, BarnHendelseData barn, boolean kanOverstyres) {
        }
    }
}
