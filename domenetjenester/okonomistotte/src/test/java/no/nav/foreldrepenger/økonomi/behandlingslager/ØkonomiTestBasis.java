package no.nav.foreldrepenger.økonomi.behandlingslager;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomistøtteUtils;

public class ØkonomiTestBasis {

    protected Avstemming115.Builder lagAvstemming115MedPaakrevdeFelter() {
        String localDateTimeStr = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now());
        return Avstemming115.builder()
            .medKodekomponent(ØkonomiKodekomponent.VLFP.getKodekomponent())
            .medNokkelAvstemming(localDateTimeStr)
            .medTidspnktMelding(localDateTimeStr);
    }

    protected Avstemming115.Builder lagAvstemming115MedPaakrevdeFelter(LocalDateTime nokkelAvstemming, LocalDateTime tidspMelding) {
        return Avstemming115.builder()
            .medKodekomponent(ØkonomiKodekomponent.VLFP.getKodekomponent())
            .medNokkelAvstemming(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(nokkelAvstemming))
            .medTidspnktMelding(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(tidspMelding));
    }
}
