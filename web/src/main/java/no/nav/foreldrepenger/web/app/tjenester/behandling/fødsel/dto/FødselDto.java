package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record FødselDto(@NotNull FødselDto.Søknad søknad, @NotNull FødselDto.Register register,@NotNull FødselDto.Gjeldende gjeldende) {

    public record BarnHendelseData(@NotNull LocalDate fødselsdato, LocalDate dødsdato) {
    }

    /*
     * List<BarnHendelseData> barn er listen over barn som er født (registrert i søknaden) og som det søkes foreldrepenger for.
     * int antallBarn er antall barn det er søkt om i søknaden, hvor barna ennå ikke er født.
     * */
    public record Søknad(@NotNull List<BarnHendelseData> barn, LocalDate termindato, LocalDate utstedtdato, @NotNull int antallBarn) {
    }

    public record Register(@NotNull List<BarnHendelseData> barn) {
    }

    public record Gjeldende(Termin termin, FødselDto.Gjeldende.Utstedtdato utstedtdato,
                            @NotNull AntallBarn antallBarn, @NotNull List<FødselDto.Gjeldende.GjeldendeBarn> barn, @NotNull FødselDokumetasjonStatus fødselDokumetasjonStatus) {

        // TODO [JOHANNES] -- frontend bruker termindato som notnull, men fra koden ser det ut som den kan bli null
        public record Termin(@NotNull Kilde kilde, @NotNull LocalDate termindato) {
        }

        public record AntallBarn(@NotNull Kilde kilde, @NotNull int antall) {
        }

        // TODO [JOHANNES] -- frontend bruker utstedtdato som notnull, men fra koden ser det ut som den kan bli null
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
