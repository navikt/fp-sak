package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.Søknadsfrister;
import no.nav.foreldrepenger.skjæringstidspunkt.svp.SøknadsperiodeFristTjenesteImpl;

@ApplicationScoped
public class FørsteLovligeUttaksdatoTjeneste {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private SøknadRepository søknadRepository;

    FørsteLovligeUttaksdatoTjeneste() {
        //For CDI
    }

    @Inject
    public FørsteLovligeUttaksdatoTjeneste(UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                                           BehandlingsresultatRepository behandlingsresultatRepository,
                                           SvangerskapspengerRepository svangerskapspengerRepository,
                                           SøknadRepository søknadRepository) {
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.søknadRepository = søknadRepository;
    }

    public Optional<AksjonspunktDefinisjon> vurder(Long behandlingId) {
        var søknadMottattDato = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(SøknadEntitet::getMottattDato).orElse(null);
        final var tidligsteLovligeUttakDato = søknadMottattDato != null ? Søknadsfrister.tidligsteDatoDagytelse(søknadMottattDato) : null;

        //Lagre søknadsfristresultat
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultatRepository.hent(behandlingId))
            .medFørsteLovligeUttaksdag(tidligsteLovligeUttakDato)
            .medMottattDato(søknadMottattDato)
            .build();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        var førsteUttaksdato = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .flatMap(SøknadsperiodeFristTjenesteImpl::utledNettoSøknadsperiodeFomFraGrunnlag);

        var forTidligUttak = førsteUttaksdato.filter(fud -> fud.isBefore(tidligsteLovligeUttakDato)).isPresent();
        return forTidligUttak ? Optional.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST) : Optional.empty();
    }

}
