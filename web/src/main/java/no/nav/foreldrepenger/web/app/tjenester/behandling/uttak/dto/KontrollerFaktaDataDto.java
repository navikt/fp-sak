package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

public class KontrollerFaktaDataDto {
    private final List<KontrollerFaktaPeriodeDto> perioder;

    public KontrollerFaktaDataDto(List<KontrollerFaktaPeriodeDto> perioder) {
        this.perioder = perioder;
    }

    public List<KontrollerFaktaPeriodeDto> getPerioder() {
        return perioder;
    }
}
