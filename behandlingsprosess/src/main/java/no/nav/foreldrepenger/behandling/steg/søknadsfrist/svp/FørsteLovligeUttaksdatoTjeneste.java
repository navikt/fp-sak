package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.svp.SøknadsperiodeFristTjenesteImpl;

@ApplicationScoped
public class FørsteLovligeUttaksdatoTjeneste {

    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;

    FørsteLovligeUttaksdatoTjeneste() {
        //For CDI
    }

    @Inject
    public FørsteLovligeUttaksdatoTjeneste(UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                                           SvangerskapspengerRepository svangerskapspengerRepository,
                                           SøknadRepository søknadRepository,
                                           BehandlingRepository behandlingRepository) {
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.søknadRepository = søknadRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public Optional<AksjonspunktDefinisjon> vurder(Long behandlingId) {
        var søknadMottattDato = søknadRepository.hentSøknadHvisEksisterer(behandlingId).map(SøknadEntitet::getMottattDato).orElseGet(LocalDate::now);

        // Revurderinger vil hente perioderense fra forrige behandling (dersom før denne søknaden) pga uheldig søknadsdynamikk ift endringer.
        var brukperiodegrense = behandlingRepository.hentBehandling(behandlingId)
            .getOriginalBehandlingId()
            .flatMap(original -> uttaksperiodegrenseRepository.hentHvisEksisterer(original))
            .map(Uttaksperiodegrense::getMottattDato)
            .filter(grenseMottattDato -> grenseMottattDato.isBefore(søknadMottattDato))
            .orElse(søknadMottattDato);

        //Henter første fra dato og dens tidligste mottatt dato fra tilretteleggingsgrunnlaget, tidligst mottat dato ble lagt inn i okt 2022 - hvis den er null, så må vi bruke brukperiodegrense
        var datoerSøknadsfrist = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .flatMap(SøknadsperiodeFristTjenesteImpl::utledFørsteSøknadsperiodeFomFraGrunnlag);

        var førsteUttaksDato = datoerSøknadsfrist.map(SøknadsperiodeFristTjenesteImpl.DatoerSøknadsfrist::førsteUttakDato);
        var tidligstMottattDato = datoerSøknadsfrist.map(SøknadsperiodeFristTjenesteImpl.DatoerSøknadsfrist::tidligstMottatt)
            .filter(d -> d.isBefore(brukperiodegrense))
            .orElse(brukperiodegrense);

        //Lagre søknadsfristresultat - obs brukes i svp-uttak-regler og må ta hensyn til revurdering med eldre tilretteleggingFom
        var uttaksperiodegrense = new Uttaksperiodegrense(tidligstMottattDato);
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);

        var tidligsteLovligeUttakDato = Søknadsfrister.tidligsteDatoDagytelse(tidligstMottattDato);

        var forTidligUttak = førsteUttaksDato.filter(fud -> fud.isBefore(tidligsteLovligeUttakDato)).isPresent();
        return forTidligUttak ? Optional.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST) : Optional.empty();
    }

}
