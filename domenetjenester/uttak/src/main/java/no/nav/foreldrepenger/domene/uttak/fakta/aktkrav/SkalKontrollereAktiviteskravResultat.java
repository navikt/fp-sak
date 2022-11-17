package no.nav.foreldrepenger.domene.uttak.fakta.aktkrav;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;

public record SkalKontrollereAktiviteskravResultat(boolean kravTilAktivitet, Set<AktivitetskravPeriodeEntitet> avklartePerioder) {

    public boolean isAvklart() {
        return !avklartePerioder().isEmpty();
    }

}
