package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public class SaldoerDto {

    private final Optional<LocalDate> maksDatoUttak;
    private final Map<String, StønadskontoDto> stonadskontoer;
    private final int tapteDagerFpff;

    public SaldoerDto(Optional<LocalDate> maksDatoUttak, Map<String, StønadskontoDto> stonadskontoer, int tapteDagerFpff) {
        this.maksDatoUttak = maksDatoUttak;
        this.stonadskontoer = stonadskontoer;
        this.tapteDagerFpff = tapteDagerFpff;
    }

    public Optional<LocalDate> getMaksDatoUttak() {
        return maksDatoUttak;
    }

    public Map<String, StønadskontoDto> getStonadskontoer() {
        return stonadskontoer;
    }

    public int getTapteDagerFpff() {
        return tapteDagerFpff;
    }
}
