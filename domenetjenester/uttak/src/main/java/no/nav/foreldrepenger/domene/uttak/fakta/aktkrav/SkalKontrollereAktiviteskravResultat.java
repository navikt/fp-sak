package no.nav.foreldrepenger.domene.uttak.fakta.aktkrav;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;

public class SkalKontrollereAktiviteskravResultat {

    private final boolean kravTilAktivitet;
    private final Set<AktivitetskravPeriodeEntitet> avklartePerioder;

    public SkalKontrollereAktiviteskravResultat(boolean kravTilAktivitet, Set<AktivitetskravPeriodeEntitet> avklartePerioder) {
        this.kravTilAktivitet = kravTilAktivitet;
        this.avklartePerioder = avklartePerioder;
    }

    public boolean isKravTilAktivitet() {
        return kravTilAktivitet;
    }

    public boolean isAvklart() {
        return !getAvklartePerioder().isEmpty();
    }

    public Set<AktivitetskravPeriodeEntitet> getAvklartePerioder() {
        return avklartePerioder;
    }
}
