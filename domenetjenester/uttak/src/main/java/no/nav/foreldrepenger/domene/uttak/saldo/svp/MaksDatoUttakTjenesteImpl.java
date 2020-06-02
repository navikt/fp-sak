package no.nav.foreldrepenger.domene.uttak.saldo.svp;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class MaksDatoUttakTjenesteImpl  implements MaksDatoUttakTjeneste {

    private SvangerskapspengerUttakResultatRepository svpUttakRepository;

    MaksDatoUttakTjenesteImpl() {
        //CDI
    }

    @Inject
    public MaksDatoUttakTjenesteImpl(SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository) {
        this.svpUttakRepository = svangerskapspengerUttakResultatRepository;
    }

    public Optional<LocalDate> beregnMaksDatoUttak(UttakInput uttakInput) {
        var ref = uttakInput.getBehandlingReferanse();

        Optional<SvangerskapspengerUttakResultatEntitet> uttakResultat = svpUttakRepository.hentHvisEksisterer(ref.getBehandlingId());
        return finnSisteUttaksdato(uttakResultat);
    }

    private Optional<LocalDate> finnSisteUttaksdato(Optional<SvangerskapspengerUttakResultatEntitet> uttakResultat) {
        return (uttakResultat.isPresent())?uttakResultat.get().finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad():Optional.empty();
    }
}
