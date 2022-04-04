package no.nav.foreldrepenger.domene.uttak.saldo.svp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
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

        return svpUttakRepository.hentHvisEksisterer(ref.getBehandlingId())
            .flatMap(MaksDatoUttakTjenesteImpl::finnSisteUttaksdato);
    }

    private static Optional<LocalDate> finnSisteUttaksdato(SvangerskapspengerUttakResultatEntitet uttakResultat) {
        return uttakResultat.finnSisteInnvilgedeUttaksdatoMedUtbetalingsgrad();
    }
}
