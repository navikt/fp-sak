package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadUtsettelseUttakDato;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;

@NamespaceRef(SøknadConstants.NAMESPACE)
public class EndringUtsettelseUttak {

    private EndringUtsettelseUttak() {
    }

    public static SøknadUtsettelseUttakDato ekstraherUtsettelseUttakFra(SøknadWrapper wrapper) {
        List<LukketPeriodeMedVedlegg> perioder = new ArrayList<>();
        if (wrapper.getOmYtelse() instanceof Foreldrepenger foreldrepenger) {
            perioder.addAll(foreldrepenger.getFordeling().getPerioder());
        } else if (wrapper.getOmYtelse() instanceof Endringssoeknad endringssoeknad) {
            perioder.addAll(endringssoeknad.getFordeling().getPerioder());
        } else {
            throw new IllegalArgumentException("Ugyldig søknadstype");
        }

        var utsettelseFom = perioder.stream()
            .filter(p -> p instanceof Utsettelsesperiode || p instanceof Oppholdsperiode)
            .map(LukketPeriodeMedVedlegg::getFom)
            .min(Comparator.naturalOrder())
            .orElse(null);
        var uttakFom = perioder.stream()
            .filter(p -> p instanceof Uttaksperiode || p instanceof Overfoeringsperiode)
            .map(LukketPeriodeMedVedlegg::getFom)
            .min(Comparator.naturalOrder())
            .orElse(null);

        return new SøknadUtsettelseUttakDato(utsettelseFom, uttakFom);
    }

}
