package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class InnvilgelseFpLanseringTjeneste {
    private static final Logger LOGGER = LoggerFactory.getLogger(InnvilgelseFpLanseringTjeneste.class);
    private static final Environment ENV = Environment.current();

    private ForeldrepengerUttakTjeneste fpUttakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    @Inject
    public InnvilgelseFpLanseringTjeneste(ForeldrepengerUttakTjeneste fpUttakRepository,
                                          FamilieHendelseRepository familieHendelseRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    InnvilgelseFpLanseringTjeneste() {
        //CDI
    }

    public DokumentMalType velgFpInnvilgelsesmal(Behandling behandling) {
        boolean kanBrukeDokgen = BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())
            && !harAvslåttePerioderEllerPerioderMedGraderingEllerSamtidigUttak(behandling)
            && !harDødtBarn(behandling);

        if (kanBrukeDokgen) {
            LOGGER.info("Saksnummer {} kan bruke Dokgen ved første lansering", behandling.getFagsak().getSaksnummer().getVerdi());
        }

        return !ENV.isProd() && kanBrukeDokgen ?
            DokumentMalType.INNVILGELSE_FORELDREPENGER : DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK;
    }

    private boolean harAvslåttePerioderEllerPerioderMedGraderingEllerSamtidigUttak(Behandling behandling) {
        return fpUttakRepository.hentUttakHvisEksisterer(behandling.getId())
            .map(ForeldrepengerUttak::getGjeldendePerioder)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(p -> !p.isInnvilget() || p.isGraderingInnvilget() || p.isSøktGradering() || p.isSamtidigUttak());
    }

    private boolean harDødtBarn(Behandling behandling) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlagEntitet = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        return familieHendelseGrunnlagEntitet.isPresent()
            && (familieHendelseGrunnlagEntitet.get().getGjeldendeVersjon().getInnholderDødtBarn()
            || familieHendelseGrunnlagEntitet.get().getGjeldendeVersjon().getInnholderDøfødtBarn());
    }
}
