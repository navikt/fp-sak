package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record FødselDto(@NotNull FødselDto.Søknad søknad, @NotNull FødselDto.Register register,@NotNull FødselDto.Gjeldende gjeldende) {

    public record BarnHendelseData(@NotNull LocalDate fødselsdato, LocalDate dødsdato, Integer barnNummer) {
    }

    public record Søknad(@NotNull List<BarnHendelseData> barn, LocalDate termindato, LocalDate utstedtdato, @NotNull int antallBarn) {
    }

    public record Register(@NotNull List<BarnHendelseData> barn) {
    }

    public record Gjeldende(Termin termin, FødselDto.Gjeldende.Utstedtdato utstedtdato,
                            @NotNull AntallBarn antallBarn, @NotNull List<FødselDto.Gjeldende.GjeldendeBarn> barn, @NotNull FødselDokumetasjonStatus fødselDokumetasjonStatus) {

        public record Termin(@NotNull Kilde kilde, @NotNull LocalDate termindato) {
        }

        public record AntallBarn(@NotNull Kilde kilde, @NotNull int antall) {
        }

        public record Utstedtdato(@NotNull Kilde kilde, @NotNull LocalDate utstedtdato) {
        }

        public record GjeldendeBarn(@NotNull Kilde kilde, @NotNull BarnHendelseData barn, @NotNull boolean kanOverstyres) {
        }

        public enum FødselDokumetasjonStatus {
            DOKUMENTERT,
            IKKE_DOKUMENTERT,
            IKKE_VURDERT;
        }
    }
}
