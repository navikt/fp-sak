package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SøknadDto(@NotNull LocalDate mottattDato, String begrunnelseForSenInnsending, @NotNull List<ManglendeVedleggDto> manglendeVedlegg,
                        @NotNull Søknadsfrist søknadsfrist) {

    public record Søknadsfrist(LocalDate gjeldendeMottattDato, LocalDate utledetSøknadsfrist,
                               LocalDate søknadsperiodeStart, LocalDate søknadsperiodeSlutt, @NotNull long dagerOversittetFrist) {

    }
}
